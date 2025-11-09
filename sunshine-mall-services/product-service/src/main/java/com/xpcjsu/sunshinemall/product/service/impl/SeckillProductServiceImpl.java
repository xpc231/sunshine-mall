package com.xpcjsu.sunshinemall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import com.xpcjsu.sunshinemall.product.util.CacheBreakdownProtection;
import com.xpcjsu.sunshinemall.product.helper.SeckillProductCacheHelper;
import com.xpcjsu.sunshinemall.product.helper.SeckillProductStockHelper;
import com.xpcjsu.sunshinemall.product.helper.SeckillProductConverter;
import com.xpcjsu.sunshinemall.product.helper.SeckillProductValidator;
import com.xpcjsu.sunshinemall.product.util.CacheExpireTimeManager;
import com.xpcjsu.sunshinemall.product.util.SeckillProductParamValidator;
import com.xpcjsu.sunshinemall.product.util.SeckillProductBloomFilter;
import com.xpcjsu.sunshinemall.framework.base.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.xpcjsu.sunshinemall.product.constant.ProductConstants.Cache.DISTRIBUTED_LOCK_DEFAULT_EXPIRE_TIME;
import static com.xpcjsu.sunshinemall.product.constant.ProductConstants.Cache.SECKILL_STOCK_LOCK_DEDUCT_PREFIX;


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
    private final CacheBreakdownProtection cacheBreakdownProtection;
    private final SeckillProductCacheHelper cacheHelper;
    private final SeckillProductStockHelper stockHelper;
    private final SeckillProductConverter converter;
    private final SeckillProductValidator validator;
    private final CacheExpireTimeManager expireTimeManager;
    private final SeckillProductBloomFilter bloomFilter;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSeckillProduct(SeckillProductDTO seckillProductDTO) {
        // 参数验证
        validator.validateSeckillProduct(seckillProductDTO);

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
        SeckillProduct seckillProduct = converter.convertToEntity(seckillProductDTO);
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

        // 添加到布隆过滤器（防止缓存穿透）
        bloomFilter.addSeckillProductId(seckillId);
        bloomFilter.addSkuId(seckillProductDTO.getSkuId());

        // 删除相关缓存（旁路策略：先更新数据库，再删除缓存）
        cacheHelper.deleteSeckillProductCache(seckillId, seckillProductDTO.getSkuId());

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
        validator.validateSeckillProduct(seckillProductDTO);

        // 验证时间范围
        if (seckillProductDTO.getStartTime().isAfter(seckillProductDTO.getEndTime())) {
            throw new ValidationException("TIME_RANGE_INVALID", "开始时间不能晚于结束时间");
        }

        // 转换为实体并更新
        SeckillProduct seckillProduct = converter.convertToEntity(seckillProductDTO);
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
            cacheHelper.deleteSeckillProductCache(seckillProductDTO.getId(), seckillProductDTO.getSkuId());
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
            cacheHelper.deleteSeckillProductCache(id, seckillProduct.getSkuId());
        }
        
        log.info("删除秒杀商品成功 - seckillId: {}", id);
        return deleted > 0;
    }

    @Override
    public SeckillProductDTO getSeckillProductById(Long id) {
        // 第一道防线：参数校验（防止恶意构造的无效ID）
        SeckillProductParamValidator.validateSeckillProductId(id);

        // 第二道防线：布隆过滤器拦截（如果不存在则直接返回）
        if (!bloomFilter.mightExist(id)) {
            log.warn("布隆过滤器拦截 - 秒杀商品ID不存在: {}", id);
            throw new BusinessException("SECKILL_NOT_FOUND", "秒杀商品不存在");
        }

        // 第三道防线：使用分布式锁 + 双重检查方案防止缓存击穿
        String cacheKey = cacheHelper.getSeckillProductDetailKey(id);
        String lockKey = ProductConstants.Cache.SECKILL_PRODUCT_LOCK_DETAIL_PREFIX + id;

        return cacheBreakdownProtection.getWithDistributedLock(
                cacheKey,
                lockKey,
                () -> {
                    // 查询数据库
                    SeckillProduct seckillProduct = seckillProductMapper.selectById(id);
                    if (seckillProduct == null) {
                        return null;
                    }
                    // 转换为DTO
                    return converter.convertToDTO(seckillProduct);
                },
                ProductConstants.Cache.SECKILL_PRODUCT_CACHE_EXPIRE,
                converter::convertCachedDTO // 处理反序列化问题
        );
    }

    @Override
    public SeckillProductDTO getSeckillProductBySkuId(Long skuId) {
        // 第一道防线：参数校验（防止恶意构造的无效SKU ID）
        SeckillProductParamValidator.validateSkuId(skuId);

        // 第二道防线：布隆过滤器拦截（如果不存在则直接返回）
        if (!bloomFilter.skuMightExist(skuId)) {
            log.warn("布隆过滤器拦截 - SKU ID不存在: {}", skuId);
            throw new BusinessException("SECKILL_NOT_FOUND", "秒杀商品不存在");
        }

        // 第三道防线：使用分布式锁 + 双重检查方案防止缓存击穿
        String cacheKey = cacheHelper.getSeckillProductSkuKey(skuId);
        String lockKey = ProductConstants.Cache.SECKILL_PRODUCT_LOCK_SKU_PREFIX + skuId;

        return cacheBreakdownProtection.getWithDistributedLock(
                cacheKey,
                lockKey,
                () -> {
                    // 查询数据库
                    SeckillProduct seckillProduct = seckillProductMapper.selectOne(
                            new LambdaQueryWrapper<SeckillProduct>()
                                    .eq(SeckillProduct::getSkuId, skuId)
                                    .eq(SeckillProduct::getIsDeleted, 0)
                    );
                    if (seckillProduct == null) {
                        return null;
                    }
                    // 转换为DTO
                    return converter.convertToDTO(seckillProduct);
                },
                ProductConstants.Cache.SECKILL_PRODUCT_CACHE_EXPIRE,
                converter::convertCachedDTO // 处理反序列化问题
        );
    }

    @Override
    public Page<SeckillProductDTO> getSeckillProductsByPage(int pageNum, int pageSize, Integer status) {
        // 第一道防线：参数校验
        SeckillProductParamValidator.validatePageParams(pageNum, pageSize);

        // 1. 先查缓存
        String cacheKey = cacheHelper.getSeckillProductPageKey(pageNum, pageSize, status);
        Object cachedObj = cacheManager.get(cacheKey);
        if (cachedObj != null) {
            log.debug("从缓存获取秒杀商品分页列表 - pageNum: {}, pageSize: {}, status: {}", pageNum, pageSize, status);
            // 处理反序列化问题：Redis反序列化时，Page中的records可能是LinkedHashMap
            try {
                Page<SeckillProductDTO> cachedPage = converter.convertCachedPage(cachedObj);
                if (cachedPage != null) {
                    return cachedPage;
                }
                // 如果转换失败，删除缓存，重新查询
                cacheManager.delete(cacheKey);
            } catch (Exception e) {
                log.warn("缓存转换失败，删除缓存key: {}", cacheKey, e);
                cacheManager.delete(cacheKey);
            }
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
                .map(converter::convertToDTO)
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);

        // 3. 写入缓存（即使为空列表也缓存，防止缓存穿透）
        // 使用随机过期时间，避免缓存雪崩
        long randomExpireTime = expireTimeManager.getRandomExpireTime(
                CacheExpireTimeManager.CacheType.SECKILL_PRODUCT_PAGE);
        cacheManager.set(cacheKey, dtoPage, randomExpireTime);
        log.debug("秒杀商品分页列表写入缓存 - pageNum: {}, pageSize: {}, status: {}, total: {}, records: {}", 
                pageNum, pageSize, status, dtoPage.getTotal(), dtoList.size());

        return dtoPage;
    }

    @Override
    public List<SeckillProductDTO> getInProgressSeckillProducts() {
        // 使用永不过期 + 异步刷新方案防止缓存击穿（适用于热点数据）
        String cacheKey = ProductConstants.Cache.SECKILL_PRODUCT_IN_PROGRESS_KEY;
        
        return cacheBreakdownProtection.getWithNeverExpire(
                cacheKey,
                () -> {
                    // 查询数据库
                    LocalDateTime now = LocalDateTime.now();
                    List<SeckillProduct> seckillProducts = seckillProductMapper.selectInProgressSeckills(now);
                    return seckillProducts.stream()
                            .map(converter::convertToDTO)
                            .collect(Collectors.toList());
                },
                ProductConstants.Cache.SECKILL_PRODUCT_IN_PROGRESS_CACHE_EXPIRE, // 逻辑过期时间
                this::refreshInProgressSeckillProductsCache, // 异步刷新任务
                converter::convertCachedList // 处理反序列化问题
        );
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
        String lockKey = SECKILL_STOCK_LOCK_DEDUCT_PREFIX + id;
        
        // 使用分布式锁确保同一秒杀商品的扣减操作串行化
        return distributedLock.executeWithLock(lockKey, DISTRIBUTED_LOCK_DEFAULT_EXPIRE_TIME, () -> {
            // 获取秒杀库存缓存键
            String stockKey = cacheHelper.getSeckillStockKey(id);
            boolean redisPreDeducted = false;

            try {
                // 第一道防线：Redis预扣减（Lua脚本保证原子性）
                Long remainingStock = stockHelper.tryRedisPreDeduct(stockKey, quantity);

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
                    stockHelper.rollbackRedisStock(stockKey, quantity);
                    throw new BusinessException("SECKILL_NOT_FOUND", "秒杀商品不存在");
                }
               
                if (seckillProduct.getSeckillStock() < quantity) {
                    stockHelper.rollbackRedisStock(stockKey, quantity);
                    log.warn("数据库库存不足 - seckillId: {}, stock: {}, quantity: {}", 
                            id, seckillProduct.getSeckillStock(), quantity);
                    throw new BusinessException("SECKILL_STOCK_INSUFFICIENT", "秒杀库存不足");
                }

                // 第三道防线：检查版本号
                Integer currentVersion = seckillProduct.getVersion() != null ? seckillProduct.getVersion() : 0;
                int updated = seckillProductMapper.deductSeckillStock(id, quantity, currentVersion);

                if (updated > 0) {
                    // 删除相关缓存（旁路策略：先更新数据库，再删除缓存）
                    cacheHelper.deleteSeckillProductCache(id, null);
                    
                    log.info("扣减秒杀库存成功 - seckillId: {}, quantity: {}, remaining: {}, version: {} -> {}", 
                            id, quantity, remainingStock, currentVersion, currentVersion + 1);
                    return true;
                } else {
                    // 数据库扣减失败（可能是版本号冲突或库存不足），回滚Redis
                    stockHelper.rollbackRedisStock(stockKey, quantity);
                    log.warn("数据库扣减失败（乐观锁冲突或库存不足），已回滚Redis - seckillId: {}, quantity: {}, version: {}", 
                            id, quantity, currentVersion);
                    throw new BusinessException("SECKILL_STOCK_INSUFFICIENT", "秒杀库存不足，请重试");
                }

            } catch (BusinessException e) {
                // 业务异常，如果已预扣减Redis则回滚
                if (redisPreDeducted) {
                    stockHelper.rollbackRedisStock(stockKey, quantity);
                }
                throw e;

            } catch (Exception e) {
                // 系统异常，如果已预扣减Redis则回滚
                if (redisPreDeducted) {
                    stockHelper.rollbackRedisStock(stockKey, quantity);
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

        String stockKey = cacheHelper.getSeckillStockKey(id);
        Integer stock = seckillProduct.getSeckillStock();

        // 计算过期时间（秒杀结束时间 - 当前时间 + 1小时缓冲）
        long baseExpireTime = java.time.Duration.between(
                LocalDateTime.now(),
                seckillProduct.getEndTime()
        ).getSeconds() + 3600; // 秒杀结束后1小时过期

        if (baseExpireTime <= 0) {
            baseExpireTime = 3600; // 如果已过期，设置1小时过期时间
        }

        // 使用随机过期时间，避免缓存雪崩
        long randomExpireTime = expireTimeManager.getRandomExpireTime(baseExpireTime);

        try {
            cacheManager.set(stockKey, stock, randomExpireTime);
            log.info("预热秒杀库存成功 - seckillId: {}, stock: {}, baseExpireTime: {}s, randomExpireTime: {}s", 
                    id, stock, baseExpireTime, randomExpireTime);
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
        String lockKey = ProductConstants.Cache.SECKILL_STOCK_LOCK_ROLLBACK_PREFIX + id;
        
        // 使用分布式锁确保同一秒杀商品的回滚操作串行化
        return distributedLock.executeWithLock(lockKey, DISTRIBUTED_LOCK_DEFAULT_EXPIRE_TIME, () -> {
            String stockKey = cacheHelper.getSeckillStockKey(id);

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
                    cacheHelper.deleteSeckillProductCache(id, seckillProduct.getSkuId());
                    
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



    /**
     * 异步刷新进行中的秒杀商品列表缓存
     * <p>
     * 使用线程池异步执行，避免阻塞主线程
     */
    public void refreshInProgressSeckillProductsCache() {
        // 使用新线程异步执行刷新任务
        new Thread(() -> {
            try {
                log.debug("开始异步刷新进行中的秒杀商品列表缓存");
                String cacheKey = ProductConstants.Cache.SECKILL_PRODUCT_IN_PROGRESS_KEY;

                // 查询数据库
                LocalDateTime now = LocalDateTime.now();
                List<SeckillProduct> seckillProducts = seckillProductMapper.selectInProgressSeckills(now);
                List<SeckillProductDTO> dtoList = seckillProducts.stream()
                        .map(converter::convertToDTO)
                        .collect(Collectors.toList());

                // 更新缓存（永不过期）
                cacheManager.set(cacheKey, dtoList, -1L);
                log.debug("异步刷新进行中的秒杀商品列表缓存完成 - count: {}", dtoList.size());
            } catch (Exception e) {
                log.error("异步刷新进行中的秒杀商品列表缓存失败", e);
            }
        }, ProductConstants.Cache.SECKILL_CACHE_REFRESH_THREAD_NAME).start();
    }


}

