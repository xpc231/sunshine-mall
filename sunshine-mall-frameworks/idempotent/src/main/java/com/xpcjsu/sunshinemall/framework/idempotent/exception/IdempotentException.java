package com.xpcjsu.sunshinemall.framework.idempotent.exception;

/**
 * 幂等性异常
 * <p>
 * 当检测到重复提交时抛出此异常
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
public class IdempotentException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public IdempotentException(String message) {
        super(message);
    }

    public IdempotentException(String message, Throwable cause) {
        super(message, cause);
    }
}
