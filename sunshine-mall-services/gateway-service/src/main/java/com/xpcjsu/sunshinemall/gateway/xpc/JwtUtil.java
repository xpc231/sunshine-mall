package com.xpcjsu.sunshinemall.gateway.xpc;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtUtil {

    private final ServiceInstance serviceInstance;
    @Value("&{jwt.secret:sunshine-mall-secret-key-2024}")
    private String secret;

    @Value("7200000")
    private Long expiration;

    public static final String TOKEN_PREFIX = "Bearer";

    public static final String HEADER_NAME = "Authorization";

    public JwtUtil(ServiceInstance serviceInstance) {
        this.serviceInstance = serviceInstance;
    }


    public String generateToken(String userId, String userName) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("userName", userName);
        return generateToken(claims);
    }

    private String generateToken(Map<String, Object> claims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("sunshine-mall")
                    .withIssuedAt(now)
                    .withExpiresAt(expiryDate)
                    .withClaim("claims", claims)
                    .sign(algorithm);
        } catch (Exception e) {
            log.error("生成JWT Token失败", e);
            throw new RuntimeException("生成Token失败", e);
        }
    }


    public boolean validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer("sunshine-mall")
                    .build();
            verifier.verify(token);
            return true;
        } catch (JWTVerificationException e) {
            log.warn("Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    //DecodedJWT接口:解析验证后的token
    public DecodedJWT parseToken(String token) {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer("sunshine-mall")
                .build();
                return verifier.verify(token);
    }

    public String getUserIdFromToken(String token) {
        DecodedJWT decodedJWT = parseToken(token);
        return decodedJWT.getClaim("claims").asMap().get("userId").toString();
    }

    public String getUsernameFromToken(String token) {
        DecodedJWT decodedJWT = parseToken(token);
        return decodedJWT.getClaim("claims").asMap().get("userName").toString();
    }

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

    public String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith(TOKEN_PREFIX)) {
            return authHeader.substring(TOKEN_PREFIX.length());
        }
        return null;
    }
}
