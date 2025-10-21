package com.xpcjsu.sunshinemall.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 用户服务启动类
 *
 * @author sunshine-mall
 * @since 1.0.0
 */
@SpringBootApplication(scanBasePackages = {
    "com.xpcjsu.sunshinemall.user",
    "com.xpcjsu.sunshinemall.framework"
})
@EnableDiscoveryClient
@MapperScan("com.xpcjsu.sunshinemall.user.mapper")
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
