package com.xpcjsu.sunshinemall.framework.common.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.DisposableBean;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * RocketMQ客户端实现类，封装了RocketMQ生产者的初始化、消息发送和资源销毁功能
 * 实现了MqClient接口和DisposableBean接口，支持多种消息发送模式
 */
@Slf4j
@RequiredArgsConstructor
public class RocketMqClient implements MqClient, DisposableBean {

    /**
     * JSON序列化工具，用于将对象转换为字节流
     */
    private final ObjectMapper objectMapper;

    /**
     * RocketMQ名称服务器地址
     */
    private final String nameServer;

    /**
     * 生产者组名称
     */
    private final String producerGroup;

    /**
     * 消息发送超时时间(毫秒)
     */
    private final int sendTimeoutMs;

    /**
     * 发送失败时的重试次数
     */
    private final int retryTimes;

    /**
     * MQ功能是否启用标识
     */
    private final boolean enabled;

    /**
     * RocketMQ生产者实例
     */
    private DefaultMQProducer producer;

    /**
     * 初始化RocketMQ生产者，设置相关参数并启动生产者
     * 如果MQ功能未启用则直接返回
     */
    public void init() {
        if (!enabled) {
            return;
        }
        DefaultMQProducer p = new DefaultMQProducer(producerGroup);
        p.setNamesrvAddr(nameServer);
        p.setSendMsgTimeout(sendTimeoutMs);
        p.setRetryTimesWhenSendFailed(retryTimes);
        try {
            p.start();
            this.producer = p;
            log.info("RocketMQ producer started: group={}, nameServer={}", producerGroup, nameServer);
        } catch (MQClientException e) {
            log.error("RocketMQ producer start failed: group={}, nameServer={}", producerGroup, nameServer, e);
            throw new IllegalStateException(e);
        }
    }

    /**
     * 销毁RocketMQ生产者，释放相关资源
     * 在Spring容器关闭时自动调用
     */
    @Override
    public void destroy() {
        if (producer != null) {
            try {
                producer.shutdown();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 同步发送消息，等待发送结果返回
     *
     * @return 消息ID，发送失败时返回null
     */
    @Override
    public String sendSync(String topic, String tag, Object payload, String key, Map<String, String> headers) {
        if (!enabled) {
            return null;
        }
        Message message = toMessage(topic, tag, payload, key, headers);
        try {
            SendResult result = producer.send(message);
            return result.getMsgId();
        } catch (Exception e) {
            log.error("MQ sendSync failed: topic={}, tag={}, key={}", topic, tag, key, e);
            return null;
        }
    }


    /**
     * 同步发送延时消息，消息将在指定延时等级后被消费
     *
     * @return 消息ID，发送失败时返回null
     */
    @Override
    public String sendDelaySync(String topic, String tag, Object payload, int delayLevel, String key,
                                Map<String, String> headers) {
        if (!enabled) {
            return null;
        }
        Message message = toMessage(topic, tag, payload, key, headers);
        message.setDelayTimeLevel(delayLevel);
        try {
            SendResult result = producer.send(message);
            return result.getMsgId();
        } catch (Exception e) {
            log.error("MQ sendDelaySync failed: topic={}, tag={}, key={}, level={}", topic, tag, key, delayLevel, e);
            return null;
        }
    }

    /**
     * 将业务对象转换为RocketMQ消息对象
     * 包含JSON序列化、消息属性设置等操作
     *
     * @throws IllegalArgumentException 当对象序列化失败时抛出
     */
    private Message toMessage(String topic, String tag, Object payload, String key, Map<String, String> headers) {
        try {
            byte[] body = objectMapper.writeValueAsBytes(payload);
            Message message = new Message(topic, tag, body);
            if (key != null && !key.isEmpty()) {
                message.setKeys(key);
            }
            if (headers != null) {
                headers.forEach(message::putUserProperty);
            }
            message.putUserProperty("Content-Type", "application/json;charset="
                    + StandardCharsets.UTF_8.name());
            return message;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}


