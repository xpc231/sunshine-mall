package com.xpcjsu.sunshinemall.order.constant;

/**
 * 订单服务常量定义
 */
public final class OrderConstants {

    private OrderConstants() {}

    /**
     * RocketMQ相关常量
     */
    public static final class MQ {
        /** 订单事件主题（创建、支付成功、取消等） */
        public static final String ORDER_EVENT_TOPIC = "order-event-topic";
        /** 订单延迟/超时主题（未支付自动取消等） */
        public static final String ORDER_DELAY_TOPIC = "order-delay-topic";

        /** 生产者组（在application.yml中同名配置） */
        public static final String PRODUCER_GROUP = "order-service-producer-group";
        /** 消费者组（可用于订单相关消费者） */
        public static final String CONSUMER_GROUP = "order-service-consumer-group";

        /** 标签定义 */
        public static final class Tags {
            public static final String CREATED = "created";
            public static final String PAID = "paid";
            public static final String CANCELLED = "cancelled";
            public static final String TIMEOUT_CANCELLED = "timeout-cancelled";
        }
    }

    /**
     * 业务常量
     */
    public static final class Biz {
        /** 自动确认收货天数默认值 */
        public static final int DEFAULT_AUTO_CONFIRM_DAY = 7;
        /** 订单备注默认值 */
        public static final String DEFAULT_NOTE = "";
    }
}