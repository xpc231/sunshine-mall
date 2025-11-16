package com.xpcjsu.sunshinemall.stock.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xpcjsu.sunshinemall.framework.base.exception.BusinessException;
import com.xpcjsu.sunshinemall.framework.base.exception.SystemException;
import com.xpcjsu.sunshinemall.framework.base.exception.ValidationException;
import com.xpcjsu.sunshinemall.framework.cache.core.CacheManager;
import com.xpcjsu.sunshinemall.framework.common.mq.MqClient;
import com.xpcjsu.sunshinemall.framework.distributedid.core.SnowflakeIdGenerator;
import com.xpcjsu.sunshinemall.framework.idempotent.annotation.Idempotent;
import com.xpcjsu.sunshinemall.stock.constant.StockConstants;
import com.xpcjsu.sunshinemall.stock.dto.StockDTO;
import com.xpcjsu.sunshinemall.stock.entity.ProductStock;
import com.xpcjsu.sunshinemall.stock.entity.StockLog;
import com.xpcjsu.sunshinemall.stock.mapper.ProductStockMapper;
import com.xpcjsu.sunshinemall.stock.mapper.StockLogMapper;
import com.xpcjsu.sunshinemall.stock.service.StockService;
import com.xpcjsu.sunshinemall.stock.mq.message.StockChangeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final ProductStockMapper productStockMapper;
    private final StockLogMapper stockLogMapper;
    private final CacheManager cacheManager;
    private final SnowflakeIdGenerator idGenerator;
    private final MqClient mqClient;

    private static final int MAX_RETRY_TIMES = 3;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean initStock(Long skuId, Integer quantity) {
        validateParams(skuId, quantity);

        ProductStock existingStock = productStockMapper.selectOne(
                new LambdaQueryWrapper<ProductStock>().eq(ProductStock::getSkuId, skuId)
        );
        if (existingStock != null) {
            throw new BusinessException("STOCK_ALREADY_INIT", "库存已初始化");
        }

        ProductStock stock = new ProductStock();
        stock.setSkuId(skuId);
        stock.setTotalStock(quantity);
        stock.setAvailableStock(quantity);
        stock.setLockedStock(0);
        stock.setVersion(0);

        int inserted = productStockMapper.insert(stock);
        if (inserted > 0) {
            recordStockLog(skuId, StockConstants.StockOperationType.IN_STOCK,
                    quantity, 0, quantity, null, "初始化库存");
            log.info("初始化库存成功 - skuId: {}, quantity: {}", skuId, quantity);
        }
        return inserted > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addStock(Long skuId, Integer quantity, String remark) {
        validateParams(skuId, quantity);
        ProductStock stock = getStock(skuId);
        int beforeStock = stock.getAvailableStock();
        int updated = productStockMapper.addStock(skuId, quantity);
        if (updated == 0) {
            throw new SystemException("ADD_STOCK_FAILED", "增加库存失败");
        }
        afterStockChange(skuId, StockConstants.StockOperationType.IN_STOCK,
                quantity, beforeStock, beforeStock + quantity, null, remark);
        log.info("增加库存成功 - skuId: {}, quantity: {}", skuId, quantity);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Idempotent(key = "'stock:deduct:' + #orderId + ':' + #skuId", expireTime = 300)
    public boolean deductStock(Long skuId, Integer quantity, Long orderId, String remark) {
        validateParams(skuId, quantity);
        boolean redisPreDeduct = false;
        return retryWithOptimisticLock(skuId, quantity, orderId, remark, redisPreDeduct,
                (stock, beforeStock) -> {
                    int updated = productStockMapper.deductStock(skuId, quantity, stock.getVersion());
                    if (updated > 0) {
                        afterStockChange(skuId, StockConstants.StockOperationType.DEDUCT,
                                quantity, beforeStock, beforeStock - quantity, orderId, remark);
                        log.info("扣减库存成功 - skuId: {}, quantity: {}, orderId: {}", skuId, quantity, orderId);
                    }
                    return updated > 0;
                });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Idempotent(key = "'stock:lock:' + #orderId + ':' + #skuId", expireTime = 300)
    public boolean lockStock(Long skuId, Integer quantity, Long orderId, String remark) {
        validateParams(skuId, quantity);
        return retryWithOptimisticLock(skuId, quantity, orderId, remark, false,
                (stock, beforeStock) -> {
                    int updated = productStockMapper.lockStock(skuId, quantity, stock.getVersion());
                    if (updated > 0) {
                        afterStockChange(skuId, StockConstants.StockOperationType.LOCK,
                                quantity, beforeStock, beforeStock - quantity, orderId, remark);
                        log.info("预占库存成功 - skuId: {}, quantity: {}, orderId: {}", skuId, quantity, orderId);
                    }
                    return updated > 0;
                });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Idempotent(key = "'stock:unlock:' + #orderId + ':' + #skuId", expireTime = 300)
    public boolean unlockStock(Long skuId, Integer quantity, Long orderId, String remark) {
        validateParams(skuId, quantity);
        return retryWithOptimisticLock(skuId, quantity, orderId, remark, false,
                (stock, beforeStock) -> {
                    if (stock.getLockedStock() < quantity) {
                        throw new BusinessException("LOCKED_STOCK_INSUFFICIENT", "锁定库存不足");
                    }
                    int updated = productStockMapper.unlockStock(skuId, quantity, stock.getVersion());
                    if (updated > 0) {
                        afterStockChange(skuId, StockConstants.StockOperationType.UNLOCK,
                                quantity, beforeStock, beforeStock + quantity, orderId, remark);
                        log.info("释放库存成功 - skuId: {}, quantity: {}, orderId: {}", skuId, quantity, orderId);
                    }
                    return updated > 0;
                });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Idempotent(key = "'stock:confirm:' + #orderId + ':' + #skuId", expireTime = 300)
    public boolean confirmDeduct(Long skuId, Integer quantity, Long orderId, String remark) {
        validateParams(skuId, quantity);
        ProductStock stock = getStock(skuId);
        if (stock.getLockedStock() < quantity) {
            throw new BusinessException("LOCKED_STOCK_INSUFFICIENT", "锁定库存不足");
        }
        int updated = productStockMapper.update(null, new LambdaUpdateWrapper<ProductStock>()
                .setSql("locked_stock = locked_stock - " + quantity)
                .setSql("total_stock = total_stock - " + quantity)
                .setSql("version = version + 1")
                .eq(ProductStock::getSkuId, skuId)
                .eq(ProductStock::getVersion, stock.getVersion()));
        if (updated > 0) {
            afterStockChange(skuId, StockConstants.StockOperationType.DEDUCT,
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
        validateParams(skuId, quantity);
        ProductStock stock = getStock(skuId);
        int beforeStock = stock.getAvailableStock();
        int updated = productStockMapper.addStock(skuId, quantity);
        if (updated > 0) {
            afterStockChange(skuId, StockConstants.StockOperationType.RETURN,
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
        String cacheKey = getStockCacheKey(skuId);
        String cacheValue = cacheManager.get(cacheKey, String.class);
        if (StringUtils.hasText(cacheValue)) {
            return JSONUtil.toBean(cacheValue, StockDTO.class);
        }
        ProductStock stock = getStock(skuId);
        StockDTO stockDTO = BeanUtil.copyProperties(stock, StockDTO.class);
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

    private void validateParams(Long skuId, Integer quantity) {
        if (skuId == null) {
            throw new ValidationException("SKU_ID_REQUIRED", "SKU ID不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new ValidationException("QUANTITY_INVALID", "数量必须大于0");
        }
    }

    private boolean retryWithOptimisticLock(Long skuId, Integer quantity, Long orderId,
                                           String remark, boolean redisPreDeducted,
                                           StockOperation operation) {
        for (int i = 0; i < MAX_RETRY_TIMES; i++) {
            ProductStock stock = getStock(skuId);
            if (stock.getAvailableStock() < quantity) {
                throw new BusinessException("STOCK_INSUFFICIENT", "库存不足");
            }
            if (operation.execute(stock, stock.getAvailableStock())) {
                return true;
            }
            log.warn("乐观锁冲突,重试 - skuId: {}, retry: {}", skuId, i + 1);
        }
        throw new SystemException("STOCK_OPERATION_FAILED", "库存操作失败,请重试");
    }

    @FunctionalInterface
    private interface StockOperation {
        boolean execute(ProductStock stock, int beforeStock);
    }

    private ProductStock getStock(Long skuId) {
        ProductStock stock = productStockMapper.selectOne(
                new LambdaQueryWrapper<ProductStock>().eq(ProductStock::getSkuId, skuId)
        );
        if (stock == null) {
            throw new BusinessException("STOCK_NOT_FOUND", "库存不存在，请先初始化库存");
        }
        return stock;
    }

    private void recordStockLog(Long skuId, Integer operationType, Integer quantity,
                               Integer beforeStock, Integer afterStock,
                               Long orderId, String remark) {
        StockLog logEntity = new StockLog();
        logEntity.setId(idGenerator.nextId());
        logEntity.setSkuId(skuId);
        logEntity.setOperationType(operationType);
        logEntity.setQuantity(quantity);
        logEntity.setBeforeStock(beforeStock);
        logEntity.setAfterStock(afterStock);
        logEntity.setOrderId(orderId);
        logEntity.setRemark(remark);
        stockLogMapper.insert(logEntity);
    }

    private String getStockCacheKey(Long skuId) {
        return StockConstants.Cache.STOCK_PREFIX + skuId;
    }

    private void cacheStockInfo(Long skuId, ProductStock stock) {
        String cacheKey = getStockCacheKey(skuId);
        StockDTO stockDTO = BeanUtil.copyProperties(stock, StockDTO.class);
        cacheManager.set(
                cacheKey,
                JSONUtil.toJsonStr(stockDTO),
                StockConstants.Cache.STOCK_CACHE_EXPIRE
        );
    }

    private void afterStockChange(Long skuId, Integer operationType, Integer quantity,
                                  Integer beforeStock, Integer afterStock,
                                  Long orderId, String remark) {
        recordStockLog(skuId, operationType, quantity, beforeStock, afterStock, orderId, remark);
        StockChangeMessage msg = new StockChangeMessage(skuId, operationType, quantity,
                beforeStock, afterStock, orderId, remark, System.currentTimeMillis());
        mqClient.sendSync(StockConstants.MQ.STOCK_CHANGE_TOPIC, null, msg, String.valueOf(skuId), null);
    }
}
