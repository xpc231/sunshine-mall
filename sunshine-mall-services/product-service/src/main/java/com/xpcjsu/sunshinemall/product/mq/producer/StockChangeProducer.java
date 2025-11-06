package com.xpcjsu.sunshinemall.product.mq.producer;

// RocketMQ已禁用，改用OpenFeign远程调用
// 如需通知其他服务库存变更，请使用Feign客户端进行同步调用
/*
import com.xpcjsu.sunshinemall.product.constant.ProductConstants;
import com.xpcjsu.sunshinemall.product.mq.message.StockChangeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

*//**
 * 库存变更消息生产者
 * 
 * 注意：RocketMQ已禁用，如需通知其他服务请使用Feign客户端
 *
 * @author xpcjsu
 *//*
@Slf4j
@Component
@RequiredArgsConstructor
public class StockChangeProducer {

    private final RocketMQTemplate rocketMQTemplate;

    *//**
     * 发送库存变更消息
     *
     * @param message 库存变更消息
     *//*
    public void sendStockChangeMessage(StockChangeMessage message) {
        try {
            message.setTimestamp(System.currentTimeMillis());
            
            rocketMQTemplate.syncSend(
                    ProductConstants.MQ.STOCK_CHANGE_TOPIC,
                    MessageBuilder.withPayload(message).build()
            );
            
            log.info("发送库存变更消息成功 - skuId: {}, operationType: {}, quantity: {}", 
                    message.getSkuId(), message.getOperationType(), message.getQuantity());
        } catch (Exception e) {
            log.error("发送库存变更消息失败 - skuId: {}", message.getSkuId(), e);
            // 消息发送失败不影响主流程
        }
    }

}
*/
