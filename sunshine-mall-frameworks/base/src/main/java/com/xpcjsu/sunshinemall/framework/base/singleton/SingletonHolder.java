package com.xpcjsu.sunshinemall.framework.base.singleton;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 单例工具类
 * <p>
 * 基于ConcurrentHashMap实现，保证高并发环境下的性能和安全性。
 */
//final确保不会被继承
public final class SingletonHolder {

    /**
     * 单例实例存储容器
     */
    private static final Map<Class<?>, Object> INSTANCES = new ConcurrentHashMap<>();

    /**
     * 私有构造器，防止实例化
     */
    private SingletonHolder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 获取单例实例（自定义初始化逻辑）
     *
     */
    public static <T> T getInstance(Class<T> clazz, Supplier<T> supplier) {
        //显式进行 null 判断
        if (clazz == null) {
            throw new IllegalArgumentException("Class cannot be null");//非法参数异常
        }
        if (supplier == null) {
            throw new IllegalArgumentException("Supplier cannot be null");
        }
        
        return (T) INSTANCES.computeIfAbsent(clazz, k -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create singleton instance for class: " + clazz.getName(), e);
            }
        });
    }

    /**
     * 获取单例实例（使用默认无参构造器）
     *
     */
    public static <T> T getInstance(Class<T> clazz) {
        //调用的方法内部进行了null判断
        return getInstance(clazz, () -> {
            try {
                //获取类的默认无参构造器
                Constructor<T> constructor = clazz.getDeclaredConstructor();
                // 设置可访问，允许访问私有构造器
                constructor.setAccessible(true);
                return constructor.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create singleton instance using default constructor for class: "
                        + clazz.getName(), e);
            }
        });
    }

    /**
     * 检查指定类型是否已有单例实例
     */
    public static boolean containsInstance(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        return INSTANCES.containsKey(clazz);
    }

    /**
     * 移除指定类型的单例实例
     * <p>
     * 注意：此操作会破坏单例语义，谨慎使用，主要用于测试场景。
     */
    public static <T> T removeInstance(Class<T> clazz) {
        if (clazz == null) {
            return null;
        }
        return (T) INSTANCES.remove(clazz);
    }

}