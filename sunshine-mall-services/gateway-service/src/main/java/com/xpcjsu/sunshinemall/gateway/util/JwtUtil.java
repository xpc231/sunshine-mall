package com.xpcjsu.sunshinemall.gateway.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类

 * 功能：
 * - Token生成
 * - Token解析
 * - Token验证
 *
 * @author sunshine-mall
 * @since 1.0.0
 */
@Slf4j
@Component
public class JwtUtil {

    /**
     * JWT 密钥
     */
    @Value("${jwt.secret:sunshine-mall-secret-key-2024}")
    private String secret;

    /**
     * Token 过期时间（默认2小时，单位：毫秒）
     */
    @Value("${jwt.expiration:7200000}")
    private Long expiration;

    /**
     * Token 前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * Token Header 名称
     */
    public static final String HEADER_NAME = "Authorization";

    /**
     * 生成 JWT Token
     *
     * @param userId 用户ID
     * @param username 用户名
     * @return JWT Token
     * @throws IllegalArgumentException 如果参数为空
     */
    public String generateToken(String userId, String username) {
        // 参数校验：防止空指针
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("userId不能为空");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("username不能为空");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        return generateToken(claims);
    }

    /**
     * 生成 JWT Token
     *
     * @param claims 自定义声明
     * @return JWT Token
     * @throws IllegalArgumentException 如果参数为空
     */
    public String generateToken(Map<String, Object> claims) {
        // 参数校验：防止空指针
        if (claims == null || claims.isEmpty()) {
            throw new IllegalArgumentException("claims不能为空");
        }

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()//构建器
                    .withIssuer("sunshine-mall")//发行者
                    .withIssuedAt(now)//签发时间
                    .withExpiresAt(expiryDate)//过期时间
                    .withClaim("claims", claims)//自定义声明
                    .sign(algorithm);//签名
        } catch (Exception e) {
            log.error("生成JWT Token失败", e);
            throw new RuntimeException("生成Token失败", e);
        }
    }

    /*
     * 验证 Token 有效性
     *
     * @param token JWT Token
     * @return true:有效, false:无效
     */
    /**
     * 验证 Token 有效性
     *
     * @param token JWT Token
     * @return true:有效, false:无效
     */
    public boolean validateToken(String token) {
        try {
            return verifyToken(token) != null;
        } catch (JWTVerificationException e) {
            log.warn("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解析 Token
     *
     * @param token JWT Token
     * @return DecodedJWT
     */
    public DecodedJWT parseToken(String token) {
        try {
            return verifyToken(token);
        } catch (JWTVerificationException e) {
            log.error("Token解析失败", e);
            throw new RuntimeException("Token解析失败", e);
        }
    }

    /**
     * 内部验证方法，用于复用验证逻辑
     *
     * @param token JWT Token
     * @return DecodedJWT 或 null（如果验证失败）
     */
    private DecodedJWT verifyToken(String token) {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer("sunshine-mall")
                .build();
        return verifier.verify(token);
    }

    /**
     * 从 Token 中获取用户ID
     *
     * @param token JWT Token
     * @return 用户ID
     * @throws RuntimeException 如果Token中缺少userId信息或为空
     */
    public String getUserIdFromToken(String token) {
        DecodedJWT decodedJWT = parseToken(token);

        // 安全获取claims Map：防止多层级空指针
        Map<String, Object> claimsMap = decodedJWT.getClaim("claims").asMap();
        if (claimsMap == null || !claimsMap.containsKey("userId")) {
            throw new RuntimeException("Token中缺少userId信息");
        }

        Object userId = claimsMap.get("userId");
        if (userId == null) {
            throw new RuntimeException("Token中userId为空");
        }

        return userId.toString();
    }

    /**
     * 从 Token 中获取用户名
     *
     * @param token JWT Token
     * @return 用户名
     * @throws RuntimeException 如果Token中缺少username信息或为空
     */
    public String getUsernameFromToken(String token) {
        DecodedJWT decodedJWT = parseToken(token);

        // 安全获取claims Map：防止多层级空指针
        Map<String, Object> claimsMap = decodedJWT.getClaim("claims").asMap();
        if (claimsMap == null || !claimsMap.containsKey("username")) {
            throw new RuntimeException("Token中缺少username信息");
        }

        Object username = claimsMap.get("username");
        if (username == null) {
            throw new RuntimeException("Token中username为空");
        }

        return username.toString();
    }

    /**
     * 检查 Token 是否过期
     *
     * @param token JWT Token
     * @return true:已过期, false:未过期
     */
    public boolean isTokenExpired(String token) {
        try {
            DecodedJWT decodedJWT = parseToken(token);
            Date expiresAt = decodedJWT.getExpiresAt();

            // 空指针检查：如果Token没有设置过期时间
            if (expiresAt == null) {
                log.warn("Token没有设置过期时间，视为未过期");
                return false;
            }

            return expiresAt.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 从请求头中提取 Token
     *
     * @param authHeader Authorization 请求头
     * @return Token（去除前缀）
     */
    public String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith(TOKEN_PREFIX)) {
            return authHeader.substring(TOKEN_PREFIX.length());
        }
        return null;
    }
}
