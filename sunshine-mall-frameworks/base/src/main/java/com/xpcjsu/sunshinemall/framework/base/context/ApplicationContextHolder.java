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
 */
@Component
public final class ApplicationContextHolder implements ApplicationContextAware {


    private static volatile ApplicationContext applicationContext;

    /**
     * 私有构造函数，防止实例化
     */
    private ApplicationContextHolder() {
    }

    /**
     * Spring容器启动时回调此方法，设置应用上下文
     */
    @Override//实现接口
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        //spring启动时自动调用该类的该方法，并将自己的ApplicationContext赋值给该类的静态字段
        ApplicationContextHolder.applicationContext = applicationContext;
    }

    /**
     * 获取Spring应用上下文
     */
    public static ApplicationContext getApplicationContext() {
        assertApplicationContextNotNull();
        return applicationContext;
    }

    /**
     * 根据Bean类型获取实例
     * 第一个 <T> 表示这是一个泛型方法当Bean不存在或存在多个时抛出
     */
    public static <T> T getBean(Class<T> clazz) {
        assertApplicationContextNotNull();
        return applicationContext.getBean(clazz);
    }

    /**
     * 根据Bean名称获取实例
     */
    public static Object getBean(String beanName) {
        assertApplicationContextNotNull();
        return applicationContext.getBean(beanName);
    }

    /**
     * 根据Bean名称和类型获取实例
     */
    public static <T> T getBean(String beanName, Class<T> clazz) {
        assertApplicationContextNotNull();
        return applicationContext.getBean(beanName, clazz);
    }

    /**
     * 根据Bean类型获取实例（Optional方式），避免处理异常
     */
    public static <T> Optional<T> getBeanOptional(Class<T> clazz) {
        if (!isApplicationContextInitialized()) {
            return Optional.empty();
        }

        try {
            return Optional.of(applicationContext.getBean(clazz));
        } catch (BeansException e) {
            return Optional.empty();
        }
    }

    /**
     * 根据Bean名称获取实例（Optional方式）
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
     */
    public static <T> Map<String, T> getBeansOfType(Class<T> clazz) {
        assertApplicationContextNotNull();
        return applicationContext.getBeansOfType(clazz);
    }

    /**
     * 根据注解获取所有标注的Bean实例
     */
    public static Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotation) {
        assertApplicationContextNotNull();
        return applicationContext.getBeansWithAnnotation(annotation);
    }

    /**
     * 检查是否包含指定名称的Bean
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
     */
    public static boolean isTypeMatch(String beanName, Class<?> type) {
        if (!isApplicationContextInitialized()) {
            return false;
        }
        return applicationContext.isTypeMatch(beanName, type);
    }

    /**
     * 检查Spring容器是否已初始化，避免抛出异常
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
}