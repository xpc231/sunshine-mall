package com.xpcjsu.sunshinemall.framework.distributedid.config;

import com.xpcjsu.sunshinemall.framework.base.singleton.SingletonHolder;
import com.xpcjsu.sunshinemall.framework.distributedid.core.SnowflakeIdGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 分布式ID模块配置类
 * <p>
 * 提供SnowflakeIdGenerator的Bean注册，支持Spring容器管理。
 * 使用SingletonHolder确保全局唯一实例。

 * 组件扫描: RedisConfig 位于组件扫描路径下，Spring会自动发现并加载
 * 自动配置: DistributedIdConfig 需要通过 spring.factories 文件声明才能被自动加载
 *
 * @author sunshine-mall
 * @since 1.0.0
 */
@Configuration
public class DistributedIdConfig {

    /**
     * 注册雪花算法ID生成器Bean
     * <p>
     * 使用SingletonHolder获取单例，确保全局唯一。
     * ConditionalOnMissingBean保证用户可以自定义替换。
     * 
     * @return SnowflakeIdGenerator单例
     */
    @Bean
    @ConditionalOnMissingBean(SnowflakeIdGenerator.class)
    public SnowflakeIdGenerator snowflakeIdGenerator() {
        return SingletonHolder.getInstance(SnowflakeIdGenerator.class);
    }
}
