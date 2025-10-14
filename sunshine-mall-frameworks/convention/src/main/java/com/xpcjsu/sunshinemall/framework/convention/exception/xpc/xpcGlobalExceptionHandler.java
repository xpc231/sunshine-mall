package com.xpcjsu.sunshinemall.framework.convention.exception.xpc;
//该类与GlobalExceptionHandler内容一样

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

@Slf4j
@RestControllerAdvice
public class xpcGlobalExceptionHandler {

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(ValidationException ex, HttpServletRequest request) {
        String errorCode = ex.getErrorCode() != null ? ex.getErrorCode() : BusinessErrorCode.SYSTEM_ERROR;

        log.warn("参数校验异常 - 请求路径: {}, 错误码: {}, 消息: {}, 上下文: {}",
                request.getRequestURI(), errorCode, ex.getMessage(), ex.getContext());

        return Result.failure(errorCode,ex.getMessage());
    }


    @ExceptionHandler
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        String errorCode = ex.getErrorCode() != null ? ex.getErrorCode() : BusinessErrorCode.SYSTEM_ERROR;

        log.warn("业务异常 - 错误码: {}, 错误消息: {}, 请求路径: {}, 上下文: {}",
                errorCode, ex.getMessage(), request.getRequestURI(), ex.getContext());

        return Result.failure(errorCode, ex.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleSystemException(SystemException ex, HttpServletRequest request) {
        String errorCode = ex.getErrorCode() != null ? ex.getErrorCode() : BusinessErrorCode.SYSTEM_ERROR;

        log.error("系统异常 - 错误码: {}, 错误消息: {}, 请求路径: {}, 详情: {}, 上下文: {}",
                errorCode, ex.getMessage(), request.getRequestURI(), ex.getDetails(), ex.getContext(), ex);

        return Result.failure(errorCode, ex.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message =  ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ":" + error.getDefaultMessage())
                .collect((Collectors.joining("; ")));

        log.warn("参数校验失败 - 错误码: {}, 校验错误: {}, 请求路径: {}",
                BusinessErrorCode.SYSTEM_PARAM_ERROR, message, request.getRequestURI());

        return Result.failure(BusinessErrorCode.SYSTEM_ERROR, message);
    }

    public Result<Void> handleException(Exception ex, HttpServletRequest request) {

        log.error("未知异常 - 错误码: {}, 异常类型: {}, 错误消息: {}, 请求路径: {}",
                BusinessErrorCode.SYSTEM_ERROR, ex.getClass().getName(), ex.getMessage(), request.getRequestURI());

        return Result.failure(BusinessErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后重试");
    }


}
