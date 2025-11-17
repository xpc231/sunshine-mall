package com.xpcjsu.sunshinemall.framework.common.mq;

/**
 * 全局MQ常量封装（最小集）
 * 说明：仅统一跨服务可复用的主题与标签；生产者/消费者组名保留在各服务的配置文件中。
 */
public final class MqConstant {

    private MqConstant() {}

    /** 订单相关主题与标签 */
    public static final class Order {
        /** 订单事件主题（创建、支付成功、取消等） */
        public static final String TOPIC_EVENT = "order-event-topic";
        /** 订单延迟主题（超时自动取消） */
        public static final String TOPIC_DELAY = "order-delay-topic";

        /** 标签定义 */
        public static final class Tag {
            public static final String CREATED = "created";
            public static final String PAID = "paid";
            public static final String CANCELLED = "cancelled";
            public static final String TIMEOUT_CANCELLED = "timeout-cancelled";
            public static final String SHIPPED = "shipped";
        }
    }
}