
package com.xpcjsu.sunshinemall.framework.cache.config;

import com.xpcjsu.sunshinemall.framework.cache.core.CacheManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
@ConditionalOnClass(RedisTemplate.class)
@ConditionalOnMissingBean(CacheManager.class)
public class CacheAutoConfiguration {

    @Bean
    @ConditionalOnBean(RedisTemplate.class)
    @ConditionalOnMissingBean
    public CacheManager cacheManager(RedisTemplate<String, Object> redisTemplate) {
        return new CacheManager(redisTemplate);
    }
}
