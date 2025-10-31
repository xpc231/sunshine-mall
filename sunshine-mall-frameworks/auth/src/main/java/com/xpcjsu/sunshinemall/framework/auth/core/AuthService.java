package com.xpcjsu.sunshinemall.framework.auth.core;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.xpcjsu.sunshinemall.framework.auth.config.JwtProperties;
import com.xpcjsu.sunshinemall.framework.base.exception.BusinessException;
import com.xpcjsu.sunshinemall.framework.convention.errorcode.BusinessErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 认证服务
 *
 * @author sunshine-mall
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    /**
     * 从HTTP请求中提取并验证用户认证信息
     *
     * @param request HTTP请求
     * @return 认证上下文
     */
    public AuthContext extractAuthContext(HttpServletRequest request) {
        String authHeader = request.getHeader(jwtProperties.getHeaderName());
        String token = jwtUtil.extractToken(authHeader);
        
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGIN, "未携带有效Token");
        }

        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGIN, "Token无效或已过期");
        }

        DecodedJWT decodedJWT = jwtUtil.parseToken(token);
        String userId = jwtUtil.getUserIdFromToken(decodedJWT);
        String username = jwtUtil.getUsernameFromToken(decodedJWT);

        return new AuthContext(Long.valueOf(userId), username, token);
    }

    /**
     * 从HTTP请求中提取用户ID
     *
     * @param request HTTP请求
     * @return 用户ID
     */
    public Long extractUserId(HttpServletRequest request) {
        AuthContext context = extractAuthContext(request);
        return context.getUserId();
    }

    /**
     * 从HTTP请求中提取用户名
     *
     * @param request HTTP请求
     * @return 用户名
     */
    public String extractUsername(HttpServletRequest request) {
        AuthContext context = extractAuthContext(request);
        return context.getUsername();
    }

    /**
     * 生成Token
     *
     * @param userId   用户ID
     * @param username 用户名
     * @return JWT Token
     */
    public String generateToken(Long userId, String username) {
        return jwtUtil.generateToken(userId.toString(), username);
    }

    /**
     * 登出（将Token加入黑名单）
     *
     * @param token JWT Token
     */
    public void logout(String token) {
        if (StringUtils.hasText(token)) {
            jwtUtil.blacklistToken(token);
            log.info("用户登出成功，Token已加入黑名单");
        }
    }

    /**
     * 从HTTP请求中登出
     *
     * @param request HTTP请求
     */
    public void logout(HttpServletRequest request) {
        String authHeader = request.getHeader(jwtProperties.getHeaderName());
        String token = jwtUtil.extractToken(authHeader);
        logout(token);
    }

    /**
     * 验证Token是否有效
     *
     * @param token JWT Token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

    /**
     * 检查Token是否即将过期
     *
     * @param token JWT Token
     * @return 是否即将过期
     */
    public boolean isTokenExpiringSoon(String token) {
        try {
            DecodedJWT decodedJWT = jwtUtil.parseToken(token);
            return jwtUtil.isTokenExpiringSoon(decodedJWT);
        } catch (Exception e) {
            return true; // Token无效时认为需要刷新
        }
    }
}