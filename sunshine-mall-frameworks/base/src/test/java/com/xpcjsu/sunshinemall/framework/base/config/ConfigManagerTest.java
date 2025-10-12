package com.xpcjsu.sunshinemall.framework.base.config;

import com.xpcjsu.sunshinemall.framework.base.singleton.SingletonHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        // 使用SingletonHolder获取ConfigManager实例
        configManager = SingletonHolder.getInstance(ConfigManager.class);
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
        // 清理SingletonHolder缓存（测试专用）
        SingletonHolder.clear();
    }

    @Test
    void testGetString() {
        // 测试默认值
        String defaultValue = configManager.getString("test.string.value", "default");
        assertEquals("default", defaultValue);

        // 清空缓存后设置系统属性
        configManager.clearCache();
        System.setProperty("test.string.value", "test-value");
        String actualValue = configManager.getString("test.string.value", "default");
        assertEquals("test-value", actualValue);
    }

    @Test
    void testGetInt() {
        // 测试默认值
        Integer defaultValue = configManager.getInt("test.int.value", 100);
        assertEquals(100, defaultValue);

        // 清空缓存后设置有效的整数值
        configManager.clearCache();
        System.setProperty("test.int.value", "200");
        Integer actualValue = configManager.getInt("test.int.value", 100);
        assertEquals(200, actualValue);

        // 清空缓存以测试新值
        configManager.clearCache();
        
        // 设置无效的整数值，应该抛出ConfigException
        System.setProperty("test.int.value", "invalid");
        ConfigException exception = assertThrows(ConfigException.class, () -> 
            configManager.getInt("test.int.value", 100));
        assertEquals("test.int.value", exception.getConfigKey());
        assertTrue(exception.getMessage().contains("配置值格式错误"));
    }

    @Test
    void testGetLong() {
        // 测试默认值
        Long defaultValue = configManager.getLong("test.long.value", 1000L);
        assertEquals(1000L, defaultValue);

        // 清空缓存后设置有效的长整数值
        configManager.clearCache();
        System.setProperty("test.long.value", "2000");
        Long actualValue = configManager.getLong("test.long.value", 1000L);
        assertEquals(2000L, actualValue);
        
        // 清空缓存以测试新值
        configManager.clearCache();
        
        // 设置无效的长整数值，应该抛出ConfigException
        System.setProperty("test.long.value", "invalid");
        ConfigException exception = assertThrows(ConfigException.class, () -> 
            configManager.getLong("test.long.value", 1000L));
        assertEquals("test.long.value", exception.getConfigKey());
        assertTrue(exception.getMessage().contains("配置值格式错误"));
    }

    @Test
    void testGetBoolean() {
        // 测试默认值
        Boolean defaultValue = configManager.getBoolean("test.boolean.value", false);
        assertEquals(false, defaultValue);

        // 清空缓存后测试true值
        configManager.clearCache();
        System.setProperty("test.boolean.value", "true");
        Boolean trueValue = configManager.getBoolean("test.boolean.value", false);
        assertEquals(true, trueValue);

        // 清空缓存后测试1值
        configManager.clearCache();
        System.setProperty("test.boolean.value", "1");
        Boolean oneValue = configManager.getBoolean("test.boolean.value", false);
        assertEquals(true, oneValue);

        // 清空缓存后测试yes值
        configManager.clearCache();
        System.setProperty("test.boolean.value", "yes");
        Boolean yesValue = configManager.getBoolean("test.boolean.value", false);
        assertEquals(true, yesValue);

        // 清空缓存后测试false值
        configManager.clearCache();
        System.setProperty("test.boolean.value", "false");
        Boolean falseValue = configManager.getBoolean("test.boolean.value", true);
        assertEquals(false, falseValue);
    }

    @Test
    void testGetDouble() {
        // 测试默认值
        Double defaultValue = configManager.getDouble("test.double.value", 10.5);
        assertEquals(10.5, defaultValue);

        // 清空缓存后设置有效的双精度值
        configManager.clearCache();
        System.setProperty("test.double.value", "20.8");
        Double actualValue = configManager.getDouble("test.double.value", 10.5);
        assertEquals(20.8, actualValue);
        
        // 清空缓存以测试新值
        configManager.clearCache();
        
        // 设置无效的双精度值，应该抛出ConfigException
        System.setProperty("test.double.value", "invalid");
        ConfigException exception = assertThrows(ConfigException.class, () -> 
            configManager.getDouble("test.double.value", 10.5));
        assertEquals("test.double.value", exception.getConfigKey());
        assertTrue(exception.getMessage().contains("配置值格式错误"));
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
        ConfigException nullException = assertThrows(ConfigException.class, () -> 
            configManager.getString(null, "default"));
        assertNull(nullException.getConfigKey());
        assertTrue(nullException.getMessage().contains("配置键不能为null"));

        // 测试空键
        ConfigException emptyException = assertThrows(ConfigException.class, () -> 
            configManager.getString("", "default"));
        assertEquals("", emptyException.getConfigKey());
        assertTrue(emptyException.getMessage().contains("配置键不能为null"));

        // 测试空白键
        ConfigException blankException = assertThrows(ConfigException.class, () -> 
            configManager.getString("   ", "default"));
        assertEquals("   ", blankException.getConfigKey());
        assertTrue(blankException.getMessage().contains("配置键不能为null"));
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