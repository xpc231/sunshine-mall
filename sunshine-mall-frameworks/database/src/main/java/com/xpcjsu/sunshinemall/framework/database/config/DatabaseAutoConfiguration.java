package com.xpcjsu.sunshinemall.framework.database.config;

import com.xpcjsu.sunshinemall.framework.database.handler.MyMetaObjectHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseAutoConfiguration {

    @Bean
    public MyMetaObjectHandler myMetaObjectHandler() {
        return new MyMetaObjectHandler();
    }
}