
package com.xpcjsu.sunshinemall.framework.cache.config;

import com.xpcjsu.sunshinemall.framework.cache.core.CacheManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
@ConditionalOnMissingBean(CacheManager.class)
public class CacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CacheManager cacheManager(RedisTemplate<String, Object> redisTemplate) {
        return new CacheManager(redisTemplate);
    }
}
