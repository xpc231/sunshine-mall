package com.xpcjsu.sunshinemall.framework.base.config.xpc;

import com.xpcjsu.sunshinemall.framework.base.context.ApplicationContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//该类内容与ConfigManager一样，只是用来重敲练习
public class xpcConfigManager {

    //配置缓存
    private final Map<String, Object> configCache = new ConcurrentHashMap<>();

    //Spring环境对象
    private Environment environment;

    //私有化构造器，防止外部实例化
    private xpcConfigManager(){
        //初始化Spring Environment
        initializeEnvironment();
    }

    //获取指定键的字符串配置值
    private String getString(String key, String defaultValue) {
        validateKey(key);
        return (String) configCache.computeIfAbsent(key, k -> {
            String value = findConfigValue(k);
            if(StringUtils.hasText(value)) {
                return value;
            }
            return defaultValue;
        });
    }

    //获取指定键的整数配置值
    public Integer getInt(String key, Integer defaultValue) {
        validateKey(key);
        return (Integer) configCache.computeIfAbsent(key + "int", k -> {
            String value = findConfigValue(k);
            try {
                if(StringUtils.hasText(value)){
                    return Integer.valueOf(value);
                }
            } catch (NumberFormatException e) {
                throw new xpcConfigException("配置值格式错误，期望整数类型", key, e);
            }
            return defaultValue;
        });
    }

    //获取指定键的Long配置值
    public Long getLong(String key, Long defaultValue) {
        validateKey(key);
        return (Long) configCache.computeIfAbsent(key + ":long", k -> {
            String value = findConfigValue(key);
            // findConfigValue已统一处理trim，这里直接使用
            if (StringUtils.hasText(value)) {
                try {
                    return Long.valueOf(value);
                } catch (NumberFormatException e) {
                    throw new xpcConfigException("配置值格式错误，期望长整数类型", key, e);
                }
            }
            return defaultValue;
        });
    }

    //获取指定键的布尔配置值
    public Boolean getBoolean(String key, Boolean defaultValue) {
        validateKey(key);
        return (Boolean) configCache.computeIfAbsent(key + ":boolean", k -> {
            String value = findConfigValue(key);
            if(StringUtils.hasText(value)) {
                String lowerValue = value.toLowerCase();
                return "true".equals(lowerValue) || "1".equals(lowerValue) || "yes".equals(lowerValue);
            }
            return defaultValue;
        });
    }

    //获取指定键的Double配置值
    public Double getDouble(String key, Double defaultValue) {
        validateKey(key);
        return (Double) configCache.computeIfAbsent(key + ":double", k -> {
            String value = findConfigValue(key);
            // findConfigValue已统一处理trim，这里直接使用
            if (StringUtils.hasText(value)) {
                try {
                    return Double.valueOf(value);
                } catch (NumberFormatException e) {
                    throw new xpcConfigException("配置值格式错误，期望双精度浮点类型", key, e);
                }
            }
            return defaultValue;
        });
    }

    //判断指定键的配置是否存在
    public boolean contains(String key) {
        validateKey( key);
        return configCache.containsKey(key);
    }

    private void logConversionError(String key, String value, String targetType, Exception e) {
        System.err.printf("Failed to convert config [%s=%s] to %s: %s%n",
                key, value, targetType, e.getMessage());
    }


    //查找配置值
    private String findConfigValue(String key) {
        //1.系统属性
        String value = System.getProperty(key);
        if(StringUtils.hasText(value)) {
            return value.trim();
        }

        //2.环境变量（将点号转换为下划线）
        String envKey = key.replace('.', '_').toUpperCase();
        value = System.getenv(envKey);
        if(StringUtils.hasText(value)) {
            return value.trim();
        }

        //3.Spring配置文件（如果可用）
        if(environment != null) {
            value = environment.getProperty(key);
            if(StringUtils.hasText(value)) {
                return value.trim();
            }
        }

        return null;
    }

    //验证配置键
    private void validateKey(String key) {
        if(key == null || key.trim().isEmpty()) {
            throw new xpcConfigException("配置键不能为null或空字符串", key);
        }
    }


    //初始化Spring Environment
    private void initializeEnvironment() {
        try {
            if(ApplicationContextHolder.isApplicationContextInitialized()) {
                this.environment = ApplicationContextHolder.getBeanOptional(Environment.class)
                        .orElse(null);
            }
        } catch (Exception e) {
            // 忽略异常，使用系统属性和环境变量作为fallback
        }
    }
}
