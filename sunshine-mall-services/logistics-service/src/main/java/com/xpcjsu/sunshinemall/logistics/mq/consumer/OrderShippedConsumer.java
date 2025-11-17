package com.xpcjsu.sunshinemall.logistics.mq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpcjsu.sunshinemall.framework.common.mq.MqConstant;
import com.xpcjsu.sunshinemall.logistics.dto.AddEventRequest;
import com.xpcjsu.sunshinemall.logistics.dto.ShipmentDTO;
import com.xpcjsu.sunshinemall.logistics.service.LogisticsService;
import com.xpcjsu.sunshinemall.framework.common.mq.message.OrderEventMessage;
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
public class OrderShippedConsumer implements DisposableBean {

    private final ObjectMapper objectMapper;
    private final LogisticsService logisticsService;

    @Value("${rocketmq.name-server}")
    private String nameServer;

    @Value("${logistics.mq.consumer-group:logistics-service-consumer-group}")
    private String consumerGroup;

    private DefaultMQPushConsumer consumer;

    @jakarta.annotation.PostConstruct
    public void start() throws Exception {
        DefaultMQPushConsumer c = new DefaultMQPushConsumer(consumerGroup);
        c.setNamesrvAddr(nameServer);
        c.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
        c.subscribe(MqConstant.Order.TOPIC_EVENT, MqConstant.Order.Tag.SHIPPED);
        c.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                                                            ConsumeConcurrentlyContext context) {
                for (MessageExt m : msgs) {
                    try {
                        String body = new String(m.getBody(), StandardCharsets.UTF_8);
                        OrderEventMessage msg = objectMapper.readValue(body, OrderEventMessage.class);
                        ShipmentDTO shipment = logisticsService.getByOrderNo(msg.getOrderNo());
                        if (shipment != null) {
                            AddEventRequest req = new AddEventRequest();
                            req.setStatus("SHIPPED");
                            req.setEventTime(LocalDateTime.now().toString());
                            req.setLocation("仓库");
                            req.setMessage("订单已发货");
                            logisticsService.addEvent(shipment.getShipmentNo(), req);
                        }
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