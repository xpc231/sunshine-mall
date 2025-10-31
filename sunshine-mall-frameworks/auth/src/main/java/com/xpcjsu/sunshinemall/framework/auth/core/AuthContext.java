package com.xpcjsu.sunshinemall.framework.auth.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证上下文
 * 用于在当前线程中存储用户认证信息
 *
 * @author sunshine-mall
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthContext {

    private static final ThreadLocal<AuthContext> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * JWT Token
     */
    private String token;

    /**
     * 设置当前认证上下文
     *
     * @param context 认证上下文
     */
    public static void setContext(AuthContext context) {
        CONTEXT_HOLDER.set(context);
    }

    /**
     * 获取当前认证上下文
     *
     * @return 认证上下文
     */
    public static AuthContext getContext() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 获取当前用户ID
     *
     * @return 用户ID
     */
    public static Long getCurrentUserId() {
        AuthContext context = getContext();
        return context != null ? context.getUserId() : null;
    }

    /**
     * 获取当前用户名
     *
     * @return 用户名
     */
    public static String getCurrentUsername() {
        AuthContext context = getContext();
        return context != null ? context.getUsername() : null;
    }

    /**
     * 获取当前Token
     *
     * @return JWT Token
     */
    public static String getCurrentToken() {
        AuthContext context = getContext();
        return context != null ? context.getToken() : null;
    }

    /**
     * 清除当前认证上下文
     */
    public static void clearContext() {
        CONTEXT_HOLDER.remove();
    }

    /**
     * 检查是否已认证
     *
     * @return 是否已认证
     */
    public static boolean isAuthenticated() {
        AuthContext context = getContext();
        return context != null && context.getUserId() != null;
    }
}