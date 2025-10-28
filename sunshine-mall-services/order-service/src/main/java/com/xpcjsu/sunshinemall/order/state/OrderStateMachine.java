package com.xpcjsu.sunshinemall.order.state;

import com.xpcjsu.sunshinemall.order.entity.OrderInfo;
import com.xpcjsu.sunshinemall.order.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 订单状态机，负责订单状态的流转与校验
 */
@Slf4j
@Component
@Schema(name = "OrderStateMachine", description = "订单状态机，包含状态流转逻辑与校验规则")
public class OrderStateMachine {

    /**
     * 按订单维度的细粒度锁控制，确保并发场景下的状态流转线程安全
     */
    private final Map<Long, ReentrantLock> orderLocks = new ConcurrentHashMap<>();

    /**
     * 状态流转白名单,
     * 静态常量映射表，键为当前订单状态，值为允许转换到的状态集合,
     * 提高了枚举类型键的存储效率
     */
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(OrderStatus.class);

    //固定的业务逻辑,一次性建立
    static {
        ALLOWED_TRANSITIONS.put(OrderStatus.WAIT_PAY, EnumSet.of(
                OrderStatus.WAIT_SHIP, OrderStatus.CANCELLED, OrderStatus.CLOSED
        ));
        ALLOWED_TRANSITIONS.put(OrderStatus.WAIT_SHIP, EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CLOSED));
        ALLOWED_TRANSITIONS.put(OrderStatus.SHIPPED, EnumSet.of(OrderStatus.COMPLETED));
        ALLOWED_TRANSITIONS.put(OrderStatus.COMPLETED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED_TRANSITIONS.put(OrderStatus.CLOSED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED_TRANSITIONS.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
    }

    /**
     * 执行状态流转
     * @param order 订单
     * @param target 目标状态
     * @param ctx 业务校验上下文
     */
    public void transition(OrderInfo order, OrderStatus target, TransitionContext ctx) {
        if (order == null || target == null) {
            throw new IllegalArgumentException("order 或 target 不能为空");
        }
        OrderStatus current = OrderStatus.fromCode(order.getStatus());
        if (current == null) {
            throw new IllegalStateException("订单当前状态非法: " + order.getStatus());
        }
        if (current == target) {
            log.info("订单[{}]状态已是目标态[{}]，忽略流转", order.getOrderNo(), target);
            return;
        }
        // 检查白名单
        Set<OrderStatus> nextSet = ALLOWED_TRANSITIONS.getOrDefault(current, EnumSet.noneOf(OrderStatus.class));
        if (!nextSet.contains(target)) {
            throw new IllegalStateException(String.format("订单[%s]状态从[%s]到[%s]不允许", order.getOrderNo(), current, target));
        }

        // 加锁，保障线程安全，锁范围直到锁释放
        // 获取与当前订单关联的锁，属于细粒度锁控制
        ReentrantLock lock = orderLocks.computeIfAbsent(order.getId(), k -> new ReentrantLock());
        lock.lock();

        try {
            // 二次确认，防止并发下状态已变
            OrderStatus recheck = OrderStatus.fromCode(order.getStatus());
            if (recheck != current) {
                log.warn("订单[{}]状态在加锁前已变化: {} -> {}，本次流转取消", order.getOrderNo(), current, recheck);
                return;
            }
            // 业务校验
            validate(current, target, order, ctx);
            // 执行流转
            order.setStatus(target.getCode());
            log.info("订单[{}]状态流转成功: {} -> {}", order.getOrderNo(), current, target);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 针对不同的流转进行业务校验
     */
    private void validate(OrderStatus current, OrderStatus target, OrderInfo order, TransitionContext ctx) {
        switch (current) {
            case WAIT_PAY -> {
                if (target == OrderStatus.WAIT_SHIP) {
                    if (ctx == null || !ctx.isPaymentVerified()) {
                        throw new IllegalStateException("支付未完成，不能进入待发货状态");
                    }
                }
                if (target == OrderStatus.CANCELLED) {
                    if (ctx == null || !ctx.isCancelApproved()) {
                        throw new IllegalStateException("取消申请未通过，不能取消订单");
                    }
                }
                if (target == OrderStatus.CLOSED) {
                    if (ctx == null || !ctx.isAdminCloseRequired()) {
                        throw new IllegalStateException("未满足关闭条件，不能关闭订单");
                    }
                }
            }
            case WAIT_SHIP -> {
                if (target == OrderStatus.SHIPPED) {
                    if (ctx == null || !ctx.isShippingPrepared()) {
                        throw new IllegalStateException("发货信息不完整或库存未扣减，不能标记已发货");
                    }
                }
                if (target == OrderStatus.CLOSED) {
                    if (ctx == null || !ctx.isAdminCloseRequired()) {
                        throw new IllegalStateException("未满足关闭条件，不能关闭订单");
                    }
                }
            }
            case SHIPPED -> {
                if (target == OrderStatus.COMPLETED) {
                    if (ctx == null || !ctx.isReceiveConfirmed()) {
                        throw new IllegalStateException("未确认收货，不能完成订单");
                    }
                }
            }
            case COMPLETED, CLOSED, CANCELLED -> {
                throw new IllegalStateException("终态订单不允许再流转");
            }
            default -> {
                throw new IllegalStateException("未知订单状态: " + current);
            }
        }
    }
}