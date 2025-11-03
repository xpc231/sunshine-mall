package com.xpcjsu.sunshinemall.framework.base.config;

import com.xpcjsu.sunshinemall.framework.base.exception.SystemException;

/**
 * 配置异常
 * <p>
 * 用于封装配置读取、解析和验证过程中的异常
 *
 * 问题快速定位：包含具体的配置键信息，能快速定位是哪个配置项出现问题
 * 监控告警支持：可以基于 ConfigException 进行专门的配置错误监控和告警
 * 异常分类管理：区别于通用的 RuntimeException，便于统一处理配置相关异常
 * 上下文信息丰富：提供配置键、错误信息、原因异常等完整上下文
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