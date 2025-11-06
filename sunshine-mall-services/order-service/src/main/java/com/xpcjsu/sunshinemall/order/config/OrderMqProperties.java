package com.xpcjsu.sunshinemall.order.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 订单服务MQ相关的轻量配置
 * 说明：仅抽取延迟取消的delayLevel，默认30分钟（RocketMQ内置级别9）
 */
@Data
@Component
@ConfigurationProperties(prefix = "order.mq")
public class OrderMqProperties {

    /** 延迟取消的延迟级别（RocketMQ内置级别，默认9≈30分钟） */
    private int delayCancelLevel = 9;
}