package com.xpcjsu.sunshinemall.framework.cache.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置类
 * <p>
 * 配置RedisTemplate的序列化方式：
 * <ul>
 * <li>Key使用String序列化</li>
 * <li>Value使用Jackson JSON序列化，支持任意Java对象</li>
 * </ul>
 *
 * @author sunshine-mall
 * @since 1.0.0
 */
// 配置前（默认）：key为乱码，value包含类信息
//\xac\xed\x00\x05t\x00\x04use
// 配置后（自定义）：key和value都为可读格式
//user:1001 -> {"id":1001,"name":"张三"}

//当Spring Boot应用启动时，会自动扫描并加载该配置类,@Bean 注解的方法会自动注册到Spring容器中
@Configuration
public class RedisConfig {

    /**
     * 配置RedisTemplate
     * <p>
     * 使用Jackson JSON作为序列化器，支持泛型和复杂对象
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        //JSON序列化器：将对象转为JSON字符串,支持复杂对象和泛型
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        //字符串序列化器：将字符串直接转为字节数组，无额外格式
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        //配置 Key 和 HashKey 的序列化方式（用字符串序列化器）
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        //配置 Value 和 HashValue 的序列化方式（用 JSON 序列化器）
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        //Bean属性设置完成后执行初始化操作
        template.afterPropertiesSet();

        return template;
    }
}
