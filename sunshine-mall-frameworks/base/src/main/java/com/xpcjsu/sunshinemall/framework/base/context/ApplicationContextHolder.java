package com.xpcjsu.sunshinemall.framework.base.context;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;

/**
 * Spring应用上下文管理器
 * 
 * 提供在静态方法中获取Spring容器中Bean的能力
 * 支持多种Bean获取方式：按类型、按名称、按注解等
 * 可以检查是否包含指定名称的Bean，或指定Bean名称和类型是否匹配
 * 线程安全，高性能，支持分布式微服务架构
 * 
 * @author sunshine-mall
 * @version 1.0
 */
@Component
public final class ApplicationContextHolder implements ApplicationContextAware {

    /**
     * 存储Spring容器上下文对象，使用volatile保证多线程可见性
     */
    private static volatile ApplicationContext applicationContext;

    /**
     * 私有构造函数，防止实例化
     */
    private ApplicationContextHolder() {
    }

    /**
     * Spring容器启动时回调此方法，设置应用上下文
     *
     * @param applicationContext Spring应用上下文
     * @throws BeansException Bean异常
     */
    @Override//实现接口
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        //spring启动时自动调用该类的该方法，并将自己的ApplicationContext赋值给该类的静态字段
        ApplicationContextHolder.applicationContext = applicationContext;
    }

    /**
     * 获取Spring应用上下文
     * 
     * @return ApplicationContext实例
     * @throws IllegalStateException 当容器未启动时抛出
     */
    public static ApplicationContext getApplicationContext() {
        assertApplicationContextNotNull();
        return applicationContext;
    }

    /**
     * 根据Bean类型获取实例
     * 
     * @param clazz Bean类型
     * @param <T>   泛型类型
     * @return Bean实例
     * @throws IllegalStateException 当容器未启动时抛出
     * @throws BeansException
     * 第一个 <T> 表示这是一个泛型方法当Bean不存在或存在多个时抛出
     */
    public static <T> T getBean(Class<T> clazz) {
        assertApplicationContextNotNull();
        return applicationContext.getBean(clazz);
    }

    /**
     * 根据Bean名称获取实例
     * 
     * @param beanName Bean名称
     * @return Bean实例
     * @throws IllegalStateException 当容器未启动时抛出
     * @throws BeansException       当Bean不存在时抛出
     */
    public static Object getBean(String beanName) {
        assertApplicationContextNotNull();
        return applicationContext.getBean(beanName);
    }

    /**
     * 根据Bean名称和类型获取实例
     * 
     * @param beanName Bean名称
     * @param clazz    Bean类型
     * @param <T>      泛型类型
     * @return Bean实例
     * @throws IllegalStateException 当容器未启动时抛出
     * @throws BeansException       当Bean不存在或类型不匹配时抛出
     */
    public static <T> T getBean(String beanName, Class<T> clazz) {
        assertApplicationContextNotNull();
        return applicationContext.getBean(beanName, clazz);
    }

    /**
     * 根据Bean类型获取实例（Optional方式），避免处理异常
     * 
     * @param clazz Bean类型
     * @param <T>   泛型类型
     * @return Optional包装的Bean实例
     */
    public static <T> Optional<T> getBeanOptional(Class<T> clazz) {
        assertApplicationContextNotNull();
        
        try {
            return Optional.of(applicationContext.getBean(clazz));
        } catch (BeansException e) {
            return Optional.empty();
        }
    }

    /**
     * 根据Bean名称获取实例（Optional方式）
     * 
     * @param beanName Bean名称
     * @return Optional包装的Bean实例
     */
    public static Optional<Object> getBeanOptional(String beanName) {
        if (!isApplicationContextInitialized()) {
            return Optional.empty();
        }
        
        try {
            return Optional.of(applicationContext.getBean(beanName));
        } catch (BeansException e) {
            return Optional.empty();
        }
    }

    /**
     * 根据Bean类型获取所有实例
     * 
     * @param clazz Bean类型
     * @param <T>   泛型类型
     * @return Bean名称到实例的映射
     * @throws IllegalStateException 当容器未启动时抛出
     */
    public static <T> Map<String, T> getBeansOfType(Class<T> clazz) {
        assertApplicationContextNotNull();
        return applicationContext.getBeansOfType(clazz);
    }

    /**
     * 根据注解获取所有标注的Bean实例
     * 
     * @param annotation 注解类型
     * @return Bean名称到实例的映射
     * @throws IllegalStateException 当容器未启动时抛出
     */
    public static Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotation) {
        assertApplicationContextNotNull();
        return applicationContext.getBeansWithAnnotation(annotation);
    }

    /**
     * 检查是否包含指定名称的Bean
     * 
     * @param beanName Bean名称
     * @return 是否包含
     */
    public static boolean containsBean(String beanName) {
        //望得到布尔值，而不是异常,所以不使用assertApplicationContextNotNull()
        if (!isApplicationContextInitialized()) {
            return false;
        }
        return applicationContext.containsBean(beanName);
    }

    /**
     * 检查指定Bean名称和类型是否匹配
     * 
     * @param beanName Bean名称
     * @param type     类型
     * @return 是否匹配
     */
    public static boolean isTypeMatch(String beanName, Class<?> type) {
        if (!isApplicationContextInitialized()) {
            return false;
        }
        return applicationContext.isTypeMatch(beanName, type);
    }

    /**
     * 检查Spring容器是否已初始化，避免抛出异常
     * 
     * @return 是否已初始化
     */
    public static boolean isApplicationContextInitialized() {
        return applicationContext != null;
    }

    /**
     * 断言ApplicationContext不为空
     * 确保了在 Spring 容器未准备就绪时，不会出现 NullPointerException
     * @throws IllegalStateException 当容器未启动时抛出
     */
    private static void assertApplicationContextNotNull() {
        if (applicationContext == null) {
            throw new IllegalStateException(
                "Spring ApplicationContext has not been initialized. " +
                "Please ensure the Spring container is started and ApplicationContextHolder is properly configured."
            );
        }
    }

    /**
     * 获取容器中Bean的总数量（调试用）
     * 
     * @return Bean数量
     */
    public static int getBeanDefinitionCount() {
        if (!isApplicationContextInitialized()) {
            return 0;
        }
        return applicationContext.getBeanDefinitionCount();
    }

    /**
     * 获取容器中所有Bean的名称（调试用）
     * 
     * @return Bean名称数组
     */
    public static String[] getBeanDefinitionNames() {
        if (!isApplicationContextInitialized()) {
            return new String[0];
        }
        return applicationContext.getBeanDefinitionNames();
    }
}