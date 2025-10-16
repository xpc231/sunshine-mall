package com.xpcjsu.sunshinemall.framework.cache.xpc;

import com.xpcjsu.sunshinemall.framework.cache.constant.CacheConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class xpcCacheManager {

    private final RedisTemplate<String, Object> redisTemplate;

    //默认过期时间
    public void set(String key, Object value) {
        set(key, value, CacheConstant.DEFAULT_EXPIRE_TIME);
    }

    //指定过期时间
    public void set(String key, Object value, long expireTime) {

        try {
            if(expireTime == CacheConstant.PERMANENT_EXPIRE_TIME) {
                redisTemplate.opsForValue().set(key, value);
            } else {
                redisTemplate.opsForValue().set(key, value, expireTime, TimeUnit.SECONDS);
            }
            log.debug("设置缓存成功 - key: {}, expireTime: {}s", key, expireTime);
        } catch (Exception e) {
            log.error("设置缓存失败 - key: {}", key, e);
            throw new RuntimeException(e);
        }
    }

    //获取缓存
    public Object get(String key) {

        try {
            Object value = redisTemplate.opsForValue().get(key);
            log.debug("获取缓存 - key: {}, hit: {}", key, value != null);
            return value;
        } catch (Exception e) {
            log.error("获取缓存失败 - key: {}", key, e);
            return null;
        }
    }

    //获取缓存（带类型转换）
    public <T> T get(String key, Class<T> clazz) {
        Object value = get(key);

        if(value == null) {
            return null;
        }

        if(CacheConstant.NULL_VALUE.equals(value)) {
            return null;
        }

        try {
            return (T) value;
        } catch (Exception e) {
            log.error("缓存值类型转换失败 - key: {}, expected: {}, actual: {}",
                    key, clazz.getName(), value.getClass().getName());
            return null;
        }
    }

    //批量获取缓存
    public List<Object> multiGet(Collection<String> keys) {
        if(keys == null || keys.isEmpty()) {
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

    //删除缓存
    public Boolean delete(String key) {
        try {
            Boolean result = redisTemplate.delete(key);
            log.debug("删除缓存 - key: {}, result: {}", key, result);
            return result != null ? result : false;
        } catch (Exception e) {
            log.error("删除缓存失败 - key: {}", key, e);
            throw new RuntimeException("删除缓存失败", e);
        }
    }

    //批量删除缓存
    public Long delete(Collection<String> keys) {
        if(keys == null || keys.isEmpty()) {
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

    //设置空值缓存（防止缓存穿透）
    public void setNullValue(String key) {
        set(key, CacheConstant.NULL_VALUE, CacheConstant.NULL_VALUE_EXPIRE_TIME);
        log.debug("设置空值缓存 - key: {}", key);
    }

    //自增操作
    public Long increment(String key , long delta) {
        try {
            return redisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            log.error("自增操作失败 - key: {}, delta: {}", key, delta, e);
            throw new RuntimeException("自增操作失败", e);
        }
    }

    //自减操作
    public Long decrement(String key, long delta) {
        try {
            return redisTemplate.opsForValue().decrement(key, delta);
        } catch (Exception e) {
            log.error("自减操作失败 - key: {}, delta: {}", key, delta, e);
            throw new RuntimeException(e);
        }
    }
}
