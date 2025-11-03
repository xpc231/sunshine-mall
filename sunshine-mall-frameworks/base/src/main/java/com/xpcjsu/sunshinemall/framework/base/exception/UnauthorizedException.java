package com.xpcjsu.sunshinemall.framework.base.exception;

/**
 * 未授权异常（HTTP 401）
 */
public class UnauthorizedException extends BaseException {

    private static final String UNAUTHORIZED_CODE = "401";

    public UnauthorizedException(String message) {
        super(UNAUTHORIZED_CODE, message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(UNAUTHORIZED_CODE, message, cause);
    }

    public UnauthorizedException(Throwable cause) {
        // 使用完整构造函数传递cause，并以cause的消息作为友好提示（为空则使用默认文案）
        super(UNAUTHORIZED_CODE, cause != null ? cause.getMessage() : "未授权", null, cause, null);
    }
}
