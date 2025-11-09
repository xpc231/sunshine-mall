package com.xpcjsu.sunshinemall.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 令牌桶配置属性
 * <p>
 * 支持全局限流和接口级限流配置
 *
 * @author sunshine-mall
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "rate-limit.token-bucket")
public class TokenBucketProperties {

    /**
     * 是否启用令牌桶限流
     */
    private boolean enabled = true;

    /**
     * 全局限流配置
     */
    private GlobalConfig global = new GlobalConfig();

    /**
     * 接口级限流配置
     * Key: 路径模式（支持Ant风格，如 /api/seckill/**）
     * Value: 限流规则
     */
    private Map<String, PathRule> pathRules = new HashMap<>();

    /**
     * 白名单路径（不进行限流）
     */
    private List<String> excludePaths = List.of(
            "/actuator/**",
            "/api/user/login",
            "/api/user/register"
    );

    /**
     * 全局限流配置
     */
    @Data
    public static class GlobalConfig {
        /**
         * 令牌桶容量
         */
        private int capacity = 100;

        /**
         * 令牌填充速率（每秒填充的令牌数）
         */
        private int refillRate = 10;
    }

    /**
     * 接口级限流规则
     */
    @Data
    public static class PathRule {
        /**
         * 令牌桶容量
         */
        private int capacity;

        /**
         * 令牌填充速率（每秒填充的令牌数）
         */
        private int refillRate;
    }
}

