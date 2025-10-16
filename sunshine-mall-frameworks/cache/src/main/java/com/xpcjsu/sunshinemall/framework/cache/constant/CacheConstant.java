package com.xpcjsu.sunshinemall.framework.cache.constant;

/**
 * 缓存常量
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
public final class CacheConstant {

    private CacheConstant() {
        throw new UnsupportedOperationException("Constant class cannot be instantiated");
    }

    /**
     * 缓存键分隔符
     */
    public static final String CACHE_KEY_SEPARATOR = ":";

    /**
     * 缓存键前缀
     */
    public static final String CACHE_KEY_PREFIX = "sunshine-mall";

    /**
     * 空值缓存标识
     */
    public static final String NULL_VALUE = "NULL";

    /**
     * 默认过期时间（秒）：30分钟
     */
    public static final long DEFAULT_EXPIRE_TIME = 1800L;

    /**
     * 空值缓存过期时间（秒）：5分钟
     */
    public static final long NULL_VALUE_EXPIRE_TIME = 300L;

    /**
     * 永久缓存标识
     */
    public static final long PERMANENT_EXPIRE_TIME = -1L;
}
