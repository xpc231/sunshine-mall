package com.xpcjsu.sunshinemall.framework.common.mq;

import java.util.HashMap;
import java.util.Map;

/**
 * MQ消息头工具类
 * 提供创建消息头键值对映射的功能
 */
public final class MqMessageHeaders {

    /**
     * 私有构造函数，防止实例化
     */
    private MqMessageHeaders() {}

    /**
     * 根据键值对数组创建Map映射
     *
     * @param kvPairs 键值对数组，按照key1, value1, key2, value2...的顺序排列
     * @return 包含键值对的HashMap，如果输入为null则返回空Map
     */
    public static Map<String, String> of(String... kvPairs) {
        Map<String, String> map = new HashMap<>();
        if (kvPairs == null) {
            return map;
        }
        // 遍历键值对数组，每次处理两个元素作为键值对
        for (int i = 0; i + 1 < kvPairs.length; i += 2) {
            String k = kvPairs[i];
            String v = kvPairs[i + 1];
            if (k != null && v != null) {
                map.put(k, v);
            }
        }
        return map;
    }
}


