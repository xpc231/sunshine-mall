package com.xpcjsu.sunshinemall.framework.distributedid.core;

/**
 * 雪花算法常量配置
 * <p>
 * 集中管理雪花算法相关的常量定义，包括起始时间戳、各部分位数、最大值和位移量。
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
public final class SnowflakeConstants {

    /**
     * 起始时间戳（2024-01-01 00:00:00）
     * 使用固定起始时间，确保时间戳的稳定性
     */
    public static final long EPOCH = 1704067200000L;

    /**
     * 各部分位数
     */
    public static final long WORKER_ID_BITS = 5L;
    public static final long DATACENTER_ID_BITS = 5L;
    public static final long SEQUENCE_BITS = 12L;

    /**
     * 各部分最大值
     * 左移,取反
     */
    public static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);        // 31
    public static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS); // 31
    public static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);          // 4095

    /**
     * 各部分左移位数
     */
    public static final long WORKER_ID_SHIFT = SEQUENCE_BITS;                                    // 12
    public static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;              // 17
    public static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS; // 22

    /**
     * 私有构造器，防止实例化
     */
    private SnowflakeConstants() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }
}
