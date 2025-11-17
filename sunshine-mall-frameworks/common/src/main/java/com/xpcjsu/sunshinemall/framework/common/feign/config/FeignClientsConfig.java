package com.xpcjsu.sunshinemall.framework.common.feign.config;

import com.xpcjsu.sunshinemall.framework.common.feign.fallback.OrderClientFallbackFactory;
import com.xpcjsu.sunshinemall.framework.common.feign.fallback.LogisticsClientFallbackFactory;
import com.xpcjsu.sunshinemall.framework.common.feign.fallback.ProductSkuClientFallbackFactory;
import com.xpcjsu.sunshinemall.framework.common.feign.fallback.StockClientFallbackFactory;
import com.xpcjsu.sunshinemall.framework.common.feign.fallback.CartClientFallbackFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 客户端相关配置：
 * 通过 @Bean 注册各 FallbackFactory。
 * - 这样在 @FeignClient 注解的 fallbackFactory 属性中即可直接引用对应的类。
 */
@Configuration
public class FeignClientsConfig {

    @Bean
    public StockClientFallbackFactory stockClientFallbackFactory() {
        return new StockClientFallbackFactory();
    }

    @Bean
    public ProductSkuClientFallbackFactory productSkuClientFallbackFactory() {
        return new ProductSkuClientFallbackFactory();
    }

    @Bean
    public OrderClientFallbackFactory orderClientFallbackFactory() {
        return new OrderClientFallbackFactory();
    }

    @Bean
    public CartClientFallbackFactory cartClientFallbackFactory() {
        return new CartClientFallbackFactory();
    }

    @Bean
    public LogisticsClientFallbackFactory logisticsClientFallbackFactory() {
        return new LogisticsClientFallbackFactory();
    }
}