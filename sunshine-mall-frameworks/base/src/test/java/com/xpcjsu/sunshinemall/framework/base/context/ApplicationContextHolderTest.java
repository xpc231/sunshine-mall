package com.xpcjsu.sunshinemall.framework.base.context;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ApplicationContextHolder 测试类
 * 
 * 测试Spring上下文管理器的各种功能
 * 包括Bean获取、容器状态检查等
 * 
 * @author sunshine-mall
 * @version 1.0
 */
@SpringBootTest
@ActiveProfiles("test")//指定当前运行环境使用"test"配置文件
class ApplicationContextHolderTest {

    @Test
    void testApplicationContextInitialization() {
        // 测试Spring容器是否正确初始化
        assertTrue(ApplicationContextHolder.isApplicationContextInitialized(),
                "Spring容器应该已经初始化");//assertTrue 断言方法，用于验证给定的布尔表达式是否为 true。
        
        ApplicationContext context = ApplicationContextHolder.getApplicationContext();
        assertNotNull(context, "ApplicationContext不应该为空");//对象是否不为 null
    }

    @Test
    void testGetBeanByType() {
        // 测试按类型获取Bean
        ApplicationContextHolder contextHolder = ApplicationContextHolder.getBean(ApplicationContextHolder.class);
        assertNotNull(contextHolder, "应该能够获取到ApplicationContextHolder实例");
    }

    @Test
    void testGetBeanOptional() {
        // 测试Optional方式获取Bean
        Optional<ApplicationContextHolder> optional = ApplicationContextHolder.getBeanOptional(ApplicationContextHolder.class);
        assertTrue(optional.isPresent(), "应该能够获取到ApplicationContextHolder的Optional实例");

        // 测试不存在的Bean
        Optional<String> nonExistentBean = ApplicationContextHolder.getBeanOptional(String.class);
        // 注意：这里可能会返回空或者抛出异常，取决于Spring容器中是否有String类型的Bean
    }

    @Test
    void testContainsBean() {
        // 测试Bean存在性检查
        boolean exists = ApplicationContextHolder.containsBean("applicationContextHolder");
        assertTrue(exists, "应该包含applicationContextHolder这个Bean");
    }

    @Test
    void testGetBeanDefinitionCount() {
        // 测试获取Bean定义数量
        int count = ApplicationContextHolder.getBeanDefinitionCount();
        assertTrue(count > 0, "Bean定义数量应该大于0");
    }

    @Test
    void testGetBeanDefinitionNames() {
        // 测试获取所有Bean名称
        String[] names = ApplicationContextHolder.getBeanDefinitionNames();
        assertNotNull(names, "Bean名称数组不应该为空");
        assertTrue(names.length > 0, "应该有Bean定义");
    }


}