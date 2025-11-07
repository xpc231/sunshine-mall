package com.xpcjsu.sunshinemall.product.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpcjsu.sunshinemall.framework.base.exception.BusinessException;
import com.xpcjsu.sunshinemall.framework.base.exception.ValidationException;
import com.xpcjsu.sunshinemall.framework.distributedid.core.SnowflakeIdGenerator;
import com.xpcjsu.sunshinemall.product.constant.ProductConstants;
import com.xpcjsu.sunshinemall.product.dto.SeckillProductDTO;
import com.xpcjsu.sunshinemall.product.entity.ProductSku;
import com.xpcjsu.sunshinemall.product.entity.SeckillProduct;
import com.xpcjsu.sunshinemall.framework.cache.core.CacheManager;
import com.xpcjsu.sunshinemall.product.mapper.ProductSkuMapper;
import com.xpcjsu.sunshinemall.product.mapper.SeckillProductMapper;
import com.xpcjsu.sunshinemall.product.service.SeckillProductService;
import com.xpcjsu.sunshinemall.product.util.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.xpcjsu.sunshinemall.product.constant.ProductConstants.Cache.SECKILL_STOCK_KEY_PREFIX;


/**
 * 秒杀商品服务实现
 *
 * @author xpcjsu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillProductServiceImpl implements SeckillProductService {

    private final SeckillProductMapper seckillProductMapper;
    private final ProductSkuMapper productSkuMapper;
    private final SnowflakeIdGenerator idGenerator;
    private final CacheManager cacheManager;
    private final DistributedLock distributedLock;


    /**
     * 秒杀库存扣减Lua脚本
     * 功能：检查库存是否充足，如果充足则扣减，否则返回-1
     * 返回值：扣减后的库存数量（>=0表示成功，-1表示库存不足）
     */
    private static final String DEDUCT_STOCK_LUA_SCRIPT =
            "local stock = redis.call('get', KEYS[1])\n" +
            "if stock == false then\n" +
            "    return -1\n" +
            "end\n" +
            "local stockNum = tonumber(stock)\n" +
            "local deductNum = tonumber(ARGV[1])\n" +
            "if stockNum < deductNum then\n" +
            "    return -1\n" +
            "end\n" +
            "local remaining = redis.call('decrby', KEYS[1], deductNum)\n" +
            "return remaining";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSeckillProduct(SeckillProductDTO seckillProductDTO) {
        // 参数验证
        validateSeckillProduct(seckillProductDTO);

        // 验证SKU是否存在且启用
        ProductSku sku = productSkuMapper.selectById(seckillProductDTO.getSkuId());

        if (sku == null) {
            throw new BusinessException("SKU_NOT_FOUND", "SKU不存在");
        }
        if (!ProductConstants.SkuStatus.ENABLED.equals(sku.getStatus())) {
            throw new BusinessException("SKU_DISABLED", "SKU已禁用");
        }

        // 验证时间范围
        if (seckillProductDTO.getStartTime().isAfter(seckillProductDTO.getEndTime())) {
            throw new ValidationException("TIME_RANGE_INVALID", "开始时间不能晚于结束时间");
        }

        // 检查该SKU是否已有秒杀活动
        SeckillProduct existing = seckillProductMapper.selectOne(
                new LambdaQueryWrapper<SeckillProduct>()
                        .eq(SeckillProduct::getSkuId, seckillProductDTO.getSkuId())
                        .eq(SeckillProduct::getIsDeleted, 0)
        );

        if (existing != null) {
            throw new BusinessException("SECKILL_EXISTS", "该SKU已存在秒杀活动");
        }

        // 生成秒杀商品ID
        Long seckillId = idGenerator.nextId();

        // 转换为实体
        SeckillProduct seckillProduct = convertToEntity(seckillProductDTO);
        seckillProduct.setId(seckillId);

        // 设置默认值
        if (seckillProduct.getLimitQuantity() == null) {
            seckillProduct.setLimitQuantity(1);
        }
        if (seckillProduct.getStatus() == null) {
            // 根据时间自动设置状态
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(seckillProduct.getStartTime())) {
                seckillProduct.setStatus(ProductConstants.SeckillStatus.NOT_STARTED);
            } else if (now.isAfter(seckillProduct.getEndTime())) {
                seckillProduct.setStatus(ProductConstants.SeckillStatus.ENDED);
            } else {
                seckillProduct.setStatus(ProductConstants.SeckillStatus.IN_PROGRESS);
            }
        }
        if (seckillProduct.getSortOrder() == null) {
            seckillProduct.setSortOrder(0);
        }

        // 保存到数据库
        seckillProductMapper.insert(seckillProduct);

        // 删除相关缓存（旁路策略：先更新数据库，再删除缓存）
        deleteSeckillProductCache(seckillId, seckillProductDTO.getSkuId());

        log.info("创建秒杀商品成功 - seckillId: {}, skuId: {}", seckillId, seckillProductDTO.getSkuId());
        return seckillId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSeckillProduct(SeckillProductDTO seckillProductDTO) {
        if (seckillProductDTO.getId() == null) {
            throw new ValidationException("SECKILL_ID_REQUIRED", "秒杀商品ID不能为空");
        }

        // 验证秒杀商品是否存在
        SeckillProduct existing = seckillProductMapper.selectById(seckillProductDTO.getId());
        if (existing == null) {
            throw new BusinessException("SECKILL_NOT_FOUND", "秒杀商品不存在");
        }

        // 参数验证
        validateSeckillProduct(seckillProductDTO);

        // 验证时间范围
        if (seckillProductDTO.getStartTime().isAfter(seckillProductDTO.getEndTime())) {
            throw new ValidationException("TIME_RANGE_INVALID", "开始时间不能晚于结束时间");
        }

        // 转换为实体并更新
        SeckillProduct seckillProduct = convertToEntity(seckillProductDTO);
        seckillProduct.setId(seckillProductDTO.getId());

        // 根据时间自动更新状态
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(seckillProduct.getStartTime())) {
            seckillProduct.setStatus(ProductConstants.SeckillStatus.NOT_STARTED);
        } else if (now.isAfter(seckillProduct.getEndTime())) {
            seckillProduct.setStatus(ProductConstants.SeckillStatus.ENDED);
        } else {
            seckillProduct.setStatus(ProductConstants.SeckillStatus.IN_PROGRESS);
        }

        int updated = seckillProductMapper.updateById(seckillProduct);
        
        // 删除相关缓存（旁路策略：先更新数据库，再删除缓存）
        if (updated > 0) {
            deleteSeckillProductCache(seckillProductDTO.getId(), seckillProductDTO.getSkuId());
        }
        
        log.info("更新秒杀商品成功 - seckillId: {}", seckillProductDTO.getId());
        return updated > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSeckillProduct(Long id) {
        if (id == null) {
            throw new ValidationException("SECKILL_ID_REQUIRED", "秒杀商品ID不能为空");
        }

        SeckillProduct seckillProduct = seckillProductMapper.selectById(id);
        if (seckillProduct == null) {
            throw new BusinessException("SECKILL_NOT_FOUND", "秒杀商品不存在");
        }

        // 逻辑删除
        int deleted = seckillProductMapper.deleteById(id);
        
        // 删除相关缓存（旁路策略：先更新数据库，再删除缓存）
        if (deleted > 0) {
            deleteSeckillProductCache(id, seckillProduct.getSkuId());
        }
        
        log.info("删除秒杀商品成功 - seckillId: {}", id);
        return deleted > 0;
    }

    @Override
    public SeckillProductDTO getSeckillProductById(Long id) {
        if (id == null) {
            throw new ValidationException("SECKILL_ID_REQUIRED", "秒杀商品ID不能为空");
        }

        // 1. 先查缓存（Cache-Aside模式）
        String cacheKey = getSeckillProductDetailKey(id);
        Object cachedObj = cacheManager.get(cacheKey);
        if (cachedObj != null) {
            log.debug("从缓存获取秒杀商品详情 - seckillId: {}", id);
            // 处理反序列化问题：Redis反序列化时可能是LinkedHashMap
            SeckillProductDTO cached = convertCachedDTO(cachedObj);
            if (cached != null) {
                return cached;
            }
            // 如果转换失败，删除缓存，重新查询
            cacheManager.delete(cacheKey);
        }

        // 2. 缓存未命中，查询数据库
        SeckillProduct seckillProduct = seckillProductMapper.selectById(id);
        if (seckillProduct == null) {
            // 防止缓存穿透：设置空值缓存
            cacheManager.setNullValue(cacheKey, ProductConstants.Cache.SECKILL_PRODUCT_CACHE_EXPIRE);
            return null;
        }

        // 3. 转换为DTO并写入缓存
        SeckillProductDTO dto = convertToDTO(seckillProduct);
        cacheManager.set(cacheKey, dto, ProductConstants.Cache.SECKILL_PRODUCT_CACHE_EXPIRE);
        log.debug("秒杀商品详情写入缓存 - seckillId: {}", id);

        return dto;
    }

    @Override
    public SeckillProductDTO getSeckillProductBySkuId(Long skuId) {
        if (skuId == null) {
            throw new ValidationException("SKU_ID_REQUIRED", "SKU ID不能为空");
        }

        // 1. 先查缓存（Cache-Aside模式）
        String cacheKey = getSeckillProductSkuKey(skuId);
        Object cachedObj = cacheManager.get(cacheKey);
        if (cachedObj != null) {
            log.debug("从缓存获取秒杀商品（SKU） - skuId: {}", skuId);
            // 处理反序列化问题：Redis反序列化时可能是LinkedHashMap
            SeckillProductDTO cached = convertCachedDTO(cachedObj);
            if (cached != null) {
                return cached;
            }
            // 如果转换失败，删除缓存，重新查询
            cacheManager.delete(cacheKey);
        }

        // 2. 缓存未命中，查询数据库
        SeckillProduct seckillProduct = seckillProductMapper.selectOne(
                new LambdaQueryWrapper<SeckillProduct>()
                        .eq(SeckillProduct::getSkuId, skuId)
                        .eq(SeckillProduct::getIsDeleted, 0)
        );

        if (seckillProduct == null) {
            // 防止缓存穿透：设置空值缓存
            cacheManager.setNullValue(cacheKey, ProductConstants.Cache.SECKILL_PRODUCT_CACHE_EXPIRE);
            return null;
        }

        // 3. 转换为DTO并写入缓存
        SeckillProductDTO dto = convertToDTO(seckillProduct);
        cacheManager.set(cacheKey, dto, ProductConstants.Cache.SECKILL_PRODUCT_CACHE_EXPIRE);
        log.debug("秒杀商品（SKU）写入缓存 - skuId: {}", skuId);

        return dto;
    }

    @Override
    public Page<SeckillProductDTO> getSeckillProductsByPage(int pageNum, int pageSize, Integer status) {
        // 验证并修正分页参数
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1 || pageSize > 100) {
            pageSize = 10; // 默认每页10条，最大100条
        }

        // 1. 先查缓存（Cache-Aside模式）
        String cacheKey = getSeckillProductPageKey(pageNum, pageSize, status);
        Object cachedObj = cacheManager.get(cacheKey);
        if (cachedObj != null) {
            log.debug("从缓存获取秒杀商品分页列表 - pageNum: {}, pageSize: {}, status: {}", pageNum, pageSize, status);
            // 处理反序列化问题：Redis反序列化时，Page中的records可能是LinkedHashMap
            Page<SeckillProductDTO> cachedPage = convertCachedPage(cachedObj);
            if (cachedPage != null) {
                return cachedPage;
            }
            // 如果转换失败，删除缓存，重新查询
            cacheManager.delete(cacheKey);
        }

        // 2. 缓存未命中，查询数据库
        // 构建分页对象
        Page<SeckillProduct> page = new Page<>(pageNum, pageSize);
        
        // 构建查询条件
        LambdaQueryWrapper<SeckillProduct> wrapper = new LambdaQueryWrapper<SeckillProduct>()
                .eq(SeckillProduct::getIsDeleted, 0)
                .orderByDesc(SeckillProduct::getCreateTime);

        if (status != null) {
            wrapper.eq(SeckillProduct::getStatus, status);
        }

        // 执行分页查询
        Page<SeckillProduct> result = seckillProductMapper.selectPage(page, wrapper);

        // 转换为DTO分页结果，确保包含完整的分页信息
        Page<SeckillProductDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<SeckillProductDTO> dtoList = result.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);

        // 3. 写入缓存（即使为空列表也缓存，防止缓存穿透）
        cacheManager.set(cacheKey, dtoPage, ProductConstants.Cache.SECKILL_PRODUCT_PAGE_CACHE_EXPIRE);
        log.debug("秒杀商品分页列表写入缓存 - pageNum: {}, pageSize: {}, status: {}, total: {}, records: {}", 
                pageNum, pageSize, status, dtoPage.getTotal(), dtoList.size());

        return dtoPage;
    }

    @Override
    public List<SeckillProductDTO> getInProgressSeckillProducts() {
        // 1. 先查缓存（Cache-Aside模式）
        String cacheKey = ProductConstants.Cache.SECKILL_PRODUCT_IN_PROGRESS_KEY;
        Object cachedObj = cacheManager.get(cacheKey);
        if (cachedObj != null) {
            log.debug("从缓存获取进行中的秒杀商品列表");
            // 处理反序列化问题：Redis反序列化时，List中的元素可能是LinkedHashMap
            List<SeckillProductDTO> cachedList = convertCachedList(cachedObj);
            if (cachedList != null && !cachedList.isEmpty()) {
                return cachedList;
            }
            // 如果转换失败，删除缓存，重新查询
            cacheManager.delete(cacheKey);
        }

        // 2. 缓存未命中，查询数据库
        LocalDateTime now = LocalDateTime.now();
        List<SeckillProduct> seckillProducts = seckillProductMapper.selectInProgressSeckills(now);
        List<SeckillProductDTO> dtoList = seckillProducts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        // 3. 写入缓存（即使为空列表也缓存，防止缓存穿透）
        cacheManager.set(cacheKey, dtoList, ProductConstants.Cache.SECKILL_PRODUCT_IN_PROGRESS_CACHE_EXPIRE);
        log.debug("进行中的秒杀商品列表写入缓存 - count: {}", dtoList.size());

        return dtoList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductSeckillStock(Long id, Integer quantity) {
        if (id == null) {
            throw new ValidationException("SECKILL_ID_REQUIRED", "秒杀商品ID不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new ValidationException("QUANTITY_INVALID", "扣减数量必须大于0");
        }

        // 分布式锁键
        String lockKey = "seckill:stock:deduct:" + id;
        
        // 使用分布式锁确保同一秒杀商品的扣减操作串行化
        return distributedLock.executeWithLock(lockKey, 30L, () -> {
            // 获取秒杀库存缓存键
            String stockKey = getSeckillStockKey(id);
            boolean redisPreDeducted = false;

            try {
                // 第一道防线：Redis预扣减（Lua脚本保证原子性）
                Long remainingStock = tryRedisPreDeduct(stockKey, quantity);

                if (remainingStock == null || remainingStock < 0) {
                    log.warn("Redis预扣减失败，库存不足 - seckillId: {}, quantity: {}", id, quantity);
                    throw new BusinessException("SECKILL_STOCK_INSUFFICIENT", "秒杀库存不足");
                }

                redisPreDeducted = true;
                log.debug("Redis预扣减成功 - seckillId: {}, quantity: {}, remaining: {}",
                        id, quantity, remainingStock);

                // 第二道防线：检查库存是否充足
                SeckillProduct seckillProduct = seckillProductMapper.selectById(id);
                if (seckillProduct == null) {
                    rollbackRedisStock(stockKey, quantity);
                    throw new BusinessException("SECKILL_NOT_FOUND", "秒杀商品不存在");
                }
               
                if (seckillProduct.getSeckillStock() < quantity) {
                    rollbackRedisStock(stockKey, quantity);
                    log.warn("数据库库存不足 - seckillId: {}, stock: {}, quantity: {}", 
                            id, seckillProduct.getSeckillStock(), quantity);
                    throw new BusinessException("SECKILL_STOCK_INSUFFICIENT", "秒杀库存不足");
                }

                // 第三道防线：检查版本号
                Integer currentVersion = seckillProduct.getVersion() != null ? seckillProduct.getVersion() : 0;
                int updated = seckillProductMapper.deductSeckillStock(id, quantity, currentVersion);

                if (updated > 0) {
                    // 删除相关缓存（旁路策略：先更新数据库，再删除缓存）
                    deleteSeckillProductCache(id, null);
                    
                    log.info("扣减秒杀库存成功 - seckillId: {}, quantity: {}, remaining: {}, version: {} -> {}", 
                            id, quantity, remainingStock, currentVersion, currentVersion + 1);
                    return true;
                } else {
                    // 数据库扣减失败（可能是版本号冲突或库存不足），回滚Redis
                    rollbackRedisStock(stockKey, quantity);
                    log.warn("数据库扣减失败（乐观锁冲突或库存不足），已回滚Redis - seckillId: {}, quantity: {}, version: {}", 
                            id, quantity, currentVersion);
                    throw new BusinessException("SECKILL_STOCK_INSUFFICIENT", "秒杀库存不足，请重试");
                }

            } catch (BusinessException e) {
                // 业务异常，如果已预扣减Redis则回滚
                if (redisPreDeducted) {
                    rollbackRedisStock(stockKey, quantity);
                }
                throw e;

            } catch (Exception e) {
                // 系统异常，如果已预扣减Redis则回滚
                if (redisPreDeducted) {
                    rollbackRedisStock(stockKey, quantity);
                }
                log.error("扣减秒杀库存异常 - seckillId: {}, quantity: {}", id, quantity, e);
                throw new BusinessException("SECKILL_STOCK_DEDUCT_ERROR", "扣减秒杀库存失败", e);
            }
        });
    }

    @Override
    public boolean warmupSeckillStock(Long id) {
        if (id == null) {
            throw new ValidationException("SECKILL_ID_REQUIRED", "秒杀商品ID不能为空");
        }

        SeckillProduct seckillProduct = seckillProductMapper.selectById(id);

        if (seckillProduct == null) {
            throw new BusinessException("SECKILL_NOT_FOUND", "秒杀商品不存在");
        }

        String stockKey = getSeckillStockKey(id);
        Integer stock = seckillProduct.getSeckillStock();

        // 计算过期时间（秒杀结束时间 - 当前时间 + 1小时缓冲）
        long expireTime = java.time.Duration.between(
                LocalDateTime.now(),
                seckillProduct.getEndTime()
        ).getSeconds() + 3600; // 秒杀结束后1小时过期

        if (expireTime <= 0) {
            expireTime = 3600; // 如果已过期，设置1小时过期时间
        }

        try {
            cacheManager.set(stockKey, stock, expireTime);
            log.info("预热秒杀库存成功 - seckillId: {}, stock: {}, expireTime: {}s", id, stock, expireTime);
            return true;
        } catch (Exception e) {
            log.error("预热秒杀库存失败 - seckillId: {}", id, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rollbackSeckillStock(Long id, Integer quantity) {
        if (id == null) {
            throw new ValidationException("SECKILL_ID_REQUIRED", "秒杀商品ID不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new ValidationException("QUANTITY_INVALID", "回滚数量必须大于0");
        }

        // 分布式锁键
        String lockKey = "seckill:stock:rollback:" + id;
        
        // 使用分布式锁确保同一秒杀商品的回滚操作串行化
        return distributedLock.executeWithLock(lockKey, 30L, () -> {
            String stockKey = getSeckillStockKey(id);

            try {
                // 回滚Redis库存
                cacheManager.increment(stockKey, quantity.longValue());
                log.info("回滚Redis秒杀库存成功 - seckillId: {}, quantity: {}", id, quantity);

                // 查询当前版本号（乐观锁）
                SeckillProduct seckillProduct = seckillProductMapper.selectById(id);
                if (seckillProduct == null) {
                    log.warn("秒杀商品不存在，无法回滚库存 - seckillId: {}", id);
                    throw new BusinessException("SECKILL_NOT_FOUND", "秒杀商品不存在");
                }

                // 回滚数据库库存（乐观锁）
                Integer currentVersion = seckillProduct.getVersion() != null ? seckillProduct.getVersion() : 0;
                int updated = seckillProductMapper.rollbackSeckillStock(id, quantity, currentVersion);

                if (updated > 0) {
                    // 删除相关缓存（旁路策略：先更新数据库，再删除缓存）
                    deleteSeckillProductCache(id, seckillProduct.getSkuId());
                    
                    log.info("回滚数据库秒杀库存成功 - seckillId: {}, quantity: {}, version: {} -> {}", 
                            id, quantity, currentVersion, currentVersion + 1);
                    return true;
                } else {
                    log.warn("回滚数据库秒杀库存失败（乐观锁冲突），已回滚Redis - seckillId: {}, quantity: {}, version: {}", 
                            id, quantity, currentVersion);
                    throw new BusinessException("SECKILL_STOCK_ROLLBACK_ERROR", "回滚秒杀库存失败，请重试");
                }

            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("回滚秒杀库存失败 - seckillId: {}, quantity: {}", id, quantity, e);
                throw new BusinessException("SECKILL_STOCK_ROLLBACK_ERROR", "回滚秒杀库存失败", e);
            }
        });
    }

    //私有方法，内部调用------------------------------------------------------------------

    /**
     * Redis预扣减（使用Lua脚本保证原子性）
     *
     * @param stockKey 库存缓存键
     * @param quantity 扣减数量
     * @return 扣减后的剩余库存（>=0表示成功，-1表示库存不足，null表示Redis异常）
     */
    private Long tryRedisPreDeduct(String stockKey, Integer quantity) {
        try {
            List<String> keys = new ArrayList<>();
            keys.add(stockKey);
            List<Object> args = new ArrayList<>();
            args.add(quantity);

            Long result = cacheManager.executeScriptAsLong(DEDUCT_STOCK_LUA_SCRIPT, keys, args);
            return result;
        } catch (Exception e) {
            log.warn("Redis预扣减异常，降级到数据库 - stockKey: {}, quantity: {}", stockKey, quantity, e);
            return null; // 返回null表示Redis异常，降级到数据库
        }
    }

    /**
     * 回滚Redis库存
     *
     * @param stockKey 库存缓存键
     * @param quantity 回滚数量
     */
    private void rollbackRedisStock(String stockKey, Integer quantity) {
        try {
            cacheManager.increment(stockKey, quantity.longValue());
            log.debug("回滚Redis库存成功 - stockKey: {}, quantity: {}", stockKey, quantity);
        } catch (Exception e) {
            log.error("回滚Redis库存失败 - stockKey: {}, quantity: {}", stockKey, quantity, e);
            // 回滚失败不影响主流程，记录日志即可
        }
    }

    /**
     * 获取秒杀库存缓存键
     *
     * @param seckillProductId 秒杀商品ID
     * @return 缓存键
     */
    private String getSeckillStockKey(Long seckillProductId) {
        return SECKILL_STOCK_KEY_PREFIX + seckillProductId;
    }

    /**
     * 获取秒杀商品详情缓存键
     *
     * @param seckillProductId 秒杀商品ID
     * @return 缓存键
     */
    private String getSeckillProductDetailKey(Long seckillProductId) {
        return ProductConstants.Cache.SECKILL_PRODUCT_DETAIL_PREFIX + seckillProductId;
    }

    /**
     * 获取秒杀商品SKU缓存键
     *
     * @param skuId SKU ID
     * @return 缓存键
     */
    private String getSeckillProductSkuKey(Long skuId) {
        return ProductConstants.Cache.SECKILL_PRODUCT_SKU_PREFIX + skuId;
    }

    /**
     * 获取秒杀商品分页查询缓存键
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param status   状态（可选）
     * @return 缓存键
     */
    private String getSeckillProductPageKey(int pageNum, int pageSize, Integer status) {
        String statusStr = status != null ? String.valueOf(status) : "all";
        return ProductConstants.Cache.SECKILL_PRODUCT_PAGE_PREFIX + pageNum + ":" + pageSize + ":" + statusStr;
    }

    /**
     * 删除秒杀商品相关缓存（旁路策略）
     * <p>
     * 删除所有可能相关的缓存键，包括：
     * - 商品详情缓存（根据ID）
     * - SKU缓存（根据SKU ID）
     * - 进行中的秒杀商品列表缓存
     * - 分页查询缓存（删除所有分页缓存，因为数据已变化）
     *
     * @param seckillProductId 秒杀商品ID
     * @param skuId SKU ID（可选，如果为null则不删除SKU缓存）
     */
    private void deleteSeckillProductCache(Long seckillProductId, Long skuId) {
        try {
            // 删除商品详情缓存
            if (seckillProductId != null) {
                String detailKey = getSeckillProductDetailKey(seckillProductId);
                cacheManager.delete(detailKey);
                log.debug("删除秒杀商品详情缓存 - seckillId: {}", seckillProductId);
            }

            // 删除SKU缓存
            if (skuId != null) {
                String skuKey = getSeckillProductSkuKey(skuId);
                cacheManager.delete(skuKey);
                log.debug("删除秒杀商品SKU缓存 - skuId: {}", skuId);
            }

            // 删除进行中的秒杀商品列表缓存（因为列表可能已变化）
            cacheManager.delete(ProductConstants.Cache.SECKILL_PRODUCT_IN_PROGRESS_KEY);
            log.debug("删除进行中的秒杀商品列表缓存");

            // 删除分页查询缓存（使用通配符删除所有分页缓存）
            // 注意：这里使用简单的删除策略，实际可以使用Redis的KEYS或SCAN命令批量删除
            // 由于分页缓存键较多，这里只删除常见的分页缓存
            // 更完善的方案是使用Redis的KEYS或SCAN命令，但性能开销较大
            // 当前策略：依赖缓存过期时间自动清理，或通过定时任务清理
            log.debug("分页查询缓存将在过期后自动清理，或通过定时任务清理");

        } catch (Exception e) {
            log.warn("删除秒杀商品缓存失败 - seckillId: {}, skuId: {}", seckillProductId, skuId, e);
            // 缓存删除失败不影响主流程，只记录日志
        }
    }

    /**
     * 验证秒杀商品参数
     */
    private void validateSeckillProduct(SeckillProductDTO dto) {
        if (dto.getProductId() == null) {
            throw new ValidationException("PRODUCT_ID_REQUIRED", "商品ID不能为空");
        }
        if (dto.getSkuId() == null) {
            throw new ValidationException("SKU_ID_REQUIRED", "SKU ID不能为空");
        }
        if (dto.getSeckillPrice() == null || dto.getSeckillPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new ValidationException("SECKILL_PRICE_INVALID", "秒杀价格必须大于0");
        }
        if (dto.getSeckillStock() == null || dto.getSeckillStock() <= 0) {
            throw new ValidationException("SECKILL_STOCK_INVALID", "秒杀库存必须大于0");
        }
        if (dto.getStartTime() == null) {
            throw new ValidationException("START_TIME_REQUIRED", "开始时间不能为空");
        }
        if (dto.getEndTime() == null) {
            throw new ValidationException("END_TIME_REQUIRED", "结束时间不能为空");
        }
    }

    /**
     * 转换为实体
     */
    private SeckillProduct convertToEntity(SeckillProductDTO dto) {
        SeckillProduct entity = new SeckillProduct();
        BeanUtil.copyProperties(dto, entity);
        return entity;
    }

    /**
     * 转换为DTO
     */
    private SeckillProductDTO convertToDTO(SeckillProduct entity) {
        SeckillProductDTO dto = new SeckillProductDTO();
        BeanUtil.copyProperties(entity, dto);
        return dto;
    }

    /**
     * 转换缓存的DTO对象（处理反序列化问题）
     * <p>
     * Redis反序列化时，DTO对象可能是LinkedHashMap而不是SeckillProductDTO
     * 需要手动转换为SeckillProductDTO
     *
     * @param cachedObj 从缓存获取的对象
     * @return 转换后的DTO对象，如果转换失败返回null
     */
    private SeckillProductDTO convertCachedDTO(Object cachedObj) {
        try {
            // 处理空值缓存
            if (cachedObj == null) {
                return null;
            }
            
            // 检查是否是空值缓存标记
            String nullValue = com.xpcjsu.sunshinemall.framework.cache.constant.CacheConstant.NULL_VALUE;
            if (nullValue.equals(cachedObj) || "NULL".equals(cachedObj)) {
                return null;
            }
            
            if (cachedObj instanceof SeckillProductDTO) {
                // 已经是SeckillProductDTO类型，直接返回
                return (SeckillProductDTO) cachedObj;
            } else if (cachedObj instanceof java.util.LinkedHashMap) {
                // 是LinkedHashMap，需要转换为SeckillProductDTO
                SeckillProductDTO dto = new SeckillProductDTO();
                BeanUtil.copyProperties(cachedObj, dto);
                return dto;
            } else {
                log.warn("缓存对象类型不支持 - type: {}, value: {}", cachedObj.getClass().getName(), cachedObj);
                return null;
            }
        } catch (Exception e) {
            log.error("转换缓存DTO对象失败 - type: {}, error: {}", 
                    cachedObj != null ? cachedObj.getClass().getName() : "null", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 转换缓存的List对象（处理反序列化问题）
     * <p>
     * Redis反序列化时，List中的元素可能是LinkedHashMap而不是SeckillProductDTO
     * 需要手动转换为SeckillProductDTO
     *
     * @param cachedObj 从缓存获取的对象
     * @return 转换后的List对象，如果转换失败返回null
     */
    @SuppressWarnings("unchecked")
    private List<SeckillProductDTO> convertCachedList(Object cachedObj) {
        try {
            if (!(cachedObj instanceof List)) {
                log.warn("缓存对象不是List类型 - type: {}", cachedObj.getClass().getName());
                return null;
            }

            List<?> cachedList = (List<?>) cachedObj;
            if (cachedList.isEmpty()) {
                return new ArrayList<>();
            }

            // 检查第一个元素的类型
            Object firstElement = cachedList.get(0);
            List<SeckillProductDTO> dtoList;

            if (firstElement instanceof SeckillProductDTO) {
                // 已经是SeckillProductDTO类型，直接转换
                dtoList = (List<SeckillProductDTO>) cachedList;
            } else if (firstElement instanceof java.util.LinkedHashMap) {
                // 是LinkedHashMap，需要转换为SeckillProductDTO
                dtoList = cachedList.stream()
                        .map(element -> {
                            if (element instanceof java.util.LinkedHashMap) {
                                SeckillProductDTO dto = new SeckillProductDTO();
                                BeanUtil.copyProperties(element, dto);
                                return dto;
                            } else {
                                log.warn("缓存元素类型异常 - type: {}", element.getClass().getName());
                                return null;
                            }
                        })
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());
            } else {
                log.warn("缓存元素类型不支持 - type: {}", firstElement.getClass().getName());
                return null;
            }

            return dtoList;
        } catch (Exception e) {
            log.error("转换缓存List对象失败", e);
            return null;
        }
    }

    /**
     * 转换缓存的Page对象（处理反序列化问题）
     * <p>
     * Redis反序列化时，Page中的records列表中的元素可能是LinkedHashMap而不是SeckillProductDTO
     * 需要手动转换为SeckillProductDTO
     *
     * @param cachedObj 从缓存获取的对象
     * @return 转换后的Page对象，如果转换失败返回null
     */
    @SuppressWarnings("unchecked")
    private Page<SeckillProductDTO> convertCachedPage(Object cachedObj) {
        try {
            if (!(cachedObj instanceof Page)) {
                log.warn("缓存对象不是Page类型 - type: {}", cachedObj.getClass().getName());
                return null;
            }

            Page<?> cachedPage = (Page<?>) cachedObj;
            List<?> records = cachedPage.getRecords();
            if (records == null || records.isEmpty()) {
                // 空列表，直接返回
                Page<SeckillProductDTO> dtoPage = new Page<>(cachedPage.getCurrent(), cachedPage.getSize(), cachedPage.getTotal());
                dtoPage.setRecords(new ArrayList<>());
                return dtoPage;
            }

            // 检查第一个元素的类型
            Object firstRecord = records.get(0);
            List<SeckillProductDTO> dtoList;

            if (firstRecord instanceof SeckillProductDTO) {
                // 已经是SeckillProductDTO类型，直接转换
                dtoList = (List<SeckillProductDTO>) records;
            } else if (firstRecord instanceof java.util.LinkedHashMap) {
                // 是LinkedHashMap，需要转换为SeckillProductDTO
                dtoList = records.stream()
                        .map(record -> {
                            if (record instanceof java.util.LinkedHashMap) {
                                SeckillProductDTO dto = new SeckillProductDTO();
                                BeanUtil.copyProperties(record, dto);
                                return dto;
                            } else {
                                log.warn("缓存记录类型异常 - type: {}", record.getClass().getName());
                                return null;
                            }
                        })
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());
            } else {
                log.warn("缓存记录类型不支持 - type: {}", firstRecord.getClass().getName());
                return null;
            }

            // 重新构建Page对象
            Page<SeckillProductDTO> dtoPage = new Page<>(cachedPage.getCurrent(), cachedPage.getSize(), cachedPage.getTotal());
            dtoPage.setRecords(dtoList);
            return dtoPage;
        } catch (Exception e) {
            log.error("转换缓存Page对象失败", e);
            return null;
        }
    }

}

