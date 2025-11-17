package com.xpcjsu.sunshinemall.framework.common.mq;

import java.util.Map;

public interface MqClient {

    String sendSync(String topic, String tag, Object payload, String key, Map<String, String> headers);

    String sendDelaySync(String topic, String tag, Object payload, int delayLevel, String key, Map<String, String> headers);
}

