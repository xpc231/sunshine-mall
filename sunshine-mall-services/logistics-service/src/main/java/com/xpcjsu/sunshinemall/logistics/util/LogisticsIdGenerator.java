package com.xpcjsu.sunshinemall.logistics.util;

import com.xpcjsu.sunshinemall.framework.distributedid.core.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LogisticsIdGenerator {

    private final SnowflakeIdGenerator snowflakeIdGenerator;

    /**
     * 生成分布式唯一ID
     */
    public long nextId() {
        return snowflakeIdGenerator.nextId();
    }

}
