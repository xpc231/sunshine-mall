package com.xpcjsu.sunshinemall.framework.convention.result;

import java.io.Serializable;

/**
 * 统一响应结果
 * <p>
 * 专注于电商业务API响应的核心需求，提供简洁、标准的响应格式。
 * 支持成功/失败状态、错误码、消息和数据载荷。
 * <p>
 * 注意：复杂响应处理建议使用成熟的框架：
 * <ul>
 * <li>分页响应：Spring Data 的 Page</li>
 * <li>文件响应：Spring 的 ResponseEntity</li>
 * <li>流式响应：Spring WebFlux 的 Flux/Mono</li>
 * </ul>
 * 
 * @param <T> 响应数据类型
 * @author sunshine-mall
 * @since 1.0.0
 */
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 成功状态码
     */
    public static final String SUCCESS_CODE = "0";

    /**
     * 成功消息
     */
    public static final String SUCCESS_MESSAGE = "操作成功";

    /**
     * 状态码
     */
    private String code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 私有构造器
     */
    private Result() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 创建成功响应（无数据）
     * 
     * @param <T> 数据类型
     * @return 成功响应
     */
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.code = SUCCESS_CODE;
        result.message = SUCCESS_MESSAGE;
        return result;
    }

    /**
     * 创建成功响应（带数据）
     * 
     * @param <T>  数据类型
     * @param data 响应数据
     * @return 成功响应
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.code = SUCCESS_CODE;
        result.message = SUCCESS_MESSAGE;
        result.data = data;
        return result;
    }

    /**
     * 创建成功响应（带数据和消息）
     * 
     * @param <T>     数据类型
     * @param data    响应数据
     * @param message 响应消息
     * @return 成功响应
     */
    public static <T> Result<T> success(T data, String message) {
        Result<T> result = new Result<>();
        result.code = SUCCESS_CODE;
        result.message = message;
        result.data = data;
        return result;
    }

    /**
     * 创建失败响应
     * 
     * @param <T>     数据类型
     * @param code    错误码
     * @param message 错误消息
     * @return 失败响应
     */
    public static <T> Result<T> failure(String code, String message) {
        Result<T> result = new Result<>();
        result.code = code;
        result.message = message;
        return result;
    }

    /**
     * 判断是否成功
     * 
     * @return 如果成功返回true，否则返回false
     */
    public boolean isSuccess() {
        return SUCCESS_CODE.equals(this.code);
    }

    /**
     * 判断是否失败
     * 
     * @return 如果失败返回true，否则返回false
     */
    public boolean isFailure() {
        return !isSuccess();
    }

    // Getter 和 Setter 方法

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Result{" +
                "code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", timestamp=" + timestamp +
                '}';
    }
}