package com.xpcjsu.sunshinemall.framework.base.config;

/**
 * 配置异常
 * <p>
 * 用于封装配置读取、解析和验证过程中的异常
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
public class ConfigException extends RuntimeException {

    /**
     * 配置键
     */
    private final String configKey;

    /**
     * 构造函数
     * 
     * @param message 异常信息
     */
    public ConfigException(String message) {
        super(message);
        this.configKey = null;
    }

    /**
     * 构造函数
     * 
     * @param message   异常信息
     * @param configKey 配置键
     */
    public ConfigException(String message, String configKey) {
        super(message);
        this.configKey = configKey;
    }

    /**
     * 构造函数
     * 
     * @param message 异常信息
     * @param cause   原因异常
     */
    public ConfigException(String message, Throwable cause) {
        super(message, cause);
        this.configKey = null;
    }

    /**
     * 构造函数
     * 
     * @param message   异常信息
     * @param configKey 配置键
     * @param cause     原因异常
     */
    public ConfigException(String message, String configKey, Throwable cause) {
        super(message, cause);
        this.configKey = configKey;
    }

    /**
     * 获取配置键
     * 
     * @return 配置键
     */
    public String getConfigKey() {
        return configKey;
    }

    @Override
    public String getMessage() {
        if (configKey != null) {
            return String.format("[Config Key: %s] %s", configKey, super.getMessage());
        }
        return super.getMessage();
    }
}