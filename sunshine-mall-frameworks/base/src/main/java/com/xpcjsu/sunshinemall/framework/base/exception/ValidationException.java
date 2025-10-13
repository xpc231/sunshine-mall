package com.xpcjsu.sunshinemall.framework.base.exception;

/**
 * 参数校验异常
 * <p>
 * 用于表示参数校验失败的异常，如参数为空、格式错误、取值范围不正确等。
 * 通常在接口入参校验、配置参数验证等场景中使用。
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
public class ValidationException extends BaseException {

    /**
     * 构造函数
     * 
     * @param message 用户友好的错误消息
     */
    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
    }

    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     * @param message   用户友好的错误消息
     */
    public ValidationException(String errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 构造函数
     * 
     * @param message 用户友好的错误消息
     * @param cause   原因异常
     */
    public ValidationException(String message, Throwable cause) {
        super("VALIDATION_ERROR", message, cause);
    }

    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     * @param message   用户友好的错误消息
     * @param cause     原因异常
     */
    public ValidationException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     * @param message   用户友好的错误消息
     * @param details   技术详情
     */
    public ValidationException(String errorCode, String message, String details) {
        super(errorCode, message, details);
    }
}