package com.xpcjsu.sunshinemall.gateway.exception;

import com.xpcjsu.sunshinemall.framework.base.exception.BaseException;

/**
 * 限流异常
 * <p>
 * 当请求超过限流阈值时抛出此异常
 *
 * @author sunshine-mall
 * @since 1.0.0
 */
public class RateLimitException extends BaseException {

    public RateLimitException(String message) {
        super("RATE_LIMIT_EXCEEDED", message);
    }

    public RateLimitException(String message, Throwable cause) {
        super("RATE_LIMIT_EXCEEDED", message, cause);
    }
}

