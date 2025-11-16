package com.xpcjsu.sunshinemall.framework.common.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MQ自动配置类，用于创建和配置RocketMQ客户端实例
 */
@Configuration
@ConditionalOnProperty(prefix = "mq", name = "enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class MqAutoConfiguration {

    /**
     * JSON对象映射器，用于Java对象与JSON格式数据之间的序列化和反序列化操作
     * ObjectMapper是Jackson库的核心组件，提供了丰富的JSON处理功能
     */
    private final ObjectMapper objectMapper;


    /**
     * 创建并初始化RocketMQ客户端Bean
     *
     * @param nameServer RocketMQ名称服务器地址
     * @param producerGroup 生产者组名称
     * @param timeoutMs 消息发送超时时间，单位毫秒，默认3000ms
     * @param retryTimes 发送失败时的重试次数，默认2次
     * @param enabled MQ功能是否启用，默认true
     * @return 初始化完成的RocketMqClient实例
     */
    @Bean
    public RocketMqClient rocketMqClient(
            @Value("${rocketmq.name-server}") String nameServer,
            @Value("${rocketmq.producer.group}") String producerGroup,
            @Value("${rocketmq.producer.send-message-timeout:3000}") int timeoutMs,
            @Value("${rocketmq.producer.retry-times-when-send-failed:2}") int retryTimes,
            @Value("${mq.enabled:false}") boolean enabled
    ) {
        // 创建RocketMQ客户端实例并进行初始化
        RocketMqClient client = new RocketMqClient(objectMapper, nameServer, producerGroup, timeoutMs, retryTimes, enabled);
        client.init();
        return client;
    }
}


