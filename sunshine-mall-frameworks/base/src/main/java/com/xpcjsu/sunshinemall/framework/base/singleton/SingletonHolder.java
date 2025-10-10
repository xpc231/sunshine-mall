package com.xpcjsu.sunshinemall.framework.base.singleton;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 单例工具类
 * <p>
 * 提供线程安全的单例实例管理，支持自定义初始化逻辑和默认构造器初始化。
 * 基于ConcurrentHashMap实现，保证高并发环境下的性能和安全性。
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
//final确保不会被继承
public final class SingletonHolder {

    /**
     * 单例实例存储容器
     * 使用ConcurrentHashMap保证线程安全和高并发性能
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
     * 如果实例不存在，则使用提供的Supplier创建实例:
     * MyServiceImpl service = SingletonHolder.getInstance(
     *     MyServiceImpl.class,
     *     () -> new MyServiceImpl()  // Lambda 表达式作为 Supplier 实现
     * );
     * 优点:调用方可以自定义对象创建逻辑
     *
     * 该方法线程安全，保证同一类型只会创建一个实例。
     * 
     * @param clazz    目标类型
     * @param supplier 实例创建逻辑
     * @param <T>      泛型类型
     * @return 单例实例
     * @throws RuntimeException 如果实例创建失败
     */
    @SuppressWarnings("unchecked")
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
     * 当目标类有公共无参构造器时，调用方无需提供 Supplier，
     * 直接传入 Class 对象即可获取单例实例
     * 减少了样板代码，提升开发效率
     *
     * 使用反射调用目标类的无参构造器创建实例。
     * 要求目标类必须有可访问的无参构造器。
     * 
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 单例实例
     * @throws RuntimeException 如果目标类没有无参构造器或实例创建失败
     */
    //Class - 类对象或类的运行时类型对象
    public static <T> T getInstance(Class<T> clazz) {
        //调用的方法内部进行了null判断
        return getInstance(clazz, () -> {
            try {
                //获取类的默认无参构造器
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create singleton instance using default constructor for class: " + clazz.getName(), e);
            }
        });
    }

    /**
     * 检查指定类型是否已有单例实例
     * 
     * @param clazz 目标类型
     * @return 如果已存在实例返回true，否则返回false
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
     * 
     * @param clazz 目标类型
     * @return 被移除的实例，如果不存在返回null
     */
    @SuppressWarnings("unchecked")
    public static <T> T removeInstance(Class<T> clazz) {
        if (clazz == null) {
            return null;
        }
        return (T) INSTANCES.remove(clazz);
    }

    /**
     * 清空所有单例实例
     * <p>
     * 注意：此操作会破坏所有单例语义，谨慎使用，主要用于测试场景。
     */
    public static void clear() {
        INSTANCES.clear();
    }

    /**
     * 获取当前管理的单例实例数量
     * 
     * @return 单例实例数量
     */
    public static int size() {
        return INSTANCES.size();
    }
}