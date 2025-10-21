package com.xpcjsu.sunshinemall.framework.cache.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

//组件扫描机制自动加载
//当Spring Boot应用启动时，会自动扫描并加载该配置类,@Bean 注解的方法会自动注册到Spring容器中

@Configuration
public class RedisConfig {

    /**
     * 配置RedisTemplate
     * <p>
     * 使用Jackson JSON作为序列化器，支持泛型和复杂对象，包括Java 8时间类型
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 配置支持Java 8时间类型的ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 注册Java 8时间模块
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        
        // 配置LocalDateTime的序列化和反序列化格式
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));
        
        objectMapper.registerModule(javaTimeModule);
        
        // 禁用将日期写为时间戳的功能（使用格式化字符串）
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // JSON序列化器：将对象转为JSON字符串，支持复杂对象、泛型和Java 8时间类型
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        // 字符串序列化器：将字符串直接转为字节数组，无额外格式
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // 配置 Key 和 HashKey 的序列化方式（用字符串序列化器）
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // 配置 Value 和 HashValue 的序列化方式（用 JSON 序列化器）
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        // Bean属性设置完成后执行初始化操作
        template.afterPropertiesSet();

        return template;
    }
}
