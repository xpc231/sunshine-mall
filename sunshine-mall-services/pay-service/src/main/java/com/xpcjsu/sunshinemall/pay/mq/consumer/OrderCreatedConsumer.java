package com.xpcjsu.sunshinemall.pay.mq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpcjsu.sunshinemall.framework.common.mq.MqConstant;
import com.xpcjsu.sunshinemall.framework.common.mq.message.OrderEventMessage;
import com.xpcjsu.sunshinemall.pay.entity.PayNotifyLog;
import com.xpcjsu.sunshinemall.pay.mapper.PayNotifyLogMapper;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mq", name = "enabled", havingValue = "true")
public class OrderCreatedConsumer implements DisposableBean {

    private final ObjectMapper objectMapper;
    private final PayNotifyLogMapper payNotifyLogMapper;

    @Value("${rocketmq.name-server}")
    private String nameServer;

    @Value("${pay.mq.consumer-group:pay-service-consumer-group}")
    private String consumerGroup;

    private DefaultMQPushConsumer consumer;

    @jakarta.annotation.PostConstruct
    public void start() throws Exception {
        DefaultMQPushConsumer c = new DefaultMQPushConsumer(consumerGroup);
        c.setNamesrvAddr(nameServer);
        c.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
        c.subscribe(MqConstant.Order.TOPIC_EVENT, MqConstant.Order.Tag.CREATED);
        c.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                                                            ConsumeConcurrentlyContext context) {
                for (MessageExt m : msgs) {
                    try {
                        String body = new String(m.getBody(), StandardCharsets.UTF_8);
                        OrderEventMessage msg = objectMapper.readValue(body, OrderEventMessage.class);
                        PayNotifyLog logEntity = PayNotifyLog.builder()
                                .refNo(msg.getOrderNo())
                                .notifyType(0)
                                .payload(body)
                                .signVerified(1)
                                .handleStatus(1)
                                .handleMessage("RECEIVED")
                                .createTime(LocalDateTime.now())
                                .build();
                        payNotifyLogMapper.insert(logEntity);
                    } catch (Exception ignored) {
                    }
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        c.start();
        this.consumer = c;
    }

    @Override
    public void destroy() {
        if (consumer != null) {
            consumer.shutdown();
        }
    }
}