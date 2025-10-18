package com.xpcjsu.sunshinemall.gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 网关全局异常处理器
 * 
 * 功能：
 * - 统一异常响应格式
 * - 捕获网关层异常
 * - 日志记录
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
@Slf4j
@Order(-1)
@Component
@RequiredArgsConstructor
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        // 设置响应头
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 根据异常类型设置状态码
        HttpStatus status = determineStatus(ex);
        response.setStatusCode(status);

        // 构建错误响应
        Map<String, Object> errorResponse = buildErrorResponse(ex, status);

        // 日志记录
        log.error("网关异常: path={}, status={}, message={}", 
                exchange.getRequest().getURI().getPath(),
                status.value(), 
                ex.getMessage(), 
                ex);

        return response.writeWith(Mono.fromSupplier(() -> {
            try {
                byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
                DataBuffer buffer = response.bufferFactory().wrap(bytes);
                return buffer;
            } catch (JsonProcessingException e) {
                log.error("序列化错误响应失败", e);
                String fallback = "{\"code\":500,\"message\":\"系统内部错误\"}";
                return response.bufferFactory()
                        .wrap(fallback.getBytes(StandardCharsets.UTF_8));
            }
        }));
    }

    /**
     * 根据异常类型确定HTTP状态码
     */
    private HttpStatus determineStatus(Throwable ex) {
/*        if (ex instanceof ResponseStatusException) {
            return ((ResponseStatusException) ex).getStatusCode();
        }*/
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * 构建错误响应体
     */
    private Map<String, Object> buildErrorResponse(Throwable ex, HttpStatus status) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("code", status.value());
        errorResponse.put("message", ex.getMessage() != null ? ex.getMessage() : status.getReasonPhrase());
        errorResponse.put("timestamp", System.currentTimeMillis());
        return errorResponse;
    }
}
