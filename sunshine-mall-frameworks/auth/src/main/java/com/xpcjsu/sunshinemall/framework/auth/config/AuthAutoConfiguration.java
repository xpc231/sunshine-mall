package com.xpcjsu.sunshinemall.framework.auth.config;

import com.xpcjsu.sunshinemall.framework.auth.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 认证组件自动配置
 *
 * @author sunshine-mall
 * @since 1.0.0
 */
@Configuration
@ComponentScan("com.xpcjsu.sunshinemall.framework.auth")
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class AuthAutoConfiguration implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/actuator/**",
                        "/error",
                        "/favicon.ico"
                );
    }
}