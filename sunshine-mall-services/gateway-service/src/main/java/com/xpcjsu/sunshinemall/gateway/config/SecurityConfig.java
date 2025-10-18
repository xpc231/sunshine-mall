package com.xpcjsu.sunshinemall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Spring Security 配置
 * 
 * 功能：
 * - 禁用默认的登录页面
 * - 禁用CSRF（使用JWT无需CSRF防护）
 * - 允许所有请求通过Security（认证由自定义过滤器处理）
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                // 禁用CSRF（JWT无状态认证无需CSRF）
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                
                // 禁用HTTP Basic认证
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                
                // 禁用表单登录
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                
                // 禁用登出
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                
                // 允许所有请求（认证由JwtAuthenticationFilter处理）
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().permitAll()
                )
                
                .build();
    }
}
