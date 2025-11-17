package com.xpcjsu.sunshinemall.order;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 订单服务启动类
 */
@SpringBootApplication(scanBasePackages = "com.xpcjsu.sunshinemall")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.xpcjsu.sunshinemall.framework.common.feign.clients"})
@EnableTransactionManagement
@MapperScan("com.xpcjsu.sunshinemall.order.mapper")
public class OrderServiceApplication {

    public static void main(String[] args) {

        SpringApplication.run(OrderServiceApplication.class, args);

    }
}