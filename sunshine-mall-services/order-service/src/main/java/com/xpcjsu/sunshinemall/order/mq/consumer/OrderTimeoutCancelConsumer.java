package com.xpcjsu.sunshinemall.order.mq.consumer;

// RocketMQ已禁用，改用OpenFeign远程调用
// 订单超时取消功能可改用定时任务实现，扫描待支付订单并自动取消
/*
import com.xpcjsu.sunshinemall.order.dto.constant.OrderConstants;
import com.xpcjsu.sunshinemall.framework.common.mq.MqConstant;
import com.xpcjsu.sunshinemall.order.mq.message.OrderEventMessage;
import com.xpcjsu.sunshinemall.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

*//**
 * 订单超时取消消费者：监听延迟消息，在到期未支付时自动取消订单
 * 
 * 注意：RocketMQ已禁用，可改用定时任务实现订单超时自动取消
 *//*
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MqConstant.Order.TOPIC_DELAY,
        consumerGroup = OrderConstants.MQ.CONSUMER_GROUP,
        selectorExpression = MqConstant.Order.Tag.TIMEOUT_CANCELLED
)
public class OrderTimeoutCancelConsumer implements RocketMQListener<OrderEventMessage> {

    private final OrderService orderService;

    @Override
    public void onMessage(OrderEventMessage msg) {
        if (msg == null) {
            log.warn("接收到空的延迟取消消息，忽略");
            return;
        }
        Long userId = msg.getUserId();
        String orderNo = msg.getOrderNo();
        try {
            // 幂等：仅对待支付订单执行取消，内部已校验
            boolean ok = orderService.cancelOrder(userId, orderNo);
            log.info("延迟取消执行完成，orderNo={}, result={}", orderNo, ok);
        } catch (Exception e) {
            // 业务异常或已支付/已取消等情况，记录日志，避免抛出导致重试风暴
            log.warn("延迟取消执行失败或不需要处理，orderNo={}, msg={}", orderNo, e.getMessage());
        }
    }
}
*/