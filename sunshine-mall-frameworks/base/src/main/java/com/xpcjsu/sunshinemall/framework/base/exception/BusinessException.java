package com.xpcjsu.sunshinemall.framework.base.exception;

/**
 * 业务异常
 * <p>
 * 用于表示业务逻辑相关的异常，如业务规则违反、状态不匹配等。
 * 通常由用户操作或业务流程引起，需要向用户提供友好的错误提示。
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
public class BusinessException extends BaseException {

    /**
     * 构造函数
     * 
     * @param message 用户友好的错误消息
     */
    public BusinessException(String message) {
        super("BUSINESS_ERROR", message);
    }

    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     * @param message   用户友好的错误消息
     */
    public BusinessException(String errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 构造函数
     * 
     * @param message 用户友好的错误消息
     * @param cause   原因异常
     */
    public BusinessException(String message, Throwable cause) {
        super("BUSINESS_ERROR", message, cause);
    }

    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     * @param message   用户友好的错误消息
     * @param cause     原因异常
     */
    public BusinessException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     * @param message   用户友好的错误消息
     * @param details   技术详情
     */
    public BusinessException(String errorCode, String message, String details) {
        super(errorCode, message, details);
    }
}