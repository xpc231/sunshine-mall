package com.xpcjsu.sunshinemall.pay;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 支付服务启动类
 */
@SpringBootApplication(scanBasePackages = "com.xpcjsu.sunshinemall")
@EnableDiscoveryClient
@EnableFeignClients
@EnableTransactionManagement
@MapperScan("com.xpcjsu.sunshinemall.pay.mapper")
public class PayServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(PayServiceApplication.class, args);
    }
}