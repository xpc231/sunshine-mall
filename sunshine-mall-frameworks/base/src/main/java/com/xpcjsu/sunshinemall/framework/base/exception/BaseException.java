package com.xpcjsu.sunshinemall.framework.base.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * 基础异常类
 * <p>
 * 为整个框架提供统一的异常基类，支持错误码、上下文信息和异常链追踪。
 * 所有自定义异常都应该继承此类，以保证异常处理的一致性。
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */

public class BaseException extends RuntimeException {

    /**
     * 错误码
     */
    private final String errorCode;

    /**
     * 技术详情，用于日志记录和开发调试
     */
    private final String details;

    /**
     * 业务上下文数据，用于问题排查
     */
    private final Map<String, Object> context;

    /**
     * 构造函数
     * 
     * @param message 用户友好的错误消息
     */
    public BaseException(String message) {
        this(null, message, null, null, new HashMap<>());
    }

    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     * @param message   用户友好的错误消息
     */
    public BaseException(String errorCode, String message) {
        this(errorCode, message, null, null, new HashMap<>());
    }

    /**
     * 构造函数
     * 
     * @param message 用户友好的错误消息
     * @param cause   原因异常
     */
    public BaseException(String message, Throwable cause) {
        this(null, message, null, cause, new HashMap<>());
    }

    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     * @param message   用户友好的错误消息
     * @param cause     原因异常
     */
    public BaseException(String errorCode, String message, Throwable cause) {
        this(errorCode, message, null, cause, new HashMap<>());
    }

    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     * @param message   用户友好的错误消息
     * @param details   技术详情
     */
    public BaseException(String errorCode, String message, String details) {
        this(errorCode, message, details, null, new HashMap<>());
    }

    /**
     * 完整构造函数
     * 
     * @param errorCode 错误码
     * @param message   用户友好的错误消息
     * @param details   技术详情
     * @param cause     原因异常
     * @param context   业务上下文数据
     */
    public BaseException(String errorCode, String message, String details, Throwable cause, Map<String, Object> context) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = details;
        this.context = context != null ? new HashMap<>(context) : new HashMap<>();
    }

    /**
     * 获取错误码
     * 
     * @return 错误码
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * 获取技术详情
     * 
     * @return 技术详情
     */
    public String getDetails() {
        return details;
    }

    /**
     * 获取业务上下文数据
     * 
     * @return 业务上下文数据的副本
     */
    public Map<String, Object> getContext() {
        //返回context的一个副本，防止外部修改原始的业务上下文数据
        return new HashMap<>(context);
    }

    /**
     * 向异常实例中添加业务上下文信息
     * 
     * @param key   键
     * @param value 值
     * @return 当前异常实例，支持链式调用
     */
    public BaseException addContext(String key, Object value) {
        if (key != null) {
            this.context.put(key, value);
        }
        return this;
    }

    /**
     * 添加多个上下文信息
     * 
     * @param contextData 上下文数据
     * @return 当前异常实例，支持链式调用
     */
    public BaseException addContext(Map<String, Object> contextData) {
        if (contextData != null) {
            this.context.putAll(contextData);
        }
        return this;
    }

    /**
     * 获取异常的完整信息描述
     *
     * @return 包含错误码、消息、详情的完整描述
     */
    @Override
    public String toString() {
        //单线程环境下的字符串构建
        StringBuilder sb = new StringBuilder();
        //当前异常类的简单类名（不包含包名的类名），并将其追加到StringBuilder对象sb中
        sb.append(getClass().getSimpleName());
        
        if (errorCode != null) {
            sb.append(" [").append(errorCode).append("]");
        }
        
        if (getMessage() != null) {
            sb.append(": ").append(getMessage());
        }
        
        if (details != null) {
            sb.append(" (Details: ").append(details).append(")");
        }
        
        if (!context.isEmpty()) {
            sb.append(" (Context: ").append(context).append(")");
        }
        
        return sb.toString();
    }
}