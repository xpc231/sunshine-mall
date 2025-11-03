package com.xpcjsu.sunshinemall.order.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 网关访问强制过滤器
 *
 * 目的：
 * - 强制所有外部访问必须通过网关（要求携带 X-Gateway-Request: true）
 * - 防止绕过网关直接访问订单服务接口
 * - 保持最小改动，不引入复杂依赖
 *
 * 规则：
 * - 仅对 /order/api/** 生效
 * - 放行内部回调接口：/order/api/order/pay/success
 */
@Slf4j
@Component
public class GatewayEnforceFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();

        // 仅对订单服务API进行校验
        if (uri != null && uri.startsWith("/order/api/")) {
            // 内部回调接口放行（支付成功回调）
            if (uri.startsWith("/order/api/order/pay/success")) {
                filterChain.doFilter(request, response);
                return;
            }

            String fromGateway = request.getHeader("X-Gateway-Request");
            if (!"true".equalsIgnoreCase(fromGateway)) {
                // 记录日志，便于追踪来源
                String remote = request.getRemoteAddr();
                String ua = request.getHeader("User-Agent");
                log.warn("拒绝非网关访问 - uri={}, remote={}, ua={}", uri, remote, ua);

                // 返回403
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.setContentType("application/json;charset=UTF-8");
                String body = "{\"code\":403,\"message\":\"禁止直接访问服务，请通过网关\"}";
                response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}