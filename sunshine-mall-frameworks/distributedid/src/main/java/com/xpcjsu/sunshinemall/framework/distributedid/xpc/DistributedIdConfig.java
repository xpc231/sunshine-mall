package com.xpcjsu.sunshinemall.framework.distributedid.xpc;

import com.xpcjsu.sunshinemall.framework.base.singleton.SingletonHolder;
import com.xpcjsu.sunshinemall.framework.distributedid.core.SnowflakeIdGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DistributedIdConfig {

    @Bean
    @ConditionalOnMissingBean(SnowflakeIdGenerator.class)
    public SnowflakeIdGenerator snowflakeIdGenerator(){
        return SingletonHolder.getInstance(SnowflakeIdGenerator.class);
    }
}
