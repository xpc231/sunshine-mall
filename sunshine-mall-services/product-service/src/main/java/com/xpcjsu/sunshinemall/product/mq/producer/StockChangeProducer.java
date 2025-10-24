package com.xpcjsu.sunshinemall.product.mq.producer;

import com.xpcjsu.sunshinemall.product.constant.ProductConstants;
import com.xpcjsu.sunshinemall.product.mq.message.StockChangeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 库存变更消息生产者
 *
 * @author xpcjsu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockChangeProducer {

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 发送库存变更消息
     *
     * @param message 库存变更消息
     */
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
