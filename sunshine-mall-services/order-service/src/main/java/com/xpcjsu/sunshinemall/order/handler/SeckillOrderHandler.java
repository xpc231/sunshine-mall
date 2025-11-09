package com.xpcjsu.sunshinemall.order.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpcjsu.sunshinemall.framework.base.exception.BusinessException;
import com.xpcjsu.sunshinemall.framework.common.feign.clients.SeckillProductClient;
import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import com.xpcjsu.sunshinemall.order.dto.common.SkuQuantityRequest;
import com.xpcjsu.sunshinemall.order.dto.entity.SeckillOrder;
import com.xpcjsu.sunshinemall.order.service.SeckillOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 秒杀订单处理器
 * <p>
 * 专门处理秒杀订单相关的业务逻辑，包括：
 * - 秒杀商品校验
 * - 防重复购买
 * - 秒杀库存扣减
 * - 秒杀价格处理
 *
 * @author xpcjsu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillOrderHandler {

    private final SeckillProductClient seckillProductClient;
    private final SeckillOrderService seckillOrderService;
    private final ObjectMapper objectMapper;

    /**
     * 处理秒杀订单前置校验和记录创建
     *
     * @param userId 用户ID
     * @param itemsReq 订单项列表
     * @return 秒杀商品信息Map，包含seckillProductId、seckillPrice、seckillOrderRecordId等
     */
    public Map<String, Object> handleSeckillOrderPreCheck(Long userId, List<SkuQuantityRequest> itemsReq) {
        // 秒杀订单只能购买一个商品
        if (itemsReq.size() != 1) {
            throw new BusinessException("SECKILL_ORDER_SINGLE_ITEM", "秒杀订单只能购买一个商品");
        }

        SkuQuantityRequest item = itemsReq.get(0);
        Long skuId = item.getSkuId();
        Integer quantity = item.getQuantity();

        // 1. 调用秒杀商品服务，获取秒杀商品信息
        Result<Map<String, Object>> seckillRes = seckillProductClient.getSeckillProductBySkuId(skuId);
        if (seckillRes == null || seckillRes.isFailure() || seckillRes.getData() == null) {
            throw new BusinessException("SECKILL_PRODUCT_NOT_FOUND", "该商品不是秒杀商品或秒杀商品不存在");
        }

        Map<String, Object> seckillProductMap = seckillRes.getData();
        Long seckillProductId = ((Number) seckillProductMap.get("id")).longValue();
        Integer status = ((Number) seckillProductMap.get("status")).intValue();
        Integer seckillStock = ((Number) seckillProductMap.get("seckillStock")).intValue();

        // 解析LocalDateTime（Feign返回的可能是字符串或已序列化的对象）
        LocalDateTime startTime;
        LocalDateTime endTime;
        try {
            Object startTimeObj = seckillProductMap.get("startTime");
            Object endTimeObj = seckillProductMap.get("endTime");
            if (startTimeObj instanceof String) {
                startTime = LocalDateTime.parse((String) startTimeObj);
            } else if (startTimeObj instanceof LocalDateTime) {
                startTime = (LocalDateTime) startTimeObj;
            } else {
                // 使用ObjectMapper转换
                startTime = objectMapper.convertValue(startTimeObj, LocalDateTime.class);
            }
            if (endTimeObj instanceof String) {
                endTime = LocalDateTime.parse((String) endTimeObj);
            } else if (endTimeObj instanceof LocalDateTime) {
                endTime = (LocalDateTime) endTimeObj;
            } else {
                endTime = objectMapper.convertValue(endTimeObj, LocalDateTime.class);
            }
        } catch (Exception e) {
            throw new BusinessException("SECKILL_TIME_PARSE_ERROR", "秒杀时间解析失败", e);
        }

        Integer limitQuantity = seckillProductMap.get("limitQuantity") != null ?
                ((Number) seckillProductMap.get("limitQuantity")).intValue() : 1;

        // 2. 校验秒杀状态
        if (status != 1) {
            throw new BusinessException("SECKILL_NOT_IN_PROGRESS", "秒杀活动未开始或已结束");
        }

        // 3. 校验秒杀时间
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            throw new BusinessException("SECKILL_NOT_IN_TIME", "不在秒杀活动时间内");
        }

        // 4. 校验秒杀库存
        if (seckillStock < quantity) {
            throw new BusinessException("SECKILL_STOCK_INSUFFICIENT", "秒杀库存不足");
        }

        // 5. 校验限购数量
        if (quantity > limitQuantity) {
            throw new BusinessException("SECKILL_LIMIT_EXCEEDED", "超过限购数量，最多只能购买" + limitQuantity + "件");
        }

        // 6. 防重复购买：检查用户是否已购买过该秒杀商品
        SeckillOrder existing = seckillOrderService.getSeckillOrderByUserAndProduct(userId, seckillProductId);
        if (existing != null && (existing.getStatus() == 0 || existing.getStatus() == 1)) {
            throw new BusinessException("SECKILL_ALREADY_PURCHASED", "您已购买过该秒杀商品，不能重复购买");
        }

        // 7. 创建秒杀订单记录（防重复购买）
        SeckillOrder seckillOrder = new SeckillOrder();
        seckillOrder.setUserId(userId);
        seckillOrder.setSeckillProductId(seckillProductId);
        seckillOrder.setSkuId(skuId);
        seckillOrder.setQuantity(quantity);
        seckillOrder.setSeckillPrice(new BigDecimal(seckillProductMap.get("seckillPrice").toString()));
        seckillOrder.setStatus(0); // 0-下单中

        Long seckillOrderRecordId = seckillOrderService.createSeckillOrder(seckillOrder);

        // 8. 返回秒杀商品信息
        Map<String, Object> result = new HashMap<>();
        result.put("seckillProductId", seckillProductId);
        result.put("seckillPrice", seckillProductMap.get("seckillPrice"));
        result.put("seckillOrderRecordId", seckillOrderRecordId);
        return result;
    }

    /**
     * 扣减秒杀库存
     *
     * @param seckillProductInfo 秒杀商品信息
     * @param quantity 扣减数量
     * @return 是否成功
     */
    public boolean deductSeckillStock(Map<String, Object> seckillProductInfo, Integer quantity) {
        Long seckillProductId = (Long) seckillProductInfo.get("seckillProductId");
        Result<Void> deductRes = seckillProductClient.deductSeckillStock(seckillProductId, quantity);
        if (deductRes == null || deductRes.isFailure()) {
            log.warn("秒杀库存扣减失败 - seckillProductId: {}, quantity: {}", seckillProductId, quantity);
            throw new BusinessException("SECKILL_STOCK_DEDUCT_FAILED", "秒杀库存扣减失败");
        }
        log.info("秒杀库存扣减成功 - seckillProductId: {}, quantity: {}", seckillProductId, quantity);
        return true;
    }

    /**
     * 获取秒杀价格
     *
     * @param seckillProductInfo 秒杀商品信息
     * @return 秒杀价格
     */
    public BigDecimal getSeckillPrice(Map<String, Object> seckillProductInfo) {
        if (seckillProductInfo == null || seckillProductInfo.get("seckillPrice") == null) {
            return null;
        }
        Object priceObj = seckillProductInfo.get("seckillPrice");
        if (priceObj instanceof BigDecimal) {
            return (BigDecimal) priceObj;
        } else if (priceObj instanceof Number) {
            return BigDecimal.valueOf(((Number) priceObj).doubleValue());
        } else {
            return new BigDecimal(priceObj.toString());
        }
    }

    /**
     * 更新秒杀订单记录关联订单信息
     *
     * @param seckillProductInfo 秒杀商品信息
     * @param orderId 订单ID
     * @param orderNo 订单编号
     */
    public void updateSeckillOrderRecord(Map<String, Object> seckillProductInfo, Long orderId, String orderNo) {
        Long seckillOrderRecordId = (Long) seckillProductInfo.get("seckillOrderRecordId");
        if (seckillOrderRecordId != null) {
            seckillOrderService.updateSeckillOrderWithOrderInfo(seckillOrderRecordId, orderId, orderNo);
        }
    }

    /**
     * 回滚秒杀库存（订单创建失败时调用）
     *
     * @param seckillProductInfo 秒杀商品信息
     * @param quantity 回滚数量
     */
    public void rollbackSeckillStock(Map<String, Object> seckillProductInfo, Integer quantity) {
        Long seckillProductId = (Long) seckillProductInfo.get("seckillProductId");
        if (seckillProductId == null) {
            log.warn("秒杀商品ID为空，无法回滚库存");
            return;
        }

        try {
            // 调用秒杀商品服务回滚库存
            Result<Void> rollbackRes = seckillProductClient.rollbackSeckillStock(seckillProductId, quantity);
            if (rollbackRes == null || rollbackRes.isFailure()) {
                log.warn("回滚秒杀库存失败 - seckillProductId: {}, quantity: {}", seckillProductId, quantity);
            } else {
                log.info("回滚秒杀库存成功 - seckillProductId: {}, quantity: {}", seckillProductId, quantity);
            }
        } catch (Exception e) {
            log.error("回滚秒杀库存异常 - seckillProductId: {}, quantity: {}", seckillProductId, quantity, e);
        }
    }

}

