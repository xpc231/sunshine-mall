package com.xpcjsu.sunshinemall.framework.base.config;

import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 配置管理器
 * <p>
 * 提供统一的配置读取能力，支持多种配置源和类型安全的配置访问。
 * 配置读取优先级：系统属性 > 环境变量 > Spring配置文件 > 默认值
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
public final class ConfigManager {

    /**
     * 配置缓存，避免重复解析
     */
    private final Map<String, Object> configCache = new ConcurrentHashMap<>();

    /**
     * Spring环境对象，用于读取配置文件
     */
    private Environment environment;

    /**
     * 私有构造器，防止外部实例化
     */
    public ConfigManager() {
        // 初始化时尝试获取Spring Environment
        initializeEnvironment();
    }

    /**
     * 获取字符串配置
     * 
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public String getString(String key, String defaultValue) {
        validateKey(key);
        return (String) configCache.computeIfAbsent(key, k -> {
            String value = findConfigValue(k);
            return StringUtils.hasText(value) ? value : defaultValue;
        });
    }

    /**
     * 获取字符串配置（Optional方式）
     * 
     * @param key 配置键
     * @return Optional包装的配置值
     */
    public Optional<String> getStringOptional(String key) {
        validateKey(key);
        String value = getString(key, null);
        return Optional.ofNullable(value);
    }

    /**
     * 获取整数配置
     * 
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public Integer getInt(String key, Integer defaultValue) {
        validateKey(key);
        return (Integer) configCache.computeIfAbsent(key + ":int", k -> {
            String value = findConfigValue(key);
            if (StringUtils.hasText(value)) {
                try {
                    return Integer.valueOf(value.trim());
                } catch (NumberFormatException e) {
                    logConversionError(key, value, "Integer", e);
                }
            }
            return defaultValue;
        });
    }

    /**
     * 获取整数配置（Optional方式）
     * 
     * @param key 配置键
     * @return Optional包装的配置值
     */
    public Optional<Integer> getIntOptional(String key) {
        validateKey(key);
        Integer value = getInt(key, null);
        return Optional.ofNullable(value);
    }

    /**
     * 获取长整数配置
     * 
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public Long getLong(String key, Long defaultValue) {
        validateKey(key);
        return (Long) configCache.computeIfAbsent(key + ":long", k -> {
            String value = findConfigValue(key);
            if (StringUtils.hasText(value)) {
                try {
                    return Long.valueOf(value.trim());
                } catch (NumberFormatException e) {
                    logConversionError(key, value, "Long", e);
                }
            }
            return defaultValue;
        });
    }

    /**
     * 获取长整数配置（Optional方式）
     * 
     * @param key 配置键
     * @return Optional包装的配置值
     */
    public Optional<Long> getLongOptional(String key) {
        validateKey(key);
        Long value = getLong(key, null);
        return Optional.ofNullable(value);
    }

    /**
     * 获取布尔配置
     * 
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public Boolean getBoolean(String key, Boolean defaultValue) {
        validateKey(key);
        return (Boolean) configCache.computeIfAbsent(key + ":boolean", k -> {
            String value = findConfigValue(key);
            if (StringUtils.hasText(value)) {
                String trimmedValue = value.trim().toLowerCase();
                return "true".equals(trimmedValue) || "1".equals(trimmedValue) || "yes".equals(trimmedValue);
            }
            return defaultValue;
        });
    }

    /**
     * 获取布尔配置（Optional方式）
     * 
     * @param key 配置键
     * @return Optional包装的配置值
     */
    public Optional<Boolean> getBooleanOptional(String key) {
        validateKey(key);
        Boolean value = getBoolean(key, null);
        return Optional.ofNullable(value);
    }

    /**
     * 获取双精度浮点配置
     * 
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public Double getDouble(String key, Double defaultValue) {
        validateKey(key);
        return (Double) configCache.computeIfAbsent(key + ":double", k -> {
            String value = findConfigValue(key);
            if (StringUtils.hasText(value)) {
                try {
                    return Double.valueOf(value.trim());
                } catch (NumberFormatException e) {
                    logConversionError(key, value, "Double", e);
                }
            }
            return defaultValue;
        });
    }

    /**
     * 获取双精度浮点配置（Optional方式）
     * 
     * @param key 配置键
     * @return Optional包装的配置值
     */
    public Optional<Double> getDoubleOptional(String key) {
        validateKey(key);
        Double value = getDouble(key, null);
        return Optional.ofNullable(value);
    }

    /**
     * 检查配置是否存在
     * 
     * @param key 配置键
     * @return 是否存在
     */
    public boolean containsKey(String key) {
        validateKey(key);
        return StringUtils.hasText(findConfigValue(key));
    }

    /**
     * 清空配置缓存
     */
    public void clearCache() {
        configCache.clear();
    }

    /**
     * 获取缓存大小
     * 
     * @return 缓存中的配置项数量
     */
    public int getCacheSize() {
        return configCache.size();
    }

    /**
     * 初始化Spring Environment
     */
    private void initializeEnvironment() {
        try {
            // 如果在Spring容器中，尝试获取Environment
            // 这里可以后续与ApplicationContextHolder集成
            // environment = ApplicationContextHolder.getBean(Environment.class);
        } catch (Exception e) {
            // 忽略异常，使用系统属性和环境变量作为fallback
        }
    }

    /**
     * 查找配置值
     * 优先级：系统属性 > 环境变量 > Spring配置文件
     * 
     * @param key 配置键
     * @return 配置值，可能为null
     */
    private String findConfigValue(String key) {
        // 1. 系统属性
        String value = System.getProperty(key);
        if (StringUtils.hasText(value)) {
            return value;
        }

        // 2. 环境变量（将点号转换为下划线）
        String envKey = key.replace('.', '_').toUpperCase();
        value = System.getenv(envKey);
        if (StringUtils.hasText(value)) {
            return value;
        }

        // 3. Spring配置文件（如果可用）
        if (environment != null) {
            value = environment.getProperty(key);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }

        return null;
    }

    /**
     * 验证配置键
     * 
     * @param key 配置键
     */
    private void validateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Configuration key cannot be null or empty");
        }
    }

    /**
     * 记录类型转换错误
     * 
     * @param key       配置键
     * @param value     配置值
     * @param targetType 目标类型
     * @param e         异常
     */
    private void logConversionError(String key, String value, String targetType, Exception e) {
        System.err.printf("Failed to convert config [%s=%s] to %s: %s%n", 
                         key, value, targetType, e.getMessage());
    }
}