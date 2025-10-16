package com.xpcjsu.sunshinemall.framework.cache.core;

import com.xpcjsu.sunshinemall.framework.cache.constant.CacheConstant;
import com.xpcjsu.sunshinemall.framework.common.util.StringUtils;

/**
 * 缓存键构建工具
 * <p>
 * 提供统一的缓存键构建规则，格式：prefix:module:business:id
 * 例如：sunshine-mall:user:info:123
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
public final class CacheKeyBuilder {

    private CacheKeyBuilder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 构建缓存键
     * 
     * @param parts 键的组成部分
     * @return 完整的缓存键
     */
    //... 表示可变参数，允许方法接受不定数量的 String 类型参数
    public static String build(String... parts) {
        if (parts == null || parts.length == 0) {
            throw new IllegalArgumentException("缓存键组成部分不能为空");
        }

        //局部变量存储在方法调用栈中，每个线程都有独立的栈空间,线程安全
        StringBuilder keyBuilder = new StringBuilder(CacheConstant.CACHE_KEY_PREFIX);
        for (String part : parts) {
            if (StringUtils.isBlank(part)) {
                throw new IllegalArgumentException("缓存键组成部分不能为空字符串");
            }
            keyBuilder.append(CacheConstant.CACHE_KEY_SEPARATOR).append(part.trim());
        }
        return keyBuilder.toString();
    }

    /**
     * 构建缓存键（带模块前缀）
     * 
     * @param module   模块名称（如：user、product、order）
     * @param business 业务标识（如：info、list、count）
     * @param id       业务ID
     * @return 完整的缓存键
     */
    public static String build(String module, String business, Object id) {
        if (id == null) {
            throw new IllegalArgumentException("业务ID不能为null");
        }
        return build(module, business, id.toString());
    }

    /**
     * 构建模式匹配键
     * <p>
     * 用于批量删除，例如：sunshine-mall:user:*
     * 
     * @param parts 键的组成部分
     * @return 模式匹配键
     */
    public static String buildPattern(String... parts) {
        return build(parts) + CacheConstant.CACHE_KEY_SEPARATOR + "*";
    }
}
