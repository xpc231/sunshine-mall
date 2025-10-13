package com.xpcjsu.sunshinemall.framework.base.exception;

/**
 * 系统异常
 * <p>
 * 用于表示系统级别的异常，如配置错误、网络异常、依赖服务异常等。
 * 通常由系统环境或外部依赖问题引起，需要技术人员介入处理。
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
public class SystemException extends BaseException {

    /**
     * 构造函数
     * 
     * @param message 用户友好的错误消息
     */
    public SystemException(String message) {
        super("SYSTEM_ERROR", message);
    }

    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     * @param message   用户友好的错误消息
     */
    public SystemException(String errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 构造函数
     * 
     * @param message 用户友好的错误消息
     * @param cause   原因异常
     */
    public SystemException(String message, Throwable cause) {
        super("SYSTEM_ERROR", message, cause);
    }

    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     * @param message   用户友好的错误消息
     * @param cause     原因异常
     */
    public SystemException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     * @param message   用户友好的错误消息
     * @param details   技术详情
     */
    public SystemException(String errorCode, String message, String details) {
        super(errorCode, message, details);
    }
}