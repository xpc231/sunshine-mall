package com.xpcjsu.sunshinemall.product.config;

// RocketMQ已禁用，改用OpenFeign远程调用
// import lombok.extern.slf4j.Slf4j;
// import org.apache.rocketmq.spring.core.RocketMQTemplate;
// import org.apache.rocketmq.spring.support.RocketMQMessageConverter;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ配置类
 * 
 * 手动创建RocketMQTemplate Bean，解决RocketMQ 5.x自动配置问题。
 * 
 * 注意：RocketMQ已禁用，改用OpenFeign远程调用
 * 
 * @author xpcjsu
 * @since 1.0.0
 */
/*
@Slf4j
@Configuration
public class RocketMQConfig {

    @Value("${rocketmq.name-server}")
    private String nameServer;

    @Value("${rocketmq.producer.group}")
    private String producerGroup;

    public RocketMQConfig() {
        log.info("RocketMQ配置类加载完成");
    }

    *//**
     * 创建RocketMQTemplate Bean
     * 
     * @return RocketMQTemplate实例
     *//*
    @Bean
    @ConditionalOnMissingBean
    public RocketMQTemplate rocketMQTemplate() {
        RocketMQTemplate template = new RocketMQTemplate();
        template.setProducer(createDefaultProducer());
        template.setMessageConverter(new RocketMQMessageConverter().getMessageConverter());
        
        log.info("RocketMQTemplate创建成功 - nameServer: {}, producerGroup: {}", nameServer, producerGroup);
        return template;
    }

    *//**
     * 创建默认生产者
     * 注意：不要在这里启动producer，RocketMQTemplate会自动启动
     *//*
    private org.apache.rocketmq.client.producer.DefaultMQProducer createDefaultProducer() {
        org.apache.rocketmq.client.producer.DefaultMQProducer producer = 
            new org.apache.rocketmq.client.producer.DefaultMQProducer(producerGroup);
        producer.setNamesrvAddr(nameServer);
        producer.setSendMsgTimeout(3000);
        producer.setRetryTimesWhenSendFailed(2);
        
        log.info("RocketMQ生产者配置完成");
        return producer;
    }
}
*/
