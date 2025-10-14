package com.xpcjsu.sunshinemall.framework.convention.exception;

import com.xpcjsu.sunshinemall.framework.base.exception.BusinessException;
import com.xpcjsu.sunshinemall.framework.base.exception.SystemException;
import com.xpcjsu.sunshinemall.framework.base.exception.ValidationException;
import com.xpcjsu.sunshinemall.framework.convention.errorcode.BusinessErrorCode;
import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 统一处理电商业务异常，提供标准化的错误响应。
 * <p>
 * 处理的异常类型：
 * <ul>
 * <li>BusinessException：业务异常（返回200）</li>
 * <li>ValidationException：参数校验异常（返回400）</li>
 * <li>SystemException：系统异常（返回500）</li>
 * <li>MethodArgumentNotValidException：Spring Validation异常（返回400）</li>
 * <li>Exception：未知异常兜底（返回500）</li>
 * </ul>
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        String errorCode = ex.getErrorCode() != null ? ex.getErrorCode() : BusinessErrorCode.SYSTEM_ERROR;
        log.warn("业务异常 - 错误码: {}, 错误消息: {}, 请求路径: {}",
                errorCode, ex.getMessage(), request.getRequestURI());
        return Result.failure(errorCode, ex.getMessage());
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(ValidationException ex, HttpServletRequest request) {
        String errorCode = ex.getErrorCode() != null ? ex.getErrorCode() : BusinessErrorCode.SYSTEM_PARAM_ERROR;
        log.warn("参数校验异常 - 错误码: {}, 错误消息: {}, 请求路径: {}",
                errorCode, ex.getMessage(), request.getRequestURI());
        return Result.failure(errorCode, ex.getMessage());
    }

    /**
     * 处理系统异常
     */
    @ExceptionHandler(SystemException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleSystemException(SystemException ex, HttpServletRequest request) {
        String errorCode = ex.getErrorCode() != null ? ex.getErrorCode() : BusinessErrorCode.SYSTEM_ERROR;
        log.error("系统异常 - 错误码: {}, 错误消息: {}, 请求路径: {}",
                errorCode, ex.getMessage(), request.getRequestURI(), ex);
        return Result.failure(errorCode, ex.getMessage());
    }

    /**
     * 处理Spring Validation参数校验异常

     * getBindingResult():获取Spring Validation校验失败的详细信息
     * getFieldErrors() 提取所有字段错误
     * getField(): 获取校验失败的字段名称
     * getDefaultMessage(): 获取该字段的默认错误信息

     * 例如校验失败的字段有：
     * username: "不能为空"
     * email: "邮箱格式不正确"
     * 转换后变成："username: 不能为空; email: 邮箱格式不正确"
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        // 获取参数校验错误信息
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));//将所有错误信息字符串用;连接

        log.warn("参数校验失败 - 错误码: {}, 校验错误: {}, 请求路径: {}",
                BusinessErrorCode.SYSTEM_PARAM_ERROR, message, request.getRequestURI());

        return Result.failure(BusinessErrorCode.SYSTEM_PARAM_ERROR, message);
    }



    /**
     * 处理未知异常（兜底）
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception ex, HttpServletRequest request) {
        log.error("未知异常 - 错误码: {}, 异常类型: {}, 错误消息: {}, 请求路径: {}",
                BusinessErrorCode.SYSTEM_ERROR, ex.getClass().getName(), ex.getMessage(), request.getRequestURI(), ex);
        return Result.failure(BusinessErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后重试");
    }
}
