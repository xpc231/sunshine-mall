package com.xpcjsu.sunshinemall.product.service.impl;

import cn.hutool.core.bean.BeanUtil;
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
        log.info("删除秒杀商品成功 - seckillId: {}", id);
        return deleted > 0;
    }

    @Override
    public SeckillProductDTO getSeckillProductById(Long id) {
        if (id == null) {
            throw new ValidationException("SECKILL_ID_REQUIRED", "秒杀商品ID不能为空");
        }

        SeckillProduct seckillProduct = seckillProductMapper.selectById(id);
        if (seckillProduct == null) {
            return null;
        }

        return convertToDTO(seckillProduct);
    }

    @Override
    public SeckillProductDTO getSeckillProductBySkuId(Long skuId) {
        if (skuId == null) {
            throw new ValidationException("SKU_ID_REQUIRED", "SKU ID不能为空");
        }

        SeckillProduct seckillProduct = seckillProductMapper.selectOne(
                new LambdaQueryWrapper<SeckillProduct>()
                        .eq(SeckillProduct::getSkuId, skuId)
                        .eq(SeckillProduct::getIsDeleted, 0)
        );

        if (seckillProduct == null) {
            return null;
        }

        return convertToDTO(seckillProduct);
    }

    @Override
    public Page<SeckillProductDTO> getSeckillProductsByPage(int pageNum, int pageSize, Integer status) {
        Page<SeckillProduct> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SeckillProduct> wrapper = new LambdaQueryWrapper<SeckillProduct>()
                .eq(SeckillProduct::getIsDeleted, 0)
                .orderByDesc(SeckillProduct::getCreateTime);

        if (status != null) {
            wrapper.eq(SeckillProduct::getStatus, status);
        }

        Page<SeckillProduct> result = seckillProductMapper.selectPage(page, wrapper);

        // 转换为DTO
        Page<SeckillProductDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<SeckillProductDTO> dtoList = result.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);

        return dtoPage;
    }

    @Override
    public List<SeckillProductDTO> getInProgressSeckillProducts() {
        LocalDateTime now = LocalDateTime.now();
        List<SeckillProduct> seckillProducts = seckillProductMapper.selectInProgressSeckills(now);
        return seckillProducts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
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
            log.debug("Redis预扣减成功 - seckillId: {}, quantity: {}, remaining: {}", id, quantity, remainingStock);

            // 第二道防线：数据库扣减（最终保障）
            int updated = seckillProductMapper.deductSeckillStock(id, quantity);

            if (updated > 0) {
                log.info("扣减秒杀库存成功 - seckillId: {}, quantity: {}, remaining: {}", id, quantity, remainingStock);
                return true;
            } else {
                // 数据库扣减失败，回滚Redis
                rollbackRedisStock(stockKey, quantity);
                log.warn("数据库扣减失败，已回滚Redis - seckillId: {}, quantity: {}", id, quantity);
                throw new BusinessException("SECKILL_STOCK_INSUFFICIENT", "秒杀库存不足");
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
    public boolean rollbackSeckillStock(Long id, Integer quantity) {
        if (id == null) {
            throw new ValidationException("SECKILL_ID_REQUIRED", "秒杀商品ID不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new ValidationException("QUANTITY_INVALID", "回滚数量必须大于0");
        }

        String stockKey = getSeckillStockKey(id);

        try {
            // 回滚Redis库存
            cacheManager.increment(stockKey, quantity.longValue());
            log.info("回滚Redis秒杀库存成功 - seckillId: {}, quantity: {}", id, quantity);

            // 回滚数据库库存
            // 注意：这里使用简单的UPDATE，实际生产环境可能需要更严格的校验
            SeckillProduct seckillProduct = seckillProductMapper.selectById(id);

            if (seckillProduct != null) {
                seckillProduct.setSeckillStock(seckillProduct.getSeckillStock() + quantity);
                seckillProductMapper.updateById(seckillProduct);
                log.info("回滚数据库秒杀库存成功 - seckillId: {}, quantity: {}", id, quantity);
            }

            return true;
        } catch (Exception e) {

            log.error("回滚秒杀库存失败 - seckillId: {}, quantity: {}", id, quantity, e);
            throw new BusinessException("SECKILL_STOCK_ROLLBACK_ERROR", "回滚秒杀库存失败", e);
        }
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

}

