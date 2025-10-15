package com.xpcjsu.sunshinemall.framework.database;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Database模块测试启动类
 */
@SpringBootApplication
@MapperScan("com.xpcjsu.sunshinemall.framework.database.mapper")
public class DatabaseTestApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(DatabaseTestApplication.class, args);
    }
}
