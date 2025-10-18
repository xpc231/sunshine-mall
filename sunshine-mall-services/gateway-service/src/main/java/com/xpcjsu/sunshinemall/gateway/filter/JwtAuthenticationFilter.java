package com.xpcjsu.sunshinemall.gateway.filter;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.xpcjsu.sunshinemall.gateway.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * JWT 认证全局过滤器
 * 
 * 功能：
 * - 拦截所有请求进行Token验证
 * - 白名单路径放行
 * - 无效Token返回401错误
 * - 验证通过后将用户信息传递给下游服务
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    /**
     * 白名单路径（无需Token验证）
     */
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/user/login",
            "/api/user/register",
            "/api/user/captcha",
            "/actuator/health",
            "/actuator/info"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 白名单路径直接放行
        if (isWhiteList(path)) {
            log.debug("白名单路径放行: {}", path);
            return chain.filter(exchange);
        }

        // 获取Token
        String authHeader = request.getHeaders().getFirst("Authorization");
        String token = jwtUtil.extractToken(authHeader);

        // Token不存在
        if (token == null) {
            log.warn("请求路径 {} 缺少Token", path);
            return unauthorizedResponse(exchange, "缺少认证Token");
        }

        // Token验证
        if (!jwtUtil.validateToken(token)) {
            log.warn("请求路径 {} Token验证失败", path);
            return unauthorizedResponse(exchange, "Token无效或已过期");
        }

        // Token验证通过，提取用户信息并传递给下游服务
        try {
            DecodedJWT decodedJWT = jwtUtil.parseToken(token);
            String userId = jwtUtil.getUserIdFromToken(decodedJWT);
            String username = jwtUtil.getUsernameFromToken(decodedJWT);

            // 将用户信息添加到请求头，传递给下游服务
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-Username", username)
                    .build();

            log.debug("Token验证通过 - userId: {}, username: {}, path: {}", userId, username, path);

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (Exception e) {
            log.error("Token解析失败 - path: {}", path, e);
            return unauthorizedResponse(exchange, "Token解析失败");
        }
    }

    /**
     * 判断是否为白名单路径
     *
     * @param path 请求路径
     * @return true:白名单, false:非白名单
     */
    private boolean isWhiteList(String path) {
        // 空指针检查：防御性编程
        if (path == null) {
            return false;
        }
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    /**
     * 返回未授权响应
     *
     * @param exchange ServerWebExchange
     * @param message 错误消息
     * @return Mono<Void>
     */
    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");

        String body = String.format("{\"code\":401,\"message\":\"%s\"}", message);
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    /**
     * 过滤器优先级（数值越小，优先级越高）
     * 认证过滤器应该在其他业务过滤器之前执行
     */
    @Override
    public int getOrder() {
        return -100;
    }
}
