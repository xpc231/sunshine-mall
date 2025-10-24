package com.xpcjsu.sunshinemall.product.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xpcjsu.sunshinemall.framework.base.exception.BusinessException;
import com.xpcjsu.sunshinemall.framework.base.exception.SystemException;
import com.xpcjsu.sunshinemall.framework.base.exception.ValidationException;
import com.xpcjsu.sunshinemall.framework.cache.core.CacheManager;
import com.xpcjsu.sunshinemall.framework.distributedid.core.SnowflakeIdGenerator;
import com.xpcjsu.sunshinemall.product.constant.ProductConstants;
import com.xpcjsu.sunshinemall.product.dto.StockDTO;
import com.xpcjsu.sunshinemall.product.entity.ProductStock;
import com.xpcjsu.sunshinemall.product.entity.StockLog;
import com.xpcjsu.sunshinemall.framework.idempotent.annotation.Idempotent;
import com.xpcjsu.sunshinemall.product.mapper.ProductStockMapper;
import com.xpcjsu.sunshinemall.product.mapper.StockLogMapper;
import com.xpcjsu.sunshinemall.product.mq.message.StockChangeMessage;
import com.xpcjsu.sunshinemall.product.mq.producer.StockChangeProducer;
import com.xpcjsu.sunshinemall.product.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 库存服务实现
 *
 * @author xpcjsu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final ProductStockMapper productStockMapper;
    private final StockLogMapper stockLogMapper;
    private final CacheManager cacheManager;
    private final SnowflakeIdGenerator idGenerator;
    private final StockChangeProducer stockChangeProducer;

    /** 库存扣减最大重试次数 */
    private static final int MAX_RETRY_TIMES = 3;

   /**
     * 初始化指定 SKU 的库存信息。
     * <p>
     * 该方法用于为指定的 SKU 初始化库存数据，包括总库存、可用库存和锁定库存等信息。
     * 若库存已存在，则抛出业务异常；否则插入新的库存记录，并同步到缓存和消息队列中。
     * </p>
     *
     * @param skuId    SKU ID，不能为空
     * @param quantity 库存数量，必须大于等于 0
     * @return 初始化成功返回 true，失败返回 false
     * @throws ValidationException  当参数不合法时抛出（如 skuId 为空或 quantity 为负数）
     * @throws BusinessException    当库存已初始化时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean initStock(Long skuId, Integer quantity) {
        if (skuId == null) {
            throw new ValidationException("SKU_ID_REQUIRED", "SKU ID不能为空");
        }
        if (quantity == null || quantity < 0) {
            throw new ValidationException("QUANTITY_INVALID", "库存数量不能为负数");
        }

        // 检查是否已经初始化
        ProductStock existingStock = productStockMapper.selectOne(
                new LambdaQueryWrapper<ProductStock>().eq(ProductStock::getSkuId, skuId)
        );
        if (existingStock != null) {
            throw new BusinessException("STOCK_ALREADY_INIT", "库存已初始化");
        }

        // 初始化库存
        ProductStock stock = new ProductStock();
        stock.setSkuId(skuId);
        stock.setTotalStock(quantity);
        stock.setAvailableStock(quantity);
        stock.setLockedStock(0);
        stock.setVersion(0);

        //若插入成功，返回值 inserted 大于 0
        int inserted = productStockMapper.insert(stock);

        if (inserted > 0) {
            // 缓存库存信息
            try {
                cacheStockInfo(skuId, stock);
            } catch (Exception e) {
                log.error("缓存库存信息失败 - skuId: {}, quantity: {}", skuId, quantity, e);
                throw new RuntimeException(e);
            }

            // 记录日志
            recordStockLog(skuId, ProductConstants.StockOperationType.IN_STOCK,
                          quantity, 0, quantity, null, "初始化库存");

            // 发送库存变更消息
            sendStockChangeMessage(skuId, ProductConstants.StockOperationType.IN_STOCK,
                                 quantity, 0, quantity, null, "初始化库存");

            log.info("初始化库存成功 - skuId: {}, quantity: {}", skuId, quantity);
        }

        return inserted > 0;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addStock(Long skuId, Integer quantity, String remark) {
        if (skuId == null) {
            throw new ValidationException("SKU_ID_REQUIRED", "SKU ID不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new ValidationException("QUANTITY_INVALID", "增加数量必须大于0");
        }

        // 获取当前库存
        ProductStock stock = getStock(skuId);
        int beforeStock = stock.getAvailableStock();

        // 增加库存
        int updated = productStockMapper.addStock(skuId, quantity);
        if (updated == 0) {
            throw new SystemException("ADD_STOCK_FAILED", "增加库存失败");
        }

        // 清除缓存
        clearStockCache(skuId);

        // 记录日志
        recordStockLog(skuId, ProductConstants.StockOperationType.IN_STOCK,
                      quantity, beforeStock, beforeStock + quantity, null, remark);

        // 发送库存变更消息
        sendStockChangeMessage(skuId, ProductConstants.StockOperationType.IN_STOCK,
                             quantity, beforeStock, beforeStock + quantity, null, remark);

        log.info("增加库存成功 - skuId: {}, quantity: {}", skuId, quantity);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Idempotent(key = "'stock:deduct:' + #orderId + ':' + #skuId", expireTime = 300)
    public boolean deductStock(Long skuId, Integer quantity, Long orderId, String remark) {
        if (skuId == null) {
            throw new ValidationException("SKU_ID_REQUIRED", "SKU ID不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new ValidationException("QUANTITY_INVALID", "扣减数量必须大于0");
        }

        // ========== 防超卖：先在 Redis 预扣减 ==========
        String stockCacheKey = getStockCacheKey(skuId);

        try {
            // 使用 Redis decrement 原子操作预扣减
            Long remainingStock = cacheManager.decrement(stockCacheKey, quantity.longValue());

            // 如果扣减后小于0，说明库存不足，需要回滚
            if (remainingStock < 0) {
                // 回滚 Redis 库存
                cacheManager.increment(stockCacheKey, quantity.longValue());
                throw new BusinessException("STOCK_INSUFFICIENT", "库存不足");
            }
        } catch (Exception e) {
            // Redis 不可用时，降级到数据库检查（但风险较高）
            log.warn("Redis 库存预扣减失败，降级到数据库检查 - skuId: {}", skuId, e);
        }
        // ================================================

        // 使用乐观锁扣减数据库库存（带重试机制）
        for (int i = 0; i < MAX_RETRY_TIMES; i++) {
            ProductStock stock = getStock(skuId);

            // 数据库二次检查库存（双重保险）
            if (stock.getAvailableStock() < quantity) {
                // 回滚 Redis 库存
                try {
                    cacheManager.increment(stockCacheKey, quantity.longValue());
                } catch (Exception e) {
                    log.error("回滚 Redis 库存失败 - skuId: {}", skuId, e);
                }
                throw new BusinessException("STOCK_INSUFFICIENT", "库存不足");
            }

            int beforeStock = stock.getAvailableStock();

            // 执行扣减（SQL 中再次校验库存）
            int updated = productStockMapper.deductStock(skuId, quantity, stock.getVersion());
            if (updated > 0) {
                // 扣减成功，清除缓存（下次查询时重建）
                clearStockCache(skuId);

                // 记录日志
                recordStockLog(skuId, ProductConstants.StockOperationType.DEDUCT,
                              quantity, beforeStock, beforeStock - quantity, orderId, remark);

                // 发送库存变更消息
                sendStockChangeMessage(skuId, ProductConstants.StockOperationType.DEDUCT,
                                     quantity, beforeStock, beforeStock - quantity, orderId, remark);

                log.info("扣减库存成功 - skuId: {}, quantity: {}, orderId: {}", skuId, quantity, orderId);
                return true;
            }

            // 乐观锁冲突，重试
            log.warn("扣减库存乐观锁冲突，重试 - skuId: {}, retry: {}", skuId, i + 1);
        }

        // 所有重试都失败，回滚 Redis 库存
        try {
            cacheManager.increment(stockCacheKey, quantity.longValue());
        } catch (Exception e) {
            log.error("回滚 Redis 库存失败 - skuId: {}", skuId, e);
        }

        throw new SystemException("DEDUCT_STOCK_FAILED", "扣减库存失败，请重试");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Idempotent(key = "'stock:lock:' + #orderId + ':' + #skuId", expireTime = 300)
    public boolean lockStock(Long skuId, Integer quantity, Long orderId, String remark) {
        if (skuId == null) {
            throw new ValidationException("SKU_ID_REQUIRED", "SKU ID不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new ValidationException("QUANTITY_INVALID", "预占数量必须大于0");
        }

        // 使用乐观锁预占库存（带重试机制）
        for (int i = 0; i < MAX_RETRY_TIMES; i++) {
            ProductStock stock = getStock(skuId);

            // 检查库存是否充足
            if (stock.getAvailableStock() < quantity) {
                throw new BusinessException("STOCK_INSUFFICIENT", "库存不足");
            }

            int beforeStock = stock.getAvailableStock();

            // 执行预占
            int updated = productStockMapper.lockStock(skuId, quantity, stock.getVersion());
            if (updated > 0) {
                // 清除缓存
                clearStockCache(skuId);

                // 记录日志
                recordStockLog(skuId, ProductConstants.StockOperationType.LOCK,
                              quantity, beforeStock, beforeStock - quantity, orderId, remark);

                // 发送库存变更消息
                sendStockChangeMessage(skuId, ProductConstants.StockOperationType.LOCK,
                                     quantity, beforeStock, beforeStock - quantity, orderId, remark);

                log.info("预占库存成功 - skuId: {}, quantity: {}, orderId: {}", skuId, quantity, orderId);
                return true;
            }

            // 乐观锁冲突，重试
            log.warn("预占库存乐观锁冲突，重试 - skuId: {}, retry: {}", skuId, i + 1);
        }

        throw new SystemException("LOCK_STOCK_FAILED", "预占库存失败，请重试");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Idempotent(key = "'stock:unlock:' + #orderId + ':' + #skuId", expireTime = 300)
    public boolean unlockStock(Long skuId, Integer quantity, Long orderId, String remark) {
        if (skuId == null) {
            throw new ValidationException("SKU_ID_REQUIRED", "SKU ID不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new ValidationException("QUANTITY_INVALID", "释放数量必须大于0");
        }

        // 使用乐观锁释放库存（带重试机制）
        for (int i = 0; i < MAX_RETRY_TIMES; i++) {
            ProductStock stock = getStock(skuId);

            // 检查锁定库存是否充足
            if (stock.getLockedStock() < quantity) {
                throw new BusinessException("LOCKED_STOCK_INSUFFICIENT", "锁定库存不足");
            }

            int beforeStock = stock.getAvailableStock();

            // 执行释放
            int updated = productStockMapper.unlockStock(skuId, quantity, stock.getVersion());
            if (updated > 0) {
                // 清除缓存
                clearStockCache(skuId);

                // 同步增加 Redis 库存（释放操作）
                try {
                    String cacheKey = getStockCacheKey(skuId);
                    cacheManager.increment(cacheKey, quantity.longValue());
                } catch (Exception e) {
                    log.error("增加 Redis 库存失败 - skuId: {}", skuId, e);
                }

                // 记录日志
                recordStockLog(skuId, ProductConstants.StockOperationType.UNLOCK,
                              quantity, beforeStock, beforeStock + quantity, orderId, remark);

                // 发送库存变更消息
                sendStockChangeMessage(skuId, ProductConstants.StockOperationType.UNLOCK,
                                     quantity, beforeStock, beforeStock + quantity, orderId, remark);

                log.info("释放库存成功 - skuId: {}, quantity: {}, orderId: {}", skuId, quantity, orderId);
                return true;
            }

            // 乐观锁冲突，重试
            log.warn("释放库存乐观锁冲突，重试 - skuId: {}, retry: {}", skuId, i + 1);
        }

        throw new SystemException("UNLOCK_STOCK_FAILED", "释放库存失败，请重试");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Idempotent(key = "'stock:confirm:' + #orderId + ':' + #skuId", expireTime = 300)
    public boolean confirmDeduct(Long skuId, Integer quantity, Long orderId, String remark) {
        // 确认扣减：直接减少锁定库存，不增加可用库存
        if (skuId == null) {
            throw new ValidationException("SKU_ID_REQUIRED", "SKU ID不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new ValidationException("QUANTITY_INVALID", "扣减数量必须大于0");
        }

        ProductStock stock = getStock(skuId);

        // 检查锁定库存是否充足
        if (stock.getLockedStock() < quantity) {
            throw new BusinessException("LOCKED_STOCK_INSUFFICIENT", "锁定库存不足");
        }

        // 直接减少锁定库存和总库存
        int updated = productStockMapper.update(null, new LambdaUpdateWrapper<ProductStock>()
                .setSql("locked_stock = locked_stock - " + quantity)
                .setSql("total_stock = total_stock - " + quantity)
                .setSql("version = version + 1")
                .eq(ProductStock::getSkuId, skuId)
                .eq(ProductStock::getVersion, stock.getVersion()));

        if (updated > 0) {
            // 清除缓存
            clearStockCache(skuId);

            // 记录日志
            recordStockLog(skuId, ProductConstants.StockOperationType.DEDUCT,
                          quantity, stock.getAvailableStock(), stock.getAvailableStock(),
                          orderId, remark);

            // 发送库存变更消息
            sendStockChangeMessage(skuId, ProductConstants.StockOperationType.DEDUCT,
                                 quantity, stock.getAvailableStock(), stock.getAvailableStock(),
                                 orderId, remark);

            log.info("确认扣减库存成功 - skuId: {}, quantity: {}, orderId: {}", skuId, quantity, orderId);
            return true;
        }

        throw new SystemException("CONFIRM_DEDUCT_FAILED", "确认扣减失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Idempotent(key = "'stock:return:' + #orderId + ':' + #skuId", expireTime = 300)
    public boolean returnStock(Long skuId, Integer quantity, Long orderId, String remark) {
        if (skuId == null) {
            throw new ValidationException("SKU_ID_REQUIRED", "SKU ID不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new ValidationException("QUANTITY_INVALID", "退货数量必须大于0");
        }

        // 退货补库存
        ProductStock stock = getStock(skuId);
        int beforeStock = stock.getAvailableStock();

        int updated = productStockMapper.addStock(skuId, quantity);
        if (updated > 0) {
            // 清除缓存
            clearStockCache(skuId);

            // 记录日志
            recordStockLog(skuId, ProductConstants.StockOperationType.RETURN,
                          quantity, beforeStock, beforeStock + quantity, orderId, remark);

            // 发送库存变更消息
            sendStockChangeMessage(skuId, ProductConstants.StockOperationType.RETURN,
                                 quantity, beforeStock, beforeStock + quantity, orderId, remark);

            log.info("退货补库存成功 - skuId: {}, quantity: {}, orderId: {}", skuId, quantity, orderId);
            return true;
        }

        throw new SystemException("RETURN_STOCK_FAILED", "退货补库存失败");
    }

    @Override
    public StockDTO getStockBySkuId(Long skuId) {
        if (skuId == null) {
            throw new ValidationException("SKU_ID_REQUIRED", "SKU ID不能为空");
        }

        // 先从缓存获取
        String cacheKey = getStockCacheKey(skuId);
        String cacheValue = cacheManager.get(cacheKey, String.class);
        if (StringUtils.hasText(cacheValue)) {
            return JSONUtil.toBean(cacheValue, StockDTO.class);
        }

        // 从数据库查询
        ProductStock stock = getStock(skuId);
        StockDTO stockDTO = BeanUtil.copyProperties(stock, StockDTO.class);

        // 缓存库存信息
        cacheStockInfo(skuId, stock);

        return stockDTO;
    }

    @Override
    public boolean checkStock(Long skuId, Integer quantity) {
        if (skuId == null || quantity == null || quantity <= 0) {
            return false;
        }

        StockDTO stock = getStockBySkuId(skuId);
        return stock.getAvailableStock() >= quantity;
    }

    /**
     * 获取库存（从数据库）
     */
    private ProductStock getStock(Long skuId) {
        ProductStock stock = productStockMapper.selectOne(
                new LambdaQueryWrapper<ProductStock>().eq(ProductStock::getSkuId, skuId)
        );
        if (stock == null) {
            throw new BusinessException("STOCK_NOT_FOUND", "库存不存在，请先初始化库存");
        }
        return stock;
    }

    /**
     * 记录库存操作日志
     */
    private void recordStockLog(Long skuId, Integer operationType, Integer quantity,
                               Integer beforeStock, Integer afterStock, 
                               Long orderId, String remark) {
        StockLog log = new StockLog();
        log.setId(idGenerator.nextId());
        log.setSkuId(skuId);
        log.setOperationType(operationType);
        log.setQuantity(quantity);
        log.setBeforeStock(beforeStock);
        log.setAfterStock(afterStock);
        log.setOrderId(orderId);
        log.setRemark(remark);
        
        stockLogMapper.insert(log);
    }

    /**
     * 获取库存缓存键
     */
    private String getStockCacheKey(Long skuId) {
        return ProductConstants.Cache.STOCK_PREFIX + skuId;
    }

    /**
     * 缓存库存信息
     */
    private void cacheStockInfo(Long skuId, ProductStock stock) {
        String cacheKey = getStockCacheKey(skuId);
        StockDTO stockDTO = BeanUtil.copyProperties(stock, StockDTO.class);
        cacheManager.set(
                cacheKey,
                JSONUtil.toJsonStr(stockDTO),
                ProductConstants.Cache.STOCK_CACHE_EXPIRE
        );
    }

    /**
     * 清除库存缓存
     */
    private void clearStockCache(Long skuId) {
        cacheManager.delete(getStockCacheKey(skuId));
        log.debug("清除库存缓存 - skuId: {}", skuId);
    }

    /**
     * 发送库存变更消息
     */
    private void sendStockChangeMessage(Long skuId, Integer operationType, Integer quantity,
                                       Integer beforeStock, Integer afterStock,
                                       Long orderId, String remark) {
        StockChangeMessage message = new StockChangeMessage(
                skuId, operationType, quantity, beforeStock, afterStock, 
                orderId, remark, null
        );
        stockChangeProducer.sendStockChangeMessage(message);
    }

}
