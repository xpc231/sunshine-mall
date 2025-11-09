package com.xpcjsu.sunshinemall.gateway.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;
import com.xpcjsu.sunshinemall.framework.cache.core.CacheKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

/**
 * 令牌桶服务
 * <p>
 * 基于 Guava RateLimiter 的本地令牌桶限流实现
 * 使用响应式编程适配 Gateway 的 Reactor 模型
 * 说明：该实现为单机限流（每个网关实例独立限流），不依赖Redis
 *
 * @author sunshine-mall
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBucketService {
    /**
     * 本地 RateLimiter 缓存
     * 过期策略：1小时未访问自动过期，最大缓存 10000 个 key
     */
    private final Cache<String, RateLimiter> limiterCache = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .maximumSize(10_000)
            .build();

    /**
     * 尝试获取令牌
     *
     * @param identifier 限流标识（如：IP地址、用户ID等）
     * @param capacity   令牌桶容量
     * @param refillRate 令牌填充速率（每秒）
     * @param requested  请求的令牌数（通常为1）
     * @return Mono<Boolean> true=获取成功，false=获取失败（限流）
     */
    public Mono<Boolean> tryAcquire(String identifier, int capacity, int refillRate, int requested) {
        // 使用 identifier + capacity + refillRate 构建唯一Key，避免不同规则相互干扰
        String limiterKey = CacheKeyBuilder.build("rate-limit", "token-bucket",
                identifier + ":" + capacity + ":" + refillRate);

        // 获取或创建 RateLimiter（permitsPerSecond 映射为 refillRate）
        RateLimiter limiter = limiterCache.getIfPresent(limiterKey);
        if (limiter == null) {
            limiter = RateLimiter.create(refillRate);
            limiterCache.put(limiterKey, limiter);
            log.debug("创建RateLimiter - key: {}, permitsPerSecond: {}", limiterKey, refillRate);
        }

        boolean acquired = requested <= 1 ? limiter.tryAcquire() : limiter.tryAcquire(requested);

        if (acquired) {
            log.debug("令牌获取成功 - key: {}", limiterKey);
        } else {
            log.debug("令牌获取失败（限流） - key: {}", limiterKey);
        }

        return Mono.just(acquired);
    }
}

