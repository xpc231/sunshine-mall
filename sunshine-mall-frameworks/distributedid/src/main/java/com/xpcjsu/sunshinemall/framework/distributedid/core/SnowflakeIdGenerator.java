package com.xpcjsu.sunshinemall.framework.distributedid.core;

import com.xpcjsu.sunshinemall.framework.base.config.ConfigManager;
import com.xpcjsu.sunshinemall.framework.base.singleton.SingletonHolder;
import lombok.Getter;
import org.springframework.stereotype.Component;

import static com.xpcjsu.sunshinemall.framework.distributedid.core.SnowflakeConstants.*;

/**
 * 雪花算法分布式ID生成器
 * <p>
 * 实现Twitter的Snowflake算法，生成64位全局唯一ID。
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
@Component
@Getter
public final class SnowflakeIdGenerator {

    /**
     * 工作机器ID（0-31）
     */
    private final long workerId;

    /**
     * 数据中心ID（0-31）
     */
    private final long datacenterId;

    /**
     * 序列号（0-4095）实例变量
     */
    private long sequence = 0L;

    /**
     * 上次生成ID的时间戳，实例变量,-1L表示尚未记录
     */
    private long lastTimestamp = -1L;

    /**
     * 私有构造器，防止外部直接实例化
     * 建议通过SingletonHolder获取单例
     */
    private SnowflakeIdGenerator() {
        // 从配置中读取DatacenterId和WorkerId
        // 注意：ConfigManager可能无法从 Spring Environment 读取配置（容器未完全初始化）
        // 因此需要提供默认值保证创建成功
        ConfigManager configManager = SingletonHolder.getInstance(ConfigManager.class);
        
        this.datacenterId = configManager.getLong("distributed-id.datacenter-id", 0L);
        this.workerId = configManager.getLong("distributed-id.worker-id", 0L);

        // 参数校验
        validateParameters(datacenterId, workerId);
    }

    /**
     * 公共构造器，用于Spring Bean创建
     * 
     * @param datacenterId 数据中心ID（0-31）
     * @param workerId     工作机器ID（0-31）
     */
    public SnowflakeIdGenerator(long datacenterId, long workerId) {
        this.datacenterId = datacenterId;
        this.workerId = workerId;
        
        // 参数校验
        validateParameters(datacenterId, workerId);
    }

    /**
     * 验证参数有效性
     * 
     * @param datacenterId 数据中心ID
     * @param workerId     工作机器ID
     */
    private void validateParameters(long datacenterId, long workerId) {
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException(
                String.format("DatacenterId 必须在 0-%d 之间，当前值: %d", MAX_DATACENTER_ID, datacenterId)
            );
        }
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(
                String.format("WorkerId 必须在 0-%d 之间，当前值: %d", MAX_WORKER_ID, workerId)
            );
        }
    }

    /**
     * 生成下一个ID（线程安全）
     * 
     * @return 64位全局唯一ID
     * @throws RuntimeException 当时钟回拨时抛出异常
     */
    public synchronized long nextId() {

        long timestamp = currentTimeMillis();

        // 时钟回拨检测
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(
                //String.format()创建格式化的字符串
                String.format("时钟回拨检测：拒绝生成ID %d 毫秒", lastTimestamp - timestamp)
            );
        }

        // 同一毫秒内，序列号递增
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;//对4096取模运算,比传统的 % 取模运算性能更高
            // 序列号溢出，等待下一毫秒
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            // 不同毫秒，序列号重置为0
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        // 组装64位ID,| 是按位或运算符
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 获取当前时间戳（毫秒）
     * 
     * @return 当前时间戳
     */
    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 等待下一毫秒
     * 
     * @param lastTimestamp 上次生成ID的时间戳
     * @return 下一毫秒的时间戳
     */
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimeMillis();
        }
        return timestamp;
    }

    /**
     * 解析ID（用于调试和分析）
     *
     * @param id 雪花算法生成的ID
     * @return ID信息字符串
     */
    public String parseId(long id) {
        long timestamp = ((id >> TIMESTAMP_SHIFT) & ~(-1L << 41)) + EPOCH;
        long datacenterId = (id >> DATACENTER_ID_SHIFT) & MAX_DATACENTER_ID;
        long workerId = (id >> WORKER_ID_SHIFT) & MAX_WORKER_ID;
        long sequence = id & MAX_SEQUENCE;

        return String.format(
                "ID: %d, Timestamp: %d, DatacenterId: %d, WorkerId: %d, Sequence: %d",
                id, timestamp, datacenterId, workerId, sequence
        );
    }
}
