package com.xpcjsu.sunshinemall.framework.base.singleton.xpc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/*getInstance(Class<T>, Supplier<T>) - 生产环境主要API
getInstance(Class<T>) - 生产环境主要API
containsInstance(Class<?>) - 生产环境可能用到*/

//这个类与SingletonHolder.java一样，只是用来重敲一遍，更好的掌握单例模式
public final class xpcSingletonHolder {

    // 单例实例存储容器
    private static final Map<Class<?>, Object> INSTANCES = new ConcurrentHashMap<>();

    //工具类，不能实例化
    private xpcSingletonHolder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }


    // 获取单例实例（自定义初始化逻辑）
    @SuppressWarnings("unchecked")
    public static <T> T getInstance(Class<T> clazz, Supplier<T> supplier) {

        if(clazz == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }
        if (supplier == null) {
            throw new IllegalArgumentException("Supplier cannot be null");
        }

        //这个强制转换是安全的
        return (T) INSTANCES.computeIfAbsent(clazz, k -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create singleton instance for class: " + clazz.getName(), e);
            }
        });
    }

    // 获取单例实例（使用默认无参构造器）
    public static <T> T getInstance(Class<T> clazz) {
        return getInstance(clazz, () -> {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create singleton instance using default constructor for class: " + clazz.getName(), e);
            }
        });
    }

    // 检查指定类型是否已有单例实例
    public static boolean containsInstance(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        return INSTANCES.containsKey(clazz);
    }

}
