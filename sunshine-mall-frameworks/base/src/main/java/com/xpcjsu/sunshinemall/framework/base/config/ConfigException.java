package com.xpcjsu.sunshinemall.framework.base.config;

import com.xpcjsu.sunshinemall.framework.base.exception.SystemException;

/**
 * 配置异常
 * <p>
 * 用于封装配置读取、解析和验证过程中的异常
 *
 */
public class ConfigException extends SystemException {

    /**
     * 配置键
     */
    private final String configKey;

    /**
     * 构造函数
     */
    public ConfigException(String message) {
        super("CONFIG_ERROR", message);
        this.configKey = null;
    }

    /**
     * 构造函数
     */
    public ConfigException(String message, String configKey) {
        super("CONFIG_ERROR", message);
        this.configKey = configKey;
        if (configKey != null) {
            addContext("configKey", configKey);
        }
    }

    /**
     * 构造函数
     */
    public ConfigException(String message, Throwable cause) {
        super("CONFIG_ERROR", message, cause);
        this.configKey = null;
    }

    /**
     * 构造函数
     */
    public ConfigException(String message, String configKey, Throwable cause) {
        super("CONFIG_ERROR", message, cause);
        this.configKey = configKey;
        if (configKey != null) {
            addContext("configKey", configKey);
        }
    }

    /**
     * 获取配置键
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