package com.xpcjsu.sunshinemall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 商品服务启动类
 * 
 * @author xpcjsu
 */
@SpringBootApplication(scanBasePackages = "com.xpcjsu.sunshinemall")
@EnableDiscoveryClient
@MapperScan("com.xpcjsu.sunshinemall.product.mapper")
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }

}
