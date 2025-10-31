package com.xpcjsu.sunshinemall.framework.auth.annotation;

import java.lang.annotation.*;

/**
 * 需要认证注解
 * 标记在Controller方法或类上，表示需要JWT认证
 *
 * @author sunshine-mall
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireAuth {

    /**
     * 是否必须认证
     * 默认为true，表示必须认证
     * 设置为false时，如果有Token则解析，没有Token则跳过
     */
    boolean required() default true;

    /**
     * 认证失败时的错误消息
     */
    String message() default "需要登录后访问";
}