package com.xpcjsu.sunshinemall.framework.base.exception;

import com.xpcjsu.sunshinemall.framework.base.config.ConfigException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BaseException测试类
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
class BaseExceptionTest {

    @Test
    void testBasicConstructor() {
        BaseException exception = new BaseException("Test message");
        
        assertEquals("Test message", exception.getMessage());
        assertNull(exception.getErrorCode());
        assertNull(exception.getDetails());
        assertTrue(exception.getContext().isEmpty());
        assertNull(exception.getCause());
    }

    @Test
    void testErrorCodeConstructor() {
        BaseException exception = new BaseException("TEST_001", "Test message");
        
        assertEquals("Test message", exception.getMessage());
        assertEquals("TEST_001", exception.getErrorCode());
        assertNull(exception.getDetails());
        assertTrue(exception.getContext().isEmpty());
    }

    @Test
    void testWithCauseConstructor() {
        RuntimeException cause = new RuntimeException("Root cause");
        BaseException exception = new BaseException("Test message", cause);
        
        assertEquals("Test message", exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertNull(exception.getErrorCode());
    }

    @Test
    void testCompleteConstructor() {
        RuntimeException cause = new RuntimeException("Root cause");
        Map<String, Object> context = new HashMap<>();
        context.put("userId", "12345");
        context.put("operation", "update");
        
        BaseException exception = new BaseException(
            "TEST_001", 
            "Test message", 
            "Technical details", 
            cause, 
            context
        );
        
        assertEquals("Test message", exception.getMessage());
        assertEquals("TEST_001", exception.getErrorCode());
        assertEquals("Technical details", exception.getDetails());
        assertEquals(cause, exception.getCause());
        assertEquals(2, exception.getContext().size());
        assertEquals("12345", exception.getContext().get("userId"));
        assertEquals("update", exception.getContext().get("operation"));
    }

    @Test
    void testAddContext() {
        BaseException exception = new BaseException("Test message");
        
        exception.addContext("key1", "value1")
                 .addContext("key2", 123);
        
        assertEquals(2, exception.getContext().size());
        assertEquals("value1", exception.getContext().get("key1"));
        assertEquals(123, exception.getContext().get("key2"));
    }

    @Test
    void testAddContextMap() {
        BaseException exception = new BaseException("Test message");
        
        Map<String, Object> contextData = new HashMap<>();
        contextData.put("batch", "B001");
        contextData.put("count", 100);
        
        exception.addContext(contextData);
        
        assertEquals(2, exception.getContext().size());
        assertEquals("B001", exception.getContext().get("batch"));
        assertEquals(100, exception.getContext().get("count"));
    }

    @Test
    void testAddContextWithNull() {
        BaseException exception = new BaseException("Test message");
        
        // 添加null键应该被忽略
        exception.addContext(null, "value");
        assertTrue(exception.getContext().isEmpty());
        
        // 添加null上下文映射应该被忽略
        exception.addContext((Map<String, Object>) null);
        assertTrue(exception.getContext().isEmpty());
    }

    @Test
    void testContextImmutability() {
        BaseException exception = new BaseException("Test message");
        exception.addContext("original", "value");
        
        // 获取的上下文是副本，修改不应影响原始异常
        Map<String, Object> context = exception.getContext();
        context.put("modified", "newValue");
        
        // 原始异常的上下文不应该被修改
        assertEquals(1, exception.getContext().size());
        assertFalse(exception.getContext().containsKey("modified"));
    }

    @Test
    void testToString() {
        BaseException exception = new BaseException(
            "TEST_001", 
            "Test message", 
            "Technical details"
        );
        exception.addContext("userId", "12345");
        
        String result = exception.toString();
        
        assertTrue(result.contains("BaseException"));
        assertTrue(result.contains("TEST_001"));
        assertTrue(result.contains("Test message"));
        assertTrue(result.contains("Technical details"));
        assertTrue(result.contains("userId=12345"));
    }

    @Test
    void testBusinessException() {
        BusinessException exception = new BusinessException("Business error occurred");
        
        assertEquals("Business error occurred", exception.getMessage());
        assertEquals("BUSINESS_ERROR", exception.getErrorCode());
        assertTrue(exception instanceof BaseException);
    }

    @Test
    void testSystemException() {
        SystemException exception = new SystemException("SYS_001", "System failure");
        
        assertEquals("System failure", exception.getMessage());
        assertEquals("SYS_001", exception.getErrorCode());
        assertTrue(exception instanceof BaseException);
    }

    @Test
    void testValidationException() {
        ValidationException exception = new ValidationException("VALID_001", "Invalid parameter");
        
        assertEquals("Invalid parameter", exception.getMessage());
        assertEquals("VALID_001", exception.getErrorCode());
        assertTrue(exception instanceof BaseException);
    }

    @Test
    void testConfigExceptionIntegration() {
        ConfigException exception = new ConfigException("Configuration error", "db.url");
        
        assertTrue(exception.getMessage().contains("Configuration error"));
        assertEquals("db.url", exception.getConfigKey());
        assertEquals("CONFIG_ERROR", exception.getErrorCode());
        assertTrue(exception instanceof SystemException);
        assertTrue(exception instanceof BaseException);
        
        // 验证上下文信息
        assertEquals("db.url", exception.getContext().get("configKey"));
    }
}