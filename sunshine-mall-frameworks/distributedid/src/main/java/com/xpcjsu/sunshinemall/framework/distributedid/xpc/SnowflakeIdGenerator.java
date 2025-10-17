package com.xpcjsu.sunshinemall.framework.distributedid.xpc;

import com.xpcjsu.sunshinemall.framework.base.config.ConfigManager;
import com.xpcjsu.sunshinemall.framework.base.singleton.SingletonHolder;
import lombok.Getter;

import static com.xpcjsu.sunshinemall.framework.distributedid.core.SnowflakeConstants.*;

@Getter
public class SnowflakeIdGenerator {

    private final long workerId;

    private final long datacenterId;

    private long sequence = 0L;

    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator() {
        ConfigManager configManager = SingletonHolder.getInstance(ConfigManager.class);

        this.datacenterId = configManager.getLong("distributed-id.datacenter-id", 0L);
        this.workerId = configManager.getLong("distributed-id.worker-id", 0L);

        // 参数校验
        if(datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException(
                String.format("DatacenterId 必须在 0-%d 之间，当前值: %d", MAX_DATACENTER_ID, datacenterId)
            );
        }

        if(workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(
                String.format("WorkerId 必须在 0-%d 之间，当前值: %d", MAX_WORKER_ID, workerId)
            );
        }
    }

    public synchronized long nextId() {

        long timestamp = currentTimeMillis();

        if(timestamp < lastTimestamp) {
            throw new RuntimeException(
                String.format("时钟回拨检测：拒绝生成ID %d 毫秒", lastTimestamp - timestamp)
            );
        }

        // 同一毫秒内，序列号递增
        if(timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            // 序列号溢出，等待下一毫秒
            if(sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            // 不同毫秒，序列号重置为0
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence);
    }

    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    private long waitNextMillis(long lastTimestamp) {
        long timestamp = currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimeMillis();
        }
        return timestamp;
    }

}
