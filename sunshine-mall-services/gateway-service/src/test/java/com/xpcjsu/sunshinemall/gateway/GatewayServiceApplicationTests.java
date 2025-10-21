package com.xpcjsu.sunshinemall.gateway;

import com.xpcjsu.sunshinemall.gateway.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * 网关服务集成测试
 * 
 * 测试场景：
 * 1. 健康检查接口（白名单）
 * 2. 缺少Token访问受保护接口
 * 3. 无效Token访问受保护接口
 * 4. 有效Token访问受保护接口
 * 5. CORS跨域请求
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayServiceApplicationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtUtil jwtUtil;

    private String validToken;

    @BeforeEach
    void setUp() {
        // 生成测试用的有效Token
        validToken = jwtUtil.generateToken("10001", "testuser");
    }

    @Test
    @DisplayName("测试健康检查接口 - 白名单路径无需Token")
    void testHealthCheck() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("测试缺少Token访问受保护接口 - 应返回401")
    void testMissingToken() {
        webTestClient.get()
                .uri("/api/user/profile")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo(401)
                .jsonPath("$.message").isEqualTo("缺少认证Token");
    }

    @Test
    @DisplayName("测试无效Token访问受保护接口 - 应返回401")
    void testInvalidToken() {
        webTestClient.get()
                .uri("/api/user/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token-12345")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo(401)
                .jsonPath("$.message").isEqualTo("Token无效或已过期");
    }

    @Test
    @DisplayName("测试有效Token访问受保护接口 - 应成功转发（后端服务未启动会返回503）")
    void testValidToken() {
        webTestClient.get()
                .uri("/api/user/profile")
                .header(HttpHeaders.AUTHORIZATION, JwtUtil.TOKEN_PREFIX + validToken)
                .exchange()
                // 后端服务未启动，期望返回503（服务不可用）
                // 如果返回503说明Token验证通过，只是后端服务未启动
                .expectStatus().is5xxServerError();
    }

    @Test
    @DisplayName("测试CORS预检请求 - 应返回200")
    void testCorsPreflightRequest() {
        webTestClient.options()
                .uri("/api/user/login")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)
                .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)
                .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS);
    }

    @Test
    @DisplayName("测试登录接口 - 白名单路径无需Token")
    void testLoginEndpoint() {
        webTestClient.post()
                .uri("/api/user/login")
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .bodyValue("{\"username\":\"test\",\"password\":\"123456\"}")
                .exchange()
                // 后端服务未启动，期望返回503
                .expectStatus().is5xxServerError();
    }

    @Test
    @DisplayName("测试Token中的用户信息传递")
    void testUserInfoInToken() {
        // 验证Token解析功能
        String userId = jwtUtil.getUserIdFromToken(jwtUtil.parseToken(validToken));
        String username = jwtUtil.getUsernameFromToken(jwtUtil.parseToken(validToken));

        assert userId.equals("10001");
        assert username.equals("testuser");
    }

    @Test
    @DisplayName("测试Token过期检查")
    void testTokenExpiration() {
        // 验证Token未过期
        boolean expired = jwtUtil.isTokenExpired(validToken);
        assert !expired;
    }
}
