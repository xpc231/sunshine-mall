package com.xpcjsu.sunshinemall.gateway.service;

import com.xpcjsu.sunshinemall.framework.cache.core.CacheKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 令牌桶服务
 * <p>
 * 基于 Redis + Lua 的分布式令牌桶限流实现
 * 使用响应式编程适配 Gateway 的 Reactor 模型
 *
 * @author sunshine-mall
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBucketService {

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    /**
     * 令牌桶 Lua 脚本（从 resources 加载）
     */
    private final DefaultRedisScript<List> tokenBucketScript = createScript();

    private DefaultRedisScript<List> createScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/token_bucket.lua"));
        script.setResultType(List.class);
        return script;
    }

    /**
     * 尝试获取令牌（分布式）
     *
     * @param identifier 限流标识（如：IP地址、用户ID等）
     * @param capacity   令牌桶容量
     * @param refillRate 令牌填充速率（每秒）
     * @param requested  请求的令牌数（通常为1）
     * @return Mono<Boolean> true=获取成功，false=获取失败（限流）
     */
    public Mono<Boolean> tryAcquire(String identifier, int capacity, int refillRate, int requested) {

        // 构建令牌桶限流器的缓存键
        String bucketKey = CacheKeyBuilder.build("rate-limit", "token-bucket",
                identifier + ":" + capacity + ":" + refillRate);

        // 构造令牌桶算法所需的键列表和参数列表
        List<String> keys = List.of(bucketKey);
        String now = String.valueOf(System.currentTimeMillis());
        List<String> args = List.of(
                String.valueOf(capacity),
                String.valueOf(refillRate),
                String.valueOf(requested),
                now
        );

        // 执行lua脚本
        return reactiveRedisTemplate.execute(tokenBucketScript, keys, args)
                .next()//返回的 Flux 中获取第一个元素，转换为 Mono<List>，Flux返回多个结果，Mono返回一个结果
                .map(result -> {
                    try {
                        // 从结果列表中获取第一个元素，转换为Long类型，并判断是否等于1L
                        Object allowedObj = ((List<?>) result).get(0);
                        // 从 List<?> 中取出的元素可能是任何类型，统一转化为long类型
                        long allowed = Long.parseLong(String.valueOf(allowedObj));
                        boolean ok = allowed == 1L;

                        if (ok) {
                            log.debug("令牌获取成功 - key: {}", bucketKey);
                        } else {
                            log.debug("令牌获取失败（限流） - key: {}", bucketKey);
                        }
                        return ok;
                    } catch (Exception e) {
                        // Lua脚本结果解析异常处理，默认放行保证系统可用性
                        log.error("Lua脚本结果解析异常，默认放行 - key: {}", bucketKey, e);
                        return true;
                    }
                })
                .onErrorResume(error -> {
                    // Redis异常时默认放行
                    log.error("令牌桶执行异常，默认放行 - key: {}", bucketKey, error);
                    return Mono.just(true);// 避免因限流服务故障导致业务中断
                });
    }
}

