package com.xpcjsu.sunshinemall.framework.auth.core;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.xpcjsu.sunshinemall.framework.auth.config.JwtProperties;
import com.xpcjsu.sunshinemall.framework.base.exception.SystemException;
import com.xpcjsu.sunshinemall.framework.cache.core.CacheManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 *
 * @author sunshine-mall
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;
    private final CacheManager cacheManager;

    public static final String HEADER_NAME = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    // JWT Claims 常量
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_USERNAME = "username";

    /**
     * 生成 JWT Token
     *
     * @param userId   用户ID
     * @param username 用户名
     * @return JWT Token
     */
    public String generateToken(String userId, String username) {
        return generateToken(userId, username, null);
    }

    /**
     * 生成 JWT Token（带额外声明）
     *
     * @param userId      用户ID
     * @param username    用户名
     * @param extraClaims 额外声明
     * @return JWT Token
     */
    public String generateToken(String userId, String username, Map<String, Object> extraClaims) {
        try {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + jwtProperties.getExpiration());

            Map<String, Object> claims = new HashMap<>();
            claims.put(CLAIM_USER_ID, userId);
            claims.put(CLAIM_USERNAME, username);
            
            // 添加额外声明
            if (extraClaims != null && !extraClaims.isEmpty()) {
                claims.putAll(extraClaims);
            }

            return JWT.create()
                    .withIssuer(jwtProperties.getIssuer())
                    .withIssuedAt(now)
                    .withExpiresAt(expiryDate)
                    .withClaim("claims", claims)
                    .sign(Algorithm.HMAC256(jwtProperties.getSecret()));
        } catch (Exception e) {
            log.error("生成JWT Token失败 - userId: {}, username: {}", userId, username, e);
            throw new SystemException("TOKEN_GENERATION_FAILED", "生成Token失败", e);
        }
    }

    /**
     * 解析 JWT Token
     *
     * @param token JWT Token
     * @return 解码后的JWT
     */
    public DecodedJWT parseToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(jwtProperties.getSecret()))
                    .withIssuer(jwtProperties.getIssuer())
                    .build();
            return verifier.verify(token);
        } catch (JWTVerificationException e) {
            log.warn("JWT Token验证失败: {}", e.getMessage());
            throw new SystemException("TOKEN_INVALID", "Token无效或已过期", e);
        }
    }

    /**
     * 验证 Token 是否有效
     *
     * @param token JWT Token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            // 检查Token是否在黑名单中
            if (isTokenBlacklisted(token)) {
                log.warn("Token已在黑名单中: {}", token);
                return false;
            }

            // 验证Token格式和签名
            parseToken(token);
            return true;
        } catch (Exception e) {
            log.warn("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从Token中获取用户ID
     *
     * @param decodedJWT 解码后的JWT
     * @return 用户ID
     */
    public String getUserIdFromToken(DecodedJWT decodedJWT) {
        Map<String, Object> claims = decodedJWT.getClaim("claims").asMap();
        return (String) claims.get(CLAIM_USER_ID);
    }

    /**
     * 从Token中获取用户名
     *
     * @param decodedJWT 解码后的JWT
     * @return 用户名
     */
    public String getUsernameFromToken(DecodedJWT decodedJWT) {
        Map<String, Object> claims = decodedJWT.getClaim("claims").asMap();
        return (String) claims.get(CLAIM_USERNAME);
    }

    /**
     * 从Token中获取自定义声明
     *
     * @param decodedJWT 解码后的JWT
     * @param claimName  声明名称
     * @return 声明值
     */
    public Object getClaimFromToken(DecodedJWT decodedJWT, String claimName) {
        Map<String, Object> claims = decodedJWT.getClaim("claims").asMap();
        return claims.get(claimName);
    }

    /**
     * 从HTTP Header中提取Token
     *
     * @param authHeader Authorization Header值
     * @return JWT Token（去除前缀）
     */
    public String extractToken(String authHeader) {
        if (!StringUtils.hasText(authHeader)) {
            return null;
        }

        if (authHeader.startsWith(jwtProperties.getTokenPrefix())) {
            return authHeader.substring(jwtProperties.getTokenPrefix().length());
        }

        // 兼容不带前缀的情况
        return authHeader;
    }

    /**
     * 将Token加入黑名单
     *
     * @param token JWT Token
     */
    public void blacklistToken(String token) {
        String blacklistKey = jwtProperties.getBlacklistPrefix() + token;
        // 设置过期时间为Token的剩余有效期
        long expireSeconds = jwtProperties.getExpiration() / 1000;
        cacheManager.set(blacklistKey, "1", expireSeconds);
        log.info("Token已加入黑名单: {}", token);
    }

    /**
     * 检查Token是否在黑名单中
     *
     * @param token JWT Token
     * @return 是否在黑名单中
     */
    public boolean isTokenBlacklisted(String token) {
        String blacklistKey = jwtProperties.getBlacklistPrefix() + token;
        return cacheManager.get(blacklistKey) != null;
    }

    /**
     * 获取Token过期时间
     *
     * @param decodedJWT 解码后的JWT
     * @return 过期时间
     */
    public Date getExpirationFromToken(DecodedJWT decodedJWT) {
        return decodedJWT.getExpiresAt();
    }

    /**
     * 检查Token是否即将过期（剩余时间少于1小时）
     *
     * @param decodedJWT 解码后的JWT
     * @return 是否即将过期
     */
    public boolean isTokenExpiringSoon(DecodedJWT decodedJWT) {
        Date expiration = getExpirationFromToken(decodedJWT);
        Date now = new Date();
        long remainingTime = expiration.getTime() - now.getTime();
        return remainingTime < 60 * 60 * 1000; // 1小时
    }
}