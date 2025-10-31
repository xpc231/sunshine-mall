package com.xpcjsu.sunshinemall.framework.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置属性
 *
 * @author sunshine-mall
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "sunshine-mall.auth.jwt")
public class JwtProperties {

    /**
     * JWT 密钥
     */
    private String secret = "sunshine-mall-default-secret-key";

    /**
     * JWT 过期时间（毫秒）
     * 默认7天
     */
    private Long expiration = 7 * 24 * 60 * 60 * 1000L;

    /**
     * JWT 签发者
     */
    private String issuer = "sunshine-mall";

    /**
     * JWT Header 名称
     */
    private String headerName = "Authorization";

    /**
     * JWT Token 前缀
     */
    private String tokenPrefix = "Bearer ";

    /**
     * Token 黑名单缓存前缀
     */
    private String blacklistPrefix = "auth:blacklist:";

    /**
     * 用户信息缓存前缀
     */
    private String userCachePrefix = "auth:user:";

    /**
     * 用户信息缓存过期时间（秒）
     * 默认1小时
     */
    private Long userCacheExpire = 3600L;
}