package com.xpcjsu.sunshinemall.framework.idempotent.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 幂等性模块配置类
 * <p>
 * 启用AspectJ自动代理，扫描幂等性相关组件，负责初始化和配置整个幂等性框架
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
@Configuration
@EnableAspectJAutoProxy
@ComponentScan("com.xpcjsu.sunshinemall.framework.idempotent")
public class IdempotentConfig {
}
