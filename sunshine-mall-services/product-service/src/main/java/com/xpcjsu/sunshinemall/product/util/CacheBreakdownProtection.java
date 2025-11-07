package com.xpcjsu.sunshinemall.product.util;

import com.xpcjsu.sunshinemall.framework.cache.core.CacheManager;
import com.xpcjsu.sunshinemall.product.constant.ProductConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 缓存击穿防护工具类
 * <p>
 * 提供三种防护方案：
 * 1. 分布式锁 + 双重检查（推荐）：适用于热点数据，防止并发查询数据库
 * 2. 互斥锁（Mutex Lock）：轻量级方案，使用Redis SETNX实现
 * 3. 永不过期 + 异步刷新：适用于极热点数据，设置永不过期，后台异步刷新
 *
 * @author xpcjsu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheBreakdownProtection {

    private final CacheManager cacheManager;
    private final DistributedLock distributedLock;

    /**
     * 方案1：分布式锁 + 双重检查
     * <p>
     * 流程：
     * 1. 第一次检查缓存
     * 2. 缓存未命中，获取分布式锁
     * 3. 获取锁后，再次检查缓存（双重检查）
     * 4. 如果缓存仍未命中，查询数据库并写入缓存
     * 5. 释放锁
     *
     * @param cacheKey 缓存键
     * @param lockKey 锁的键
     * @param dataLoader 数据加载器（查询数据库的函数）
     * @param expireTime 缓存过期时间（秒）
     * @param converter 缓存对象转换器（处理反序列化问题）
     * @param <T> 数据类型
     * @return 缓存数据
     */
    public <T> T getWithDistributedLock(String cacheKey, String lockKey, 
                                        Supplier<T> dataLoader, long expireTime,
                                        Function<Object, T> converter) {
        // 第一次检查缓存
        T cached = tryGetFromCache(cacheKey, converter, "第一次检查");
        if (cached != null) {
            return cached;
        }

        // 缓存未命中，使用分布式锁保护
        return distributedLock.executeWithLock(lockKey, ProductConstants.Cache.DISTRIBUTED_LOCK_DEFAULT_EXPIRE_TIME, () -> {
            //等待锁期间，可能已经有其他线程完成了数据库查询并更新了缓存
            //双重检查：获取锁后再次检查缓存
            T cached2 = tryGetFromCache(cacheKey, converter, "双重检查");
            if (cached2 != null) {
                return cached2;
            }

            // 缓存仍未命中，查询数据库
            log.debug("缓存未命中，查询数据库 - cacheKey: {}", cacheKey);
            T data = dataLoader.get();

            // 写入缓存
            writeToCache(cacheKey, data, expireTime);

            return data;
        });
    }

    /**
     * 方案2：互斥锁（Mutex Lock）
     * <p>
     * 流程：
     * 1. 第一次检查缓存
     * 2. 缓存未命中，尝试获取互斥锁（SETNX）
     * 3. 获取锁成功，查询数据库并写入缓存，释放锁
     * 4. 获取锁失败，等待一段时间后重试从缓存读取
     *
     * @param cacheKey 缓存键
     * @param dataLoader 数据加载器
     * @param expireTime 缓存过期时间（秒）
     * @param converter 缓存对象转换器（处理反序列化问题）
     * @param <T> 数据类型
     * @return 缓存数据
     */
    public <T> T getWithMutexLock(String cacheKey, Supplier<T> dataLoader, long expireTime,
                                  Function<Object, T> converter) {
        // 第一次检查缓存
        T cached = tryGetFromCache(cacheKey, converter, "第一次检查");
        if (cached != null) {
            return cached;
        }

        // 缓存未命中，设置一个分布式锁键
        String mutexKey = ProductConstants.Cache.MUTEX_LOCK_PREFIX + cacheKey;
        boolean lockAcquired = cacheManager.setIfAbsent(mutexKey, ProductConstants.Cache.MUTEX_LOCK_VALUE, ProductConstants.Cache.MUTEX_LOCK_EXPIRE_TIME);

        if (lockAcquired) {
            // 获取锁成功，查询数据库
            try {
                log.debug("获取互斥锁成功，查询数据库 - cacheKey: {}", cacheKey);
                T data = dataLoader.get();

                // 写入缓存
                writeToCache(cacheKey, data, expireTime);

                return data;
            } finally {
                // 释放互斥锁
                cacheManager.delete(mutexKey);
                log.debug("释放互斥锁 - mutexKey: {}", mutexKey);
            }
        } else {
            // 获取锁失败，等待后重试从缓存读取
            log.debug("获取互斥锁失败，等待后重试 - cacheKey: {}", cacheKey);
            try {
                Thread.sleep(ProductConstants.Cache.MUTEX_LOCK_RETRY_WAIT_TIME_MS); // 等待后重试
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 重试从缓存读取
            T retryCached = tryGetFromCache(cacheKey, converter, "重试");
            if (retryCached != null) {
                return retryCached;
            }

            // 重试仍未命中，返回null
            log.warn("重试后缓存仍未命中 - cacheKey: {}", cacheKey);
            return null;
        }
    }

    /**
     * 方案3：永不过期 + 异步刷新
     * <p>
     * 流程：
     * 1. 检查缓存（永不过期）
     * 2. 缓存命中，直接返回，并触发异步刷新（如果需要）
     * 3. 缓存未命中，查询数据库并写入缓存（永不过期）
     * 4. 如果缓存即将过期（通过逻辑过期时间判断），触发异步刷新
     *
     * @param cacheKey 缓存键
     * @param dataLoader 数据加载器
     * @param refreshTime 逻辑过期时间（秒），用于判断是否需要刷新
     * @param asyncRefreshTask 异步刷新任务
     * @param converter 缓存对象转换器（处理反序列化问题）
     * @param <T> 数据类型
     * @return 缓存数据
     */
    public <T> T getWithNeverExpire(String cacheKey, Supplier<T> dataLoader, 
                                    long refreshTime, Runnable asyncRefreshTask,
                                    Function<Object, T> converter) {
        // 检查缓存
        T cached = tryGetFromCache(cacheKey, converter, "永不过期");
        if (cached != null) {
            // 检查是否需要异步刷新（这里简化处理，实际可以通过缓存中的时间戳判断）
            // 如果需要刷新，异步执行刷新任务
            if (asyncRefreshTask != null) {
                // 异步刷新（这里简化处理，实际应该使用线程池）
                new Thread(() -> {
                    try {
                        asyncRefreshTask.run();
                    } catch (Exception e) {
                        log.error("异步刷新缓存失败 - cacheKey: {}", cacheKey, e);
                    }
                }).start();
            }
            return cached;
        }

        // 缓存未命中，查询数据库
        log.debug("缓存未命中，查询数据库 - cacheKey: {}", cacheKey);
        T data = dataLoader.get();

        // 写入缓存（永不过期）
        writeToCache(cacheKey, data, -1L);

        return data;
    }


    //--私有方法----------------------------------------------------------------------------------------------------------

    /**
     * 尝试从缓存获取并转换对象
     * <p>
     * 如果缓存命中且转换成功，返回转换后的对象
     * 如果转换失败，删除缓存并返回null
     */
    private <T> T tryGetFromCache(String cacheKey, Function<Object, T> converter, String checkType) {
        Object cachedObj = cacheManager.get(cacheKey);
        if (cachedObj == null) {
            return null;
        }

        log.debug("缓存命中（{}） - cacheKey: {}", checkType, cacheKey);
        T converted = converter != null ? converter.apply(cachedObj) : (T) cachedObj;
        if (converted != null) {
            return converted;
        }

        // 转换失败，删除缓存
        log.warn("缓存对象转换失败，删除缓存 - cacheKey: {}", cacheKey);
        cacheManager.delete(cacheKey);
        return null;
    }

    /**
     * 写入缓存
     * <p>
     * 如果数据不为null，写入正常缓存
     * 如果数据为null，写入空值缓存（防止缓存穿透）
     *
     * @param cacheKey 缓存键
     * @param data 数据
     * @param expireTime 过期时间（秒），如果为-1表示永不过期
     * @param <T> 数据类型
     */
    private <T> void writeToCache(String cacheKey, T data, long expireTime) {
        if (data != null) {
            // 写入缓存
            cacheManager.set(cacheKey, data, expireTime);
            log.debug("数据写入缓存 - cacheKey: {}, expireTime: {}s", cacheKey, expireTime);
        } else {
            // 防止缓存穿透：设置空值缓存（使用较短的过期时间）
            long nullValueExpireTime = expireTime == -1L 
                    ? ProductConstants.Cache.NULL_VALUE_CACHE_EXPIRE_TIME 
                    : Math.min(expireTime / 6, ProductConstants.Cache.NULL_VALUE_CACHE_EXPIRE_TIME); // 最多5分钟
            cacheManager.setNullValue(cacheKey, nullValueExpireTime);
            log.debug("设置空值缓存 - cacheKey: {}, expireTime: {}s", cacheKey, nullValueExpireTime);
        }
    }
}

