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
     * 若库存已存在，则抛出业务异常；否则插入新的库存记录，并同步到消息队列中。
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
        validateParams(skuId, quantity);

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

        // 插入库存记录
        int inserted = productStockMapper.insert(stock);
        
        if (inserted > 0) {
            // 记录操作日志和发送消息
            recordStockLog(skuId, ProductConstants.StockOperationType.IN_STOCK, 
                         quantity, 0, quantity, null, "初始化库存");
            sendStockChangeMessage(skuId, ProductConstants.StockOperationType.IN_STOCK, 
                                 quantity, 0, quantity, null, "初始化库存");
            log.info("初始化库存成功 - skuId: {}, quantity: {}", skuId, quantity);
        }
        
        return inserted > 0;
    }


    /**
     * 增加指定SKU的库存数量
     *
     * @param skuId 商品SKU ID，不能为空
     * @param quantity 增加的库存数量，必须大于0
     * @param remark 操作备注信息
     * @return 操作成功返回true
     * @throws SystemException 当增加库存失败时抛出异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addStock(Long skuId, Integer quantity, String remark) {
        // 验证输入参数的有效性
        validateParams(skuId, quantity);

        // 获取当前库存信息并记录操作前的库存数量
        ProductStock stock = getStock(skuId);
        int beforeStock = stock.getAvailableStock();

        // 执行库存增加操作
        int updated = productStockMapper.addStock(skuId, quantity);
        if (updated == 0) {
            throw new SystemException("ADD_STOCK_FAILED", "增加库存失败");
        }

        // 库存变更后的后续处理，包括记录操作日志等
        afterStockChange(skuId, ProductConstants.StockOperationType.IN_STOCK,
                        quantity, beforeStock, beforeStock + quantity, null, remark);

        log.info("增加库存成功 - skuId: {}, quantity: {}", skuId, quantity);
        return true;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    @Idempotent(key = "'stock:deduct:' + #orderId + ':' + #skuId", expireTime = 300)
    public boolean deductStock(Long skuId, Integer quantity, Long orderId, String remark) {
        validateParams(skuId, quantity);

        // Redis预扣减（可选，高并发场景开启）
        boolean redisPreDeduct = tryRedisPreDeduct(skuId, quantity);

        // 乐观锁重试执行扣减
        return retryWithOptimisticLock(skuId, quantity, orderId, remark, redisPreDeduct,
            (stock, beforeStock) -> {
                int updated = productStockMapper.deductStock(skuId, quantity, stock.getVersion());
                if (updated > 0) {
                    afterStockChange(skuId, ProductConstants.StockOperationType.DEDUCT,
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
                    afterStockChange(skuId, ProductConstants.StockOperationType.LOCK,
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
                    afterStockChange(skuId, ProductConstants.StockOperationType.UNLOCK,
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
            afterStockChange(skuId, ProductConstants.StockOperationType.DEDUCT,
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
            afterStockChange(skuId, ProductConstants.StockOperationType.RETURN,
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
     * 参数校验
     */
    private void validateParams(Long skuId, Integer quantity) {
        if (skuId == null) {
            throw new ValidationException("SKU_ID_REQUIRED", "SKU ID不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new ValidationException("QUANTITY_INVALID", "数量必须大于0");
        }
    }

    /**
     * Redis预扣减（高并发优化）
     */
    private boolean tryRedisPreDeduct(Long skuId, Integer quantity) {
        String cacheKey = getStockCacheKey(skuId);
        try {
            Long remaining = cacheManager.decrement(cacheKey, quantity.longValue());
            if (remaining < 0) {
                cacheManager.increment(cacheKey, quantity.longValue());
                throw new BusinessException("STOCK_INSUFFICIENT", "库存不足");
            }
            return true;
        } catch (Exception e) {
            log.warn("Redis预扣减失败,降级到数据库 - skuId: {}", skuId, e);
            return false;
        }
    }

    /**
     * 使用乐观锁机制重试库存操作
     *
     * @param skuId 商品SKU ID
     * @param quantity 操作数量
     * @param orderId 订单ID
     * @param remark 备注信息
     * @param redisPreDeducted 是否已预扣减Redis库存
     * @param operation 库存操作接口实现
     * @return 操作成功返回true，失败则抛出异常
     * @throws BusinessException 库存不足时抛出
     * @throws SystemException 操作失败超过重试次数时抛出
     */
    private boolean retryWithOptimisticLock(Long skuId, Integer quantity, Long orderId,
                                           String remark, boolean redisPreDeducted,
                                           StockOperation operation) {
        // 循环重试库存操作，直到成功或超过最大重试次数
        for (int i = 0; i < MAX_RETRY_TIMES; i++) {
            ProductStock stock = getStock(skuId);
            if (stock.getAvailableStock() < quantity) {
                if (redisPreDeducted) rollbackRedis(skuId, quantity);
                throw new BusinessException("STOCK_INSUFFICIENT", "库存不足");
            }

            // 执行具体的库存操作，如果成功则返回true
            if (operation.execute(stock, stock.getAvailableStock())) {
                return true;
            }
            log.warn("乐观锁冲突,重试 - skuId: {}, retry: {}", skuId, i + 1);
        }

        // 超过重试次数后回滚Redis预扣减并抛出系统异常
        if (redisPreDeducted) rollbackRedis(skuId, quantity);
        throw new SystemException("STOCK_OPERATION_FAILED", "库存操作失败,请重试");
    }


    /**
     * 回滚Redis库存
     */
    private void rollbackRedis(Long skuId, Integer quantity) {
        try {
            cacheManager.increment(getStockCacheKey(skuId), quantity.longValue());
        } catch (Exception e) {
            log.error("回滚Redis库存失败 - skuId: {}", skuId, e);
        }
    }

    /**
     * 库存变更后的统一处理
     */
    private void afterStockChange(Long skuId, Integer operationType, Integer quantity,
                                 Integer beforeStock, Integer afterStock,
                                 Long orderId, String remark) {
        clearStockCache(skuId);
        recordStockLog(skuId, operationType, quantity, beforeStock, afterStock, orderId, remark);
        sendStockChangeMessage(skuId, operationType, quantity, beforeStock, afterStock, orderId, remark);
    }

    /**
     * 库存操作函数式接口
     */
    @FunctionalInterface
    private interface StockOperation {
        boolean execute(ProductStock stock, int beforeStock);
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
