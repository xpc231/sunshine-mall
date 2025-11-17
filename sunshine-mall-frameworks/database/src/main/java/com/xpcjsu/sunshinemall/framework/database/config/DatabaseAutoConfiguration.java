package com.xpcjsu.sunshinemall.framework.database.config;

import com.xpcjsu.sunshinemall.framework.database.handler.MyMetaObjectHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MyMetaObjectHandler.class)
    public MyMetaObjectHandler myMetaObjectHandler() {
        return new MyMetaObjectHandler();
    }
}