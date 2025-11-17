package com.xpcjsu.sunshinemall.framework.distributedid.config;

import com.xpcjsu.sunshinemall.framework.distributedid.core.SnowflakeIdGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

/**
 * 分布式ID模块配置类
 * <p>
 * 提供SnowflakeIdGenerator的Bean注册，支持Spring容器管理。
 * 直接使用构造器创建，避免SingletonHolder在Spring容器未完全初始化时的问题。

 * 组件扫描: RedisConfig 位于组件扫描路径下，Spring会自动发现并加载
 * 自动配置: DistributedIdConfig 需要通过 spring.factories 文件声明才能被自动加载
 *
 * @author sunshine-mall
 * @since 1.0.0
 */
@Configuration
public class DistributedIdConfig {

    /**
     * 分布式ID配置属性
     */
    @Data
    public static class DistributedIdProperties {
        /**
         * 数据中心ID（0-31）
         */
        private Long datacenterId = 0L;
        
        /**
         * 工作机器ID（0-31）
         */
        private Long workerId = 0L;
    }

    /**
     * 注册分布式ID配置属性Bean
     */
    @Bean
    @ConfigurationProperties(prefix = "distributed-id")
    public DistributedIdProperties distributedIdProperties() {
        return new DistributedIdProperties();
    }

    /**
     * 注册雪花算法ID生成器Bean
     * <p>
     * 直接通过构造器创建，使用Spring注入的配置属性。
     * ConditionalOnMissingBean保证用户可以自定义替换。
     * 
     * @param properties 分布式ID配置属性
     * @return SnowflakeIdGenerator实例
     */
    @Bean
    @ConditionalOnMissingBean(SnowflakeIdGenerator.class)
    public SnowflakeIdGenerator snowflakeIdGenerator(DistributedIdProperties properties) {
        return new SnowflakeIdGenerator(properties.getDatacenterId(), properties.getWorkerId());
    }
}
