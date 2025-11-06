package com.xpcjsu.sunshinemall.order;

// RocketMQ已禁用
// import org.apache.rocketmq.spring.core.RocketMQTemplate;
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
@EnableFeignClients(basePackages = {
        "com.xpcjsu.sunshinemall.framework.common.feign.clients"
})
@EnableTransactionManagement
@MapperScan("com.xpcjsu.sunshinemall.order.mapper")
public class OrderServiceApplication {

    public static void main(String[] args) {

        SpringApplication.run(OrderServiceApplication.class, args);

/*
        ConfigurableApplicationContext context = SpringApplication.run(OrderServiceApplication.class, args);
        // 读取配置值
        String nameServerAddr = context.getEnvironment().getProperty("rocketmq.name-server");
        System.out.println("配置文件中的rocketmq.name-server值: " + nameServerAddr);

        // 获取实际初始化的 RocketMQTemplate 来验证配置
        try {
            RocketMQTemplate rocketMQTemplate = context.getBean(RocketMQTemplate.class);
            if (rocketMQTemplate.getProducer() != null) {
                String actualNamesrvAddr = rocketMQTemplate.getProducer().getNamesrvAddr();
                System.out.println("实际使用的NameServer地址: " + actualNamesrvAddr);
                System.out.println("配置验证: 成功");
            } else {
                System.out.println("配置验证: 失败 - Producer未初始化");
            }
        } catch (Exception e) {
            System.out.println("配置验证: 失败 - " + e.getMessage());
        }*/
    }

}