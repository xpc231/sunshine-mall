package com.xpcjsu.sunshinemall.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 网关服务启动类
 * 
 * 职责：
 * - 统一入口：所有外部请求的唯一入口
 * - 路由转发：基于配置的动态路由
 * - 认证鉴权：JWT Token验证
 * - CORS处理：统一跨域配置
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}
