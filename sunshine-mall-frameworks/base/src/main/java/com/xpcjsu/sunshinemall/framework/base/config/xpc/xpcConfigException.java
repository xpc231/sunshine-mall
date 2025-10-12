package com.xpcjsu.sunshinemall.framework.base.config.xpc;

//该类与ConfigException内容一样，用来重敲练习
public class xpcConfigException extends RuntimeException {

    private final String configKey;

    public xpcConfigException(String message) {
        super(message);
        this.configKey = null;
    }

    public xpcConfigException(String message, String configKey) {
        super(message);
        this.configKey = configKey;
    }

    public xpcConfigException(String message, Throwable cause) {
        super(message, cause);
        this.configKey = null;
    }

    public xpcConfigException(String message, String configKey, Throwable cause) {
        super(message, cause);
        this.configKey = configKey;
    }

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
