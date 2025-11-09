/*package com.xpcjsu.sunshinemall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

*//**
 * 响应式Redis配置
 * <p>
 * 为Gateway提供响应式Redis操作支持，用于令牌桶限流等场景
 *
 * @author sunshine-mall
 * @since 1.0.0
 *//*
@Configuration
public class ReactiveRedisConfig {

    *//**
     * 配置响应式RedisTemplate
     * <p>
     * Key和Value都使用String序列化，适合令牌桶场景（存储数字）
     *//*
    @Bean
    public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        RedisSerializationContext<String, String> serializationContext =
                RedisSerializationContext.<String, String>newSerializationContext()
                        .key(stringSerializer)
                        .value(stringSerializer)
                        .hashKey(stringSerializer)
                        .hashValue(stringSerializer)
                        .build();

        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }
}*/

