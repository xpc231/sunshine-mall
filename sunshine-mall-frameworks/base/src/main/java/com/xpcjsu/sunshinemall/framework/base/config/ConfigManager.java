package com.xpcjsu.sunshinemall.framework.base.config;

// ===================================================================================
// 【修改1】新增 import ApplicationContextHolder
// 目的：集成ApplicationContextHolder以便从 Spring容器中获取 Environment
// ===================================================================================
import com.xpcjsu.sunshinemall.framework.base.context.ApplicationContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.Map;
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
     * 配置缓存，避免重复解析，安全高并发
     */
    private final Map<String, Object> configCache = new ConcurrentHashMap<>();

    /**
     * Spring环境对象，用于读取配置文件
     */
    private Environment environment;

    /**
     * 私有构造器，防止外部实例化
     * 建议通过SingletonHolder.getInstance(ConfigManager.class)获取实例
     */
    private ConfigManager() {
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
            // findConfigValue已统一处理trim，这里直接使用
            if (StringUtils.hasText(value)) {
                return value;
            }
            return defaultValue;
        });
    }

    /**
     * 获取整数配置
     *.valueOf()可能抛出异常
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public Integer getInt(String key, Integer defaultValue) {
        validateKey(key);
        return (Integer) configCache.computeIfAbsent(key + ":int", k -> {
            String value = findConfigValue(key);
            // findConfigValue已统一处理trim，这里直接使用
            if (StringUtils.hasText(value)) {
                try {
                    return Integer.valueOf(value);
                } catch (NumberFormatException e) {
                    throw new ConfigException("配置值格式错误，期望整数类型", key, e);
                }
            }
            return defaultValue;
        });
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
            // findConfigValue已统一处理trim，这里直接使用
            if (StringUtils.hasText(value)) {
                try {
                    return Long.valueOf(value);
                } catch (NumberFormatException e) {
                    throw new ConfigException("配置值格式错误，期望长整数类型", key, e);
                }
            }
            return defaultValue;
        });
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
            // 统一转化为小写
            if (StringUtils.hasText(value)) {
                String lowerValue = value.toLowerCase();
                return "true".equals(lowerValue) || "1".equals(lowerValue) || "yes".equals(lowerValue);
            }
            return defaultValue;//明确的默认行为
        });
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
            // findConfigValue已统一处理trim，这里直接使用
            if (StringUtils.hasText(value)) {
                try {
                    return Double.valueOf(value);
                } catch (NumberFormatException e) {
                    throw new ConfigException("配置值格式错误，期望双精度浮点类型", key, e);
                }
            }
            return defaultValue;
        });
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
     * 初始化Spring Environment
     */
    private void initializeEnvironment() {
        try {
            // ===================================================================================
            // 【修改2】修改之前：注释掉的代码
            // 修改之后：使用ApplicationContextHolder获取Spring Environment
            // 目的：实现完整的配置源支持，从系统属性、环境变量、Spring配置文件中读取配置
            // ===================================================================================
            
            // 修改前的代码：
            // if (在Spring容器中，尝试获取Environment
            // 这里可以后续与ApplicationContextHolder集成
            // environment = ApplicationContextHolder.getBean(Environment.class);
            
            // 修改后的代码：
            //使用了 Optional.orElse()
            if (ApplicationContextHolder.isApplicationContextInitialized()) {
                this.environment = ApplicationContextHolder.getBeanOptional(Environment.class)
                    .orElse(null);
            }
        } catch (Exception e) {
            // 忽略异常，使用系统属性和环境变量作为fallback
        }
    }

    /**
     * 查找配置值
     * 优先级：系统属性 > 环境变量 > Spring配置文件
     * <p>
     * 统一处理trim，避免在各个类型方法中重复处理
     * 
     * @param key 配置键
     * @return 配置值（已trim），可能为null
     */
    private String findConfigValue(String key) {
        // 1. 系统属性
        String value = System.getProperty(key);
        //spring 框架提供的一个工具方法，用于检查字符串是否包含文本内容。
        if (StringUtils.hasText(value)) {
            return value.trim();
        }

        // 2. 环境变量（将点号转换为下划线）,命名规范
        String envKey = key.replace('.', '_').toUpperCase();
        value = System.getenv(envKey);
        if (StringUtils.hasText(value)) {
            return value.trim();
        }

        // 3. Spring配置文件（如果可用）
        if (environment != null) {
            value = environment.getProperty(key);
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }

        return null;
    }

    /**
     * 验证配置键
     * 
     * @param key 配置键
     * @throws ConfigException 当配置键为null或空字符串时
     */
    private void validateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new ConfigException("配置键不能为null或空字符串", key);
        }
    }


    /**
     * 清空配置缓存
     *
     * <p><strong>注意：此方法仅用于测试，生产环境不应使用</strong></p>
     * 清空缓存会导致下次获取配置时重新解析，影响性能
     */
    public void clearCache() {
        configCache.clear();
    }

    /**
     * 获取缓存大小
     *
     * <p><strong>注意：此方法仅用于测试，生产环境不应使用</strong></p>
     * 主要用于验证缓存机制是否正常工作
     *
     * @return 缓存中的配置项数量
     */
    public int getCacheSize() {
        return configCache.size();
    }
}

