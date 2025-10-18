package com.xpcjsu.sunshinemall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * CORS 跨域配置
 * 
 * 功能：
 * - 允许前端跨域访问
 * - 配置允许的域名、方法、头部
 * - 支持携带认证信息（Cookie、Token）
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 允许所有域名跨域访问（生产环境应限制具体域名）
        config.addAllowedOriginPattern("*");

        // 允许所有HTTP方法
        config.addAllowedMethod("*");

        // 允许所有请求头
        config.addAllowedHeader("*");

        // 允许携带认证信息（Cookie、Authorization header）
        config.setAllowCredentials(true);

        // 预检请求的有效期（3600秒 = 1小时）
        config.setMaxAge(3600L);

        // 暴露的响应头（允许前端访问的自定义响应头）
        config.addExposedHeader("X-User-Id");
        config.addExposedHeader("X-Username");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
