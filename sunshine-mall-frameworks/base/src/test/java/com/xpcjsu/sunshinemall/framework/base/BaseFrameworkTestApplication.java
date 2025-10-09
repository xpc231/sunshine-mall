package com.xpcjsu.sunshinemall.framework.base;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Base框架模块测试启动类
 * 
 * 用于测试环境下启动Spring容器
 * 验证ApplicationContextHolder等组件的功能
 * 
 * @author sunshine-mall
 * @version 1.0
 */
@SpringBootApplication
public class BaseFrameworkTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(BaseFrameworkTestApplication.class, args);
    }
}