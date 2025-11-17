package com.xpcjsu.sunshinemall.user.utils;

import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTValidator;
import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;
import com.xpcjsu.sunshinemall.framework.base.exception.UnauthorizedException;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.time.Duration;
import java.util.Date;

@Component
public class JwtTool {

    // JWT 签名
    private final JWTSigner jwtSigner;

    // 使用提供的密钥对创建JWT工具实例
    public JwtTool(KeyPair keyPair) {
        this.jwtSigner = JWTSignerUtil.createSigner("rs256", keyPair);
    }

    // 创建访问令牌
    public String createAccessToken(Long userId, Duration ttl) {
        return JWT.create()
                .setPayload("user", userId)
                .setPayload("type", "access")
                .setExpiresAt(new Date(System.currentTimeMillis() + ttl.toMillis()))
                .setSigner(jwtSigner)
                .sign();
    }

    // 创建刷新令牌
    public String createRefreshToken(Long userId, Duration ttl) {
        return JWT.create()
                .setPayload("user", userId)
                .setPayload("type", "refresh")
                .setExpiresAt(new Date(System.currentTimeMillis() + ttl.toMillis()))
                .setSigner(jwtSigner)
                .sign();
    }


    // 解析访问令牌
    public Long parseAccessToken(String token) {
        JWT jwt = parseAndValidate(token);
        Object type = jwt.getPayload("type");
        if (type != null && !"access".equals(type.toString())) {
            throw new UnauthorizedException("无效的token");
        }
        Object userPayload = jwt.getPayload("user");
        if (userPayload == null) {
            throw new UnauthorizedException("无效的token");
        }
        try {
            return Long.valueOf(userPayload.toString());
        } catch (RuntimeException e) {
            throw new UnauthorizedException("无效的token");
        }
    }

    // 解析刷新令牌
    public Long parseRefreshToken(String token) {
        JWT jwt = parseAndValidate(token);
        Object type = jwt.getPayload("type");
        if (type == null || !"refresh".equals(type.toString())) {
            throw new UnauthorizedException("无效的token");
        }
        Object userPayload = jwt.getPayload("user");
        if (userPayload == null) {
            throw new UnauthorizedException("无效的token");
        }
        try {
            return Long.valueOf(userPayload.toString());
        } catch (RuntimeException e) {
            throw new UnauthorizedException("无效的token");
        }
    }

    // 解析并验证令牌
    private JWT parseAndValidate(String token) {
        if (token == null) {
            throw new UnauthorizedException("未登录");
        }
        JWT jwt;
        try {
            jwt = JWT.of(token).setSigner(jwtSigner);
        } catch (Exception e) {
            throw new UnauthorizedException("无效的token", e);
        }
        if (!jwt.verify()) {
            throw new UnauthorizedException("无效的token");
        }
        try {
            JWTValidator.of(jwt).validateDate();
        } catch (ValidateException e) {
            throw new UnauthorizedException("token已经过期");
        }
        return jwt;
    }
}