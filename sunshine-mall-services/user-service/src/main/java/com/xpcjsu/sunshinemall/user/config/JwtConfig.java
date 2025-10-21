package com.xpcjsu.sunshinemall.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 配置类
 *
 * @author sunshine-mall
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /**
     * JWT 密钥
     */
    private String secret;

    /**
     * JWT 过期时间（毫秒）
     */
    private Long expiration;
}
