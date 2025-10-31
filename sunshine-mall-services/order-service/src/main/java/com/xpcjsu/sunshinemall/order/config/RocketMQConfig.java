package com.xpcjsu.sunshinemall.order.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ配置（order-service）
 * 手动创建RocketMQTemplate，适配RocketMQ 5.x自动配置差异。
 */
@Slf4j
@Configuration
public class RocketMQConfig {

    @Value("${rocketmq.name-server}")
    private String nameServer;

    @Value("${rocketmq.producer.group}")
    private String producerGroup;

    /**
     * 创建RocketMQTemplate Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public RocketMQTemplate rocketMQTemplate() {
        RocketMQTemplate template = new RocketMQTemplate();
        template.setProducer(createDefaultProducer());
        template.setMessageConverter(new RocketMQMessageConverter().getMessageConverter());
        log.info("[order-service] RocketMQTemplate 初始化完成 - nameServer: {}, producerGroup: {}", nameServer, producerGroup);
        return template;
    }

    /**
     * 创建默认生产者，不要主动启动，交由RocketMQTemplate管理
     */
    private org.apache.rocketmq.client.producer.DefaultMQProducer createDefaultProducer() {
        org.apache.rocketmq.client.producer.DefaultMQProducer producer =
                new org.apache.rocketmq.client.producer.DefaultMQProducer(producerGroup);
        producer.setNamesrvAddr(nameServer);
        producer.setSendMsgTimeout(3000);
        producer.setRetryTimesWhenSendFailed(2);
        log.info("[order-service] RocketMQ Producer 配置完成");
        return producer;
    }
}