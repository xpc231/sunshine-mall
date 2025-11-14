package com.xpcjsu.sunshinemall.framework.common.mq;

import org.apache.rocketmq.client.producer.SendCallback;

import java.util.Map;

public interface MqClient {

    String sendSync(String topic, String tag, Object payload, String key, Map<String, String> headers);

    void sendAsync(String topic, String tag, Object payload, String key, Map<String, String> headers, SendCallback callback);

    void sendOneWay(String topic, String tag, Object payload, String key, Map<String, String> headers);

    String sendDelaySync(String topic, String tag, Object payload, int delayLevel, String key, Map<String, String> headers);
}

