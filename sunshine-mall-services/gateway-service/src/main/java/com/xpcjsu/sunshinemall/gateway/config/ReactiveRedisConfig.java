package com.xpcjsu.sunshinemall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 响应式 Redis 客户端
 * <p>
 * 为Gateway提供响应式Redis操作支持，用于令牌桶限流等场景
 * <p>
 *使用默认序列化器存储的结果（二进制数据）：\xAC\xED\x00\x05t\x00\x04test
 * <p>
 *使用StringRedisSerializer存储的结果（可读文本）："test"
 * @author sunshine-mall
 * @since 1.0.0
 */
@Configuration
public class ReactiveRedisConfig {

   /**
     * 配置响应式RedisTemplate
     * <p>
     * Key和Value都使用String序列化，适合令牌桶场景（存储数字）
     *
    */
    @Bean
    public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {
        // 创建字符串序列化器，用于key和value的序列化
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // 构建Redis序列化上下文，指定key、value、hashKey、hashValue都使用字符串序列化
        RedisSerializationContext<String, String> serializationContext =
                RedisSerializationContext.<String, String>newSerializationContext()
                        .key(stringSerializer)
                        .value(stringSerializer)
                        .hashKey(stringSerializer)
                        .hashValue(stringSerializer)
                        .build();

        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }
}

