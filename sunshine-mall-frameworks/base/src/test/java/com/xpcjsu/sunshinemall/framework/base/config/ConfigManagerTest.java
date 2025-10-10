package com.xpcjsu.sunshinemall.framework.base.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConfigManager测试类
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
class ConfigManagerTest {

    private ConfigManager configManager;

    @BeforeEach
    void setUp() {
        configManager = new ConfigManager();
        // 清理系统属性
        System.clearProperty("test.string.value");
        System.clearProperty("test.int.value");
        System.clearProperty("test.boolean.value");
        System.clearProperty("test.long.value");
        System.clearProperty("test.double.value");
    }

    @AfterEach
    void tearDown() {
        // 清理缓存
        configManager.clearCache();
        // 清理系统属性
        System.clearProperty("test.string.value");
        System.clearProperty("test.int.value");
        System.clearProperty("test.boolean.value");
        System.clearProperty("test.long.value");
        System.clearProperty("test.double.value");
    }

    @Test
    void testGetString() {
        // 测试默认值
        String defaultValue = configManager.getString("test.string.value", "default");
        assertEquals("default", defaultValue);

        // 设置系统属性
        System.setProperty("test.string.value", "test-value");
        String actualValue = configManager.getString("test.string.value", "default");
        assertEquals("test-value", actualValue);
    }

    @Test
    void testGetStringOptional() {
        // 测试不存在的配置
        Optional<String> emptyValue = configManager.getStringOptional("test.nonexistent.value");
        assertTrue(emptyValue.isEmpty());

        // 设置系统属性
        System.setProperty("test.string.value", "test-value");
        Optional<String> actualValue = configManager.getStringOptional("test.string.value");
        assertTrue(actualValue.isPresent());
        assertEquals("test-value", actualValue.get());
    }

    @Test
    void testGetInt() {
        // 测试默认值
        Integer defaultValue = configManager.getInt("test.int.value", 100);
        assertEquals(100, defaultValue);

        // 设置有效的整数值
        System.setProperty("test.int.value", "200");
        Integer actualValue = configManager.getInt("test.int.value", 100);
        assertEquals(200, actualValue);

        // 设置无效的整数值
        System.setProperty("test.int.value", "invalid");
        Integer invalidValue = configManager.getInt("test.int.value", 100);
        assertEquals(100, invalidValue); // 应该返回默认值
    }

    @Test
    void testGetIntOptional() {
        // 测试不存在的配置
        Optional<Integer> emptyValue = configManager.getIntOptional("test.nonexistent.value");
        assertTrue(emptyValue.isEmpty());

        // 设置有效值
        System.setProperty("test.int.value", "300");
        Optional<Integer> actualValue = configManager.getIntOptional("test.int.value");
        assertTrue(actualValue.isPresent());
        assertEquals(300, actualValue.get());
    }

    @Test
    void testGetLong() {
        // 测试默认值
        Long defaultValue = configManager.getLong("test.long.value", 1000L);
        assertEquals(1000L, defaultValue);

        // 设置有效的长整数值
        System.setProperty("test.long.value", "2000");
        Long actualValue = configManager.getLong("test.long.value", 1000L);
        assertEquals(2000L, actualValue);
    }

    @Test
    void testGetLongOptional() {
        // 测试不存在的配置
        Optional<Long> emptyValue = configManager.getLongOptional("test.nonexistent.value");
        assertTrue(emptyValue.isEmpty());

        // 设置有效值
        System.setProperty("test.long.value", "3000");
        Optional<Long> actualValue = configManager.getLongOptional("test.long.value");
        assertTrue(actualValue.isPresent());
        assertEquals(3000L, actualValue.get());
    }

    @Test
    void testGetBoolean() {
        // 测试默认值
        Boolean defaultValue = configManager.getBoolean("test.boolean.value", false);
        assertEquals(false, defaultValue);

        // 测试true值
        System.setProperty("test.boolean.value", "true");
        Boolean trueValue = configManager.getBoolean("test.boolean.value", false);
        assertEquals(true, trueValue);

        // 测试1值
        System.setProperty("test.boolean.value", "1");
        Boolean oneValue = configManager.getBoolean("test.boolean.value", false);
        assertEquals(true, oneValue);

        // 测试yes值
        System.setProperty("test.boolean.value", "yes");
        Boolean yesValue = configManager.getBoolean("test.boolean.value", false);
        assertEquals(true, yesValue);

        // 测试false值
        System.setProperty("test.boolean.value", "false");
        Boolean falseValue = configManager.getBoolean("test.boolean.value", true);
        assertEquals(false, falseValue);
    }

    @Test
    void testGetBooleanOptional() {
        // 测试不存在的配置
        Optional<Boolean> emptyValue = configManager.getBooleanOptional("test.nonexistent.value");
        assertTrue(emptyValue.isEmpty());

        // 设置有效值
        System.setProperty("test.boolean.value", "true");
        Optional<Boolean> actualValue = configManager.getBooleanOptional("test.boolean.value");
        assertTrue(actualValue.isPresent());
        assertEquals(true, actualValue.get());
    }

    @Test
    void testGetDouble() {
        // 测试默认值
        Double defaultValue = configManager.getDouble("test.double.value", 10.5);
        assertEquals(10.5, defaultValue);

        // 设置有效的双精度值
        System.setProperty("test.double.value", "20.8");
        Double actualValue = configManager.getDouble("test.double.value", 10.5);
        assertEquals(20.8, actualValue);
    }

    @Test
    void testGetDoubleOptional() {
        // 测试不存在的配置
        Optional<Double> emptyValue = configManager.getDoubleOptional("test.nonexistent.value");
        assertTrue(emptyValue.isEmpty());

        // 设置有效值
        System.setProperty("test.double.value", "30.9");
        Optional<Double> actualValue = configManager.getDoubleOptional("test.double.value");
        assertTrue(actualValue.isPresent());
        assertEquals(30.9, actualValue.get());
    }

    @Test
    void testContainsKey() {
        // 测试不存在的配置
        assertFalse(configManager.containsKey("test.nonexistent.value"));

        // 设置配置
        System.setProperty("test.string.value", "test-value");
        assertTrue(configManager.containsKey("test.string.value"));
    }

    @Test
    void testCacheSize() {
        // 初始缓存为空
        assertEquals(0, configManager.getCacheSize());

        // 读取配置后缓存增加
        configManager.getString("test.key1", "default");
        assertEquals(1, configManager.getCacheSize());

        configManager.getInt("test.key2", 100);
        assertEquals(2, configManager.getCacheSize());

        // 清空缓存
        configManager.clearCache();
        assertEquals(0, configManager.getCacheSize());
    }

    @Test
    void testInvalidKey() {
        // 测试null键
        assertThrows(IllegalArgumentException.class, () -> 
            configManager.getString(null, "default"));

        // 测试空键
        assertThrows(IllegalArgumentException.class, () -> 
            configManager.getString("", "default"));

        // 测试空白键
        assertThrows(IllegalArgumentException.class, () -> 
            configManager.getString("   ", "default"));
    }

    @Test
    void testConfigurationPriority() {
        // 设置系统属性
        System.setProperty("test.priority.value", "system-property");
        
        // 系统属性应该有最高优先级
        String value = configManager.getString("test.priority.value", "default");
        assertEquals("system-property", value);
    }

    @Test
    void testConfigCaching() {
        // 第一次读取
        String value1 = configManager.getString("test.cache.value", "default");
        assertEquals("default", value1);

        // 设置系统属性（但缓存中已有值）
        System.setProperty("test.cache.value", "new-value");
        
        // 再次读取，应该返回缓存中的值
        String value2 = configManager.getString("test.cache.value", "default");
        assertEquals("default", value2);

        // 清空缓存后再读取
        configManager.clearCache();
        String value3 = configManager.getString("test.cache.value", "default");
        assertEquals("new-value", value3);
    }
}