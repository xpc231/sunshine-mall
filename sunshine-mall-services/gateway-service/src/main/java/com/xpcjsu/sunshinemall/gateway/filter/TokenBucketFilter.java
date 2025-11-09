package com.xpcjsu.sunshinemall.gateway.filter;

import com.xpcjsu.sunshinemall.gateway.config.TokenBucketProperties;
import com.xpcjsu.sunshinemall.gateway.exception.RateLimitException;
import com.xpcjsu.sunshinemall.gateway.service.TokenBucketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 令牌桶限流过滤器
 * <p>
 * 在认证过滤器之前执行限流检查
 * 支持全局限流和接口级限流
 *
 * @author sunshine-mall
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenBucketFilter implements GlobalFilter, Ordered {

    private final TokenBucketProperties tokenBucketProperties;
    private final TokenBucketService tokenBucketService;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    /**
     * 过滤器执行顺序
     * 设置为-1，确保在认证过滤器（Order=0）之前执行
     */
    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 检查是否启用限流
        if (!tokenBucketProperties.isEnabled()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 2. 检查白名单
        if (isExcludePath(path)) {
            log.debug("路径在白名单中，跳过限流 - path: {}", path);
            return chain.filter(exchange);
        }

        // 3. 获取限流标识（优先使用用户ID，否则使用IP地址）
        String identifier = getIdentifier(request);

        // 4. 获取限流配置（接口级配置优先，否则使用全局配置）
        TokenBucketProperties.PathRule rule = getLimitRule(path);
        int capacity = rule != null ? rule.getCapacity() : tokenBucketProperties.getGlobal().getCapacity();
        int refillRate = rule != null ? rule.getRefillRate() : tokenBucketProperties.getGlobal().getRefillRate();

        // 5. 尝试获取令牌
        return tokenBucketService.tryAcquire(identifier, capacity, refillRate, 1)
                .flatMap(acquired -> {
                    if (acquired) {
                        // 获取令牌成功，放行
                        log.debug("限流检查通过 - path: {}, identifier: {}", path, identifier);
                        return chain.filter(exchange);
                    } else {
                        // 获取令牌失败，限流
                        log.warn("请求被限流 - path: {}, identifier: {}, capacity: {}, refillRate: {}",
                                path, identifier, capacity, refillRate);
                        return handleRateLimit(exchange);
                    }
                })
                .onErrorResume(error -> {
                    // Redis异常等错误，记录日志但放行（保证系统可用性）
                    log.error("限流检查异常 - path: {}, identifier: {}", path, identifier, error);
                    return chain.filter(exchange);
                });
    }

    /**
     * 检查路径是否在白名单中
     */
    private boolean isExcludePath(String path) {
        if (tokenBucketProperties.getExcludePaths() == null) {
            return false;
        }
        return tokenBucketProperties.getExcludePaths().stream()
                .anyMatch(pattern -> antPathMatcher.match(pattern, path));
    }

    /**
     * 获取限流标识
     * <p>
     * 注意：限流过滤器在认证过滤器之前执行，所以此时还没有用户信息
     * 因此使用IP地址作为限流标识
     * <p>
     * 如果需要用户级限流，可以：
     * 1. 在认证过滤器之后添加另一个限流过滤器
     * 2. 或者在业务层进行用户级限流
     */
    private String getIdentifier(ServerHttpRequest request) {
        // 使用IP地址作为限流标识
        String ip = getClientIp(request);
        return "ip:" + ip;
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(ServerHttpRequest request) {
        // 优先从X-Forwarded-For头获取（经过代理的情况）
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For可能包含多个IP，取第一个
            return xForwardedFor.split(",")[0].trim();
        }

        // 从X-Real-IP头获取
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        // 从远程地址获取
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }

        return "unknown";
    }

    /**
     * 获取接口级限流规则
     */
    private TokenBucketProperties.PathRule getLimitRule(String path) {
        if (tokenBucketProperties.getPathRules() == null || tokenBucketProperties.getPathRules().isEmpty()) {
            return null;
        }

        // 按路径模式匹配
        return tokenBucketProperties.getPathRules().entrySet().stream()
                .filter(entry -> antPathMatcher.match(entry.getKey(), path))
                .map(entry -> {
                    log.debug("匹配到接口级限流规则 - path: {}, pattern: {}, rule: {}", 
                            path, entry.getKey(), entry.getValue());
                    return entry.getValue();
                })
                .findFirst()
                .orElse(null);
    }

    /**
     * 处理限流响应
     */
    private Mono<Void> handleRateLimit(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");

        // 构造错误响应
        String errorResponse = """
                {
                    "code": 429,
                    "message": "请求过于频繁，请稍后重试",
                    "timestamp": %d
                }
                """.formatted(System.currentTimeMillis());

        byte[] bytes = errorResponse.getBytes(StandardCharsets.UTF_8);
        org.springframework.core.io.buffer.DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}

