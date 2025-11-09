package com.xpcjsu.sunshinemall.order.service;

import com.xpcjsu.sunshinemall.order.dto.entity.SeckillOrder;

/**
 * 秒杀订单服务接口
 *
 * @author xpcjsu
 */
public interface SeckillOrderService {

    /**
     * 创建秒杀订单记录（防重复购买）
     *
     * @param seckillOrder 秒杀订单记录
     * @return 秒杀订单记录ID
     */
    Long createSeckillOrder(SeckillOrder seckillOrder);

    /**
     * 根据用户ID和秒杀商品ID查询秒杀订单记录
     *
     * @param userId 用户ID
     * @param seckillProductId 秒杀商品ID
     * @return 秒杀订单记录
     */
    SeckillOrder getSeckillOrderByUserAndProduct(Long userId, Long seckillProductId);

    /**
     * 更新秒杀订单记录（关联订单信息）
     *
     * @param seckillOrderId 秒杀订单记录ID
     * @param orderId 订单ID
     * @param orderNo 订单编号
     * @return 是否成功
     */
    boolean updateSeckillOrderWithOrderInfo(Long seckillOrderId, Long orderId, String orderNo);

    /**
     * 更新秒杀订单状态
     *
     * @param seckillOrderId 秒杀订单记录ID
     * @param status 状态（0-下单中，1-已下单，2-已取消）
     * @return 是否成功
     */
    boolean updateSeckillOrderStatus(Long seckillOrderId, Integer status);

    /**
     * 根据订单ID查询秒杀订单记录
     *
     * @param orderId 订单ID
     * @return 秒杀订单记录
     */
    SeckillOrder getSeckillOrderByOrderId(Long orderId);

}

