package com.xpcjsu.sunshinemall.framework.idempotent.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 幂等性注解
 * <p>
 * 用于标记需要保证幂等性的方法，防止重复提交。
 * <p>
 * 使用示例：
 * <pre>
 * {@code @Idempotent(key = "#orderId", expireTime = 60)}
 * public void createOrder(Long orderId) {
 *     // 业务逻辑
 * }
 * </pre>
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
@Target(ElementType.METHOD)//该注解只能应用于方法上
//该注解会在编译后的class文件中保留,运行时通过反射获取。运行时动态地读取和处理该注解信息。
@Retention(RetentionPolicy.RUNTIME)
@Documented//元素会在生成的API文档中显示出来
//定义注解的关键字
public @interface Idempotent {

    /**
     * 幂等键表达式（支持SpEL）
     * <p>
 * 示例：
     * <ul>
     * <li>"#orderId" - 使用方法参数orderId</li>
     * <li>"#user.id" - 使用对象属性</li>
     * <li>"'fixed'" - 固定字符串</li>
     * </ul>
     * 
     * @return 幂等键表达式
     */
    String key();

    /**
     * 幂等键前缀,默认值设置
     * <p>
     * 用于区分不同业务场景，默认为"idempotent"
     *
     * @return 幂等键前缀
     */
    String prefix() default "idempotent";

    /**
     * 过期时间
     * <p>
     * 超过此时间后，幂等限制失效，默认60秒
     * 
     * @return 过期时间
     */
    long expireTime() default 60;

    /**
     * 时间单位
     * <p>
     * 默认为秒
     * 
     * @return 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 重复提交时的提示信息
     * <p>
     * 默认为"请勿重复提交"
     * 
     * @return 提示信息
     */
    String message() default "请勿重复提交";
}
