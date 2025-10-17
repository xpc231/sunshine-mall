package com.xpcjsu.sunshinemall.framework.cache.core;

import com.xpcjsu.sunshinemall.framework.cache.CacheTestApplication;
import com.xpcjsu.sunshinemall.framework.cache.constant.CacheConstant;
import com.xpcjsu.sunshinemall.framework.cache.entity.TestUser;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CacheManager测试类
 * <p>
 * 注意：需要本地启动Redis服务才能运行测试
 */
@SpringBootTest(classes = CacheTestApplication.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CacheManagerTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String TEST_KEY = "test:user:1";
    private static final TestUser TEST_USER = new TestUser(1L, "张三", "zhangsan@example.com", 25);

    @BeforeEach
    void setUp() {
        // 清理测试数据
        redisTemplate.delete(TEST_KEY);
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        redisTemplate.delete(TEST_KEY);
    }

    @Test
    @Order(1)
    @DisplayName("测试基础set/get操作")
    void testBasicSetGet() {
        // 设置缓存
        cacheManager.set(TEST_KEY, TEST_USER);

        // 获取缓存
        TestUser cached = cacheManager.get(TEST_KEY, TestUser.class);

        // 验证
        assertNotNull(cached);
        assertEquals(TEST_USER.getId(), cached.getId());
        assertEquals(TEST_USER.getUsername(), cached.getUsername());
        assertEquals(TEST_USER.getEmail(), cached.getEmail());
        assertEquals(TEST_USER.getAge(), cached.getAge());
    }

    @Test
    @Order(2)
    @DisplayName("测试带过期时间的set操作")
    void testSetWithExpire() throws InterruptedException {
        // 设置缓存，3秒过期
        cacheManager.set(TEST_KEY, TEST_USER, 3L);

        // 验证缓存存在（通过get判断）
        assertNotNull(cacheManager.get(TEST_KEY));

        // 等待4秒
        Thread.sleep(4000);

        // 验证缓存已过期
        assertNull(cacheManager.get(TEST_KEY));
    }

    @Test
    @Order(3)
    @DisplayName("测试delete操作")
    void testDelete() {
        // 设置缓存
        cacheManager.set(TEST_KEY, TEST_USER);
        assertNotNull(cacheManager.get(TEST_KEY));

        // 删除缓存
        Boolean result = cacheManager.delete(TEST_KEY);

        // 验证
        assertTrue(result);
        assertNull(cacheManager.get(TEST_KEY));
        
        // 删除不存在的key，应该返回false而不是抛异常
        Boolean result2 = cacheManager.delete("不存在的key");
        assertFalse(result2);
    }

    @Test
    @Order(4)
    @DisplayName("测试批量删除操作")
    void testBatchDelete() {
        // 准备测试数据
        String key1 = "test:user:1";
        String key2 = "test:user:2";
        String key3 = "test:user:3";

        cacheManager.set(key1, new TestUser(1L, "用户1", "user1@example.com", 20));
        cacheManager.set(key2, new TestUser(2L, "用户2", "user2@example.com", 21));
        cacheManager.set(key3, new TestUser(3L, "用户3", "user3@example.com", 22));

        // 批量删除
        Long count = cacheManager.delete(Arrays.asList(key1, key2, key3));

        // 验证
        assertEquals(3L, count);
        assertNull(cacheManager.get(key1));
        assertNull(cacheManager.get(key2));
        assertNull(cacheManager.get(key3));
    }



    @Test
    @Order(5)
    @DisplayName("测试空值缓存（防穿透）")
    void testNullValueCache() {
        // 测试默认过期时间
        cacheManager.setNullValue(TEST_KEY);
        assertNotNull(cacheManager.get(TEST_KEY));
        TestUser cached = cacheManager.get(TEST_KEY, TestUser.class);
        assertNull(cached);
        
        // 清理
        cacheManager.delete(TEST_KEY);
        
        // 测试自定义过期时间
        String customKey = "test:null:custom";
        cacheManager.setNullValue(customKey, 60L);  // 60秒
        assertNotNull(cacheManager.get(customKey));
        TestUser cached2 = cacheManager.get(customKey, TestUser.class);
        assertNull(cached2);
        
        // 清理
        cacheManager.delete(customKey);
    }

    @Test
    @Order(6)
    @DisplayName("测试批量获取操作")
    void testMultiGet() {
        // 准备测试数据
        String key1 = "test:user:1";
        String key2 = "test:user:2";
        String key3 = "test:user:3";

        TestUser user1 = new TestUser(1L, "用户1", "user1@example.com", 20);
        TestUser user2 = new TestUser(2L, "用户2", "user2@example.com", 21);

        cacheManager.set(key1, user1);
        cacheManager.set(key2, user2);
        // key3不设置，测试部分命中

        // 批量获取
        List<Object> results = cacheManager.multiGet(Arrays.asList(key1, key2, key3));

        // 验证
        assertEquals(3, results.size());
        assertNotNull(results.get(0));
        assertNotNull(results.get(1));
        assertNull(results.get(2));

        // 清理
        cacheManager.delete(Arrays.asList(key1, key2));
    }

    @Test
    @Order(7)
    @DisplayName("测试increment操作")
    void testIncrement() {
        String counterKey = "test:counter:1";

        // 自增
        Long value1 = cacheManager.increment(counterKey, 1L);
        assertEquals(1L, value1);

        Long value2 = cacheManager.increment(counterKey, 5L);
        assertEquals(6L, value2);

        // 清理
        cacheManager.delete(counterKey);
    }

    @Test
    @Order(8)
    @DisplayName("测试setIfAbsent操作（幂等性控制）")
    void testSetIfAbsent() {
        String idempotentKey = "test:idempotent:order-123";
        
        // 第一次设置，应该成功
        Boolean result1 = cacheManager.setIfAbsent(idempotentKey, "1", 60L);
        assertTrue(result1);
        
        // 第二次设置同key，应该失败
        Boolean result2 = cacheManager.setIfAbsent(idempotentKey, "2", 60L);
        assertFalse(result2);
        
        // 验证值没有被覆盖
        Object value = cacheManager.get(idempotentKey);
        assertEquals("1", value);
        
        // 清理
        cacheManager.delete(idempotentKey);
    }
    
    @Test
    @Order(9)
    @DisplayName("测试hasKey操作")
    void testHasKey() {
        // key不存在
        assertFalse(cacheManager.hasKey(TEST_KEY));
        
        // 设置缓存
        cacheManager.set(TEST_KEY, TEST_USER);
        
        // key存在
        assertTrue(cacheManager.hasKey(TEST_KEY));
        
        // 清理
        cacheManager.delete(TEST_KEY);
    }

    @Test
    @Order(10)
    @DisplayName("测试decrement操作")
    void testDecrement() {
        String counterKey = "test:counter:2";

        // 先设置初始值
        cacheManager.set(counterKey, 10);

        // 自减
        Long value1 = cacheManager.decrement(counterKey, 3L);
        assertEquals(7L, value1);

        Long value2 = cacheManager.decrement(counterKey, 2L);
        assertEquals(5L, value2);

        // 清理
        cacheManager.delete(counterKey);
    }

    @Test
    @Order(9)
    @DisplayName("测试CacheKeyBuilder")
    void testCacheKeyBuilder() {
        // 基础构建
        String key1 = CacheKeyBuilder.build("user", "info", "123");
        assertEquals("sunshine-mall:user:info:123", key1);

        // 使用Object类型ID
        String key2 = CacheKeyBuilder.build("product", "detail", 456L);
        assertEquals("sunshine-mall:product:detail:456", key2);

        // 模式匹配键
        String pattern = CacheKeyBuilder.buildPattern("order", "list");
        assertEquals("sunshine-mall:order:list:*", pattern);
    }

    @Test
    @Order(10)
    @DisplayName("测试CacheKeyBuilder异常情况")
    void testCacheKeyBuilderException() {
        // 空参数
        assertThrows(IllegalArgumentException.class, () -> CacheKeyBuilder.build());

        // null参数
        assertThrows(IllegalArgumentException.class, () -> CacheKeyBuilder.build("user", null, "123"));

        // 空字符串参数
        assertThrows(IllegalArgumentException.class, () -> CacheKeyBuilder.build("user", "", "123"));

        // null ID
        assertThrows(IllegalArgumentException.class, () -> CacheKeyBuilder.build("user", "info", null));
    }
}
