package com.xpcjsu.sunshinemall.framework.cache.core;

import com.xpcjsu.sunshinemall.framework.cache.constant.CacheConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 缓存管理器
 * <p>
 * 提供统一的Redis缓存操作接口，封装常用的缓存操作。
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */


@Slf4j
@Component
@RequiredArgsConstructor
public class CacheManager {

    //若一个类只有一个构造方法，Spring会自动通过该构造方法注入依赖（无需 @Autowired）
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 设置缓存（使用默认过期时间）
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    public void set(String key, Object value) {
        set(key, value, CacheConstant.DEFAULT_EXPIRE_TIME);
    }

    /**
     * 设置缓存（指定过期时间）
     *
     * @param key        缓存键
     * @param value      缓存值
     * @param expireTime 过期时间（秒）
     */
    public void set(String key, Object value, long expireTime) {
        try {
            if (expireTime == CacheConstant.PERMANENT_EXPIRE_TIME) {
                // 永久缓存
                // opsForValue()返回 ValueOperations<String, Object> 接口实例
                redisTemplate.opsForValue().set(key, value);
            } else {
                //TimeUnit是并发包中的时间单位枚举量，用于指定时间的计量单位
                redisTemplate.opsForValue().set(key, value, expireTime, TimeUnit.SECONDS);
            }
            log.debug("设置缓存成功 - key: {}, expireTime: {}s", key, expireTime);
        } catch (Exception e) {
            log.error("设置缓存失败 - key: {}", key, e);
            throw new RuntimeException("设置缓存失败", e);
        }
    }

    /**
     * 获取缓存
     *
     * @param key 缓存键
     * @return 缓存值
     */
    public Object get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            //hit:是否命中
            log.debug("获取缓存 - key: {}, hit: {}", key, value != null);
            return value;
        } catch (Exception e) {
            log.error("获取缓存失败 - key: {}", key, e);
            //业务代码可以自然处理
            return null;
        }
    }

    /**
     * 获取缓存（带类型转换）
     *
     * @param key   缓存键
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 缓存值
     */
    //使用泛型方法获取缓存,强约束，避免类型转换异常
    public <T> T get(String key, Class<T> clazz) {
        Object value = get(key);
        //缓存未命中
        if (value == null) {
            return null;
        }
        
        // 处理空值缓存
        if (CacheConstant.NULL_VALUE.equals(value)) {
            return null;
        }
        
        try {
            return (T) value;
        } catch (ClassCastException e) {
            log.error("缓存值类型转换失败 - key: {}, expected: {}, actual: {}", 
                    key, clazz.getName(), value.getClass().getName());
            return null;
        }
    }

    /**
     * 删除缓存
     * <p>
     * 注意：删除失败会抛出异常，因为可能导致脏数据残留
     *
     * @param key 缓存键
     * @return 是否删除成功（true:删除成功或key不存在, false:其他情况）
     * @throws RuntimeException 删除操作失败时
     */
    public Boolean delete(String key) {
        try {
            Boolean result = redisTemplate.delete(key);//可能返回 null
            log.debug("删除缓存 - key: {}, result: {}", key, result);
            return result != null ? result : false;
        } catch (Exception e) {
            log.error("删除缓存失败 - key: {}", key, e);
            throw new RuntimeException("删除缓存失败", e);
        }
    }

    /**
     * 批量删除缓存
     * <p>
     * 注意：删除失败会抛出异常，因为可能导致脏数据残留
     *
     * @param keys 缓存键集合
     * @return 删除的数量
     * @throws RuntimeException 删除操作失败时
     */
    public Long delete(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }
        try {
            Long count = redisTemplate.delete(keys);
            log.debug("批量删除缓存 - count: {}", count);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.error("批量删除缓存失败 - keys: {}", keys, e);
            throw new RuntimeException("批量删除缓存失败", e);
        }
    }


    /**
     * 设置空值缓存（防止缓存穿透）
     * <p>
     * 使用默认过期时间（5分钟）
     *
     * @param key 缓存键
     */
    public void setNullValue(String key) {
        setNullValue(key, CacheConstant.NULL_VALUE_EXPIRE_TIME);
    }

    /**
     * 设置空值缓存（防止缓存穿透）
     * <p>
     * 支持自定义过期时间，适应不同业务场景：
     * <ul>
     * <li>高频查询的不存在数据：设置较长时间（1-2小时）</li>
     * <li>低频查询的不存在数据：设置较短时间（1-3分钟）</li>
     * <li>可能很快创建的数据：设置很短时间（30秒-1分钟）</li>
     * </ul>
     *
     * @param key        缓存键
     * @param expireTime 过期时间（秒）
     */
    public void setNullValue(String key, long expireTime) {
        set(key, CacheConstant.NULL_VALUE, expireTime);
        log.debug("设置空值缓存 - key: {}, expireTime: {}s", key, expireTime);
    }

    /**
     * 批量获取缓存
     *
     * @param keys 缓存键集合
     * @return 缓存值列表
     */
    public List<Object> multiGet(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            //return List.of();
            return Collections.emptyList();
        }
        try {
            return redisTemplate.opsForValue().multiGet(keys);
        } catch (Exception e) {
            log.error("批量获取缓存失败 - keys: {}", keys, e);
            return Collections.emptyList();
        }
    }

    /**
     * 自增操作
     *
     * @param key   缓存键
     * @param delta 增量值
     * @return 自增后的值
     */
    public Long increment(String key, long delta) {
        try {
            return redisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            log.error("自增操作失败 - key: {}, delta: {}", key, delta, e);
            throw new RuntimeException("自增操作失败", e);
        }
    }

    /**
     * 自减操作
     *
     * @param key   缓存键
     * @param delta 减量值
     * @return 自减后的值
     */
    public Long decrement(String key, long delta) {
        try {
            return redisTemplate.opsForValue().decrement(key, delta);
        } catch (Exception e) {
            log.error("自减操作失败 - key: {}, delta: {}", key, delta, e);
            //对于可能影响数据一致性的操作都采用抛出异常的方式
            throw new RuntimeException("自减操作失败", e);
        }
    }
}
