package com.xpcjsu.sunshinemall.framework.idempotent.aspect;

import com.xpcjsu.sunshinemall.framework.cache.core.CacheManager;
import com.xpcjsu.sunshinemall.framework.idempotent.annotation.Idempotent;
import com.xpcjsu.sunshinemall.framework.idempotent.exception.IdempotentException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 幂等性切面测试
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
@SpringBootTest(classes = IdempotentAspectTest.TestConfig.class)
class IdempotentAspectTest {

    @Autowired
    private TestService testService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        // 清理测试数据
        cacheManager.delete("idempotent:order-123");
        cacheManager.delete("idempotent:user-456");
        cacheManager.delete("test:custom-key");
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        cacheManager.delete("idempotent:order-123");
        cacheManager.delete("idempotent:user-456");
        cacheManager.delete("test:custom-key");
    }

    @Test
    void testIdempotent() {
        // 第一次调用应该成功
        String result1 = testService.createOrder("order-123");
        assertEquals("订单创建成功: order-123", result1);

        // 第二次调用应该抛出幂等异常
        IdempotentException exception = assertThrows(
            IdempotentException.class,
            () -> testService.createOrder("order-123")
        );
        assertEquals("请勿重复提交", exception.getMessage());
    }

    @Test
    void testDifferentKeys() {
        // 不同的key应该可以分别执行
        String result1 = testService.createOrder("order-123");
        String result2 = testService.createOrder("order-456");

        assertEquals("订单创建成功: order-123", result1);
        assertEquals("订单创建成功: order-456", result2);
    }

    @Test
    void testCustomMessage() {
        // 第一次调用成功
        testService.updateUser(456L);

        // 第二次调用抛出自定义异常消息
        IdempotentException exception = assertThrows(
            IdempotentException.class,
            () -> testService.updateUser(456L)
        );
        assertEquals("用户信息更新中，请稍后再试", exception.getMessage());
    }

    @Test
    void testCustomPrefix() {
        // 第一次调用成功
        String result1 = testService.customPrefix("custom-key");
        assertEquals("自定义前缀成功", result1);

        // 验证使用了自定义前缀
        assertTrue(cacheManager.hasKey("test:custom-key"));

        // 第二次调用失败
        assertThrows(
            IdempotentException.class,
            () -> testService.customPrefix("custom-key")
        );
    }

    @Test
    void testExpireTime() throws InterruptedException {
        // 第一次调用成功
        testService.shortExpire("short-key");

        // 第二次调用失败
        assertThrows(
            IdempotentException.class,
            () -> testService.shortExpire("short-key")
        );

        // 等待2秒后过期
        TimeUnit.SECONDS.sleep(3);

        // 过期后可以再次调用
        String result = testService.shortExpire("short-key");
        assertEquals("短期幂等成功", result);
    }

    @Test
    void testBusinessException() {
        // 业务异常时应该删除幂等键，允许重试
        assertThrows(
            RuntimeException.class,
            () -> testService.businessError("error-key")
        );

        // 幂等键已被删除，可以重试
        assertThrows(
            RuntimeException.class,
            () -> testService.businessError("error-key")
        );
    }

    @Test
    void testObjectParameter() {
        TestOrder order = new TestOrder("order-789", 100L);
        
        // 第一次调用成功
        String result1 = testService.createOrderWithObject(order);
        assertEquals("订单创建成功: order-789", result1);

        // 第二次调用失败
        assertThrows(
            IdempotentException.class,
            () -> testService.createOrderWithObject(order)
        );
    }

    /**
     * 测试服务类
     */
    @Service
    static class TestService {

        @Idempotent(key = "#orderId")
        public String createOrder(String orderId) {
            return "订单创建成功: " + orderId;
        }

        @Idempotent(key = "#userId", message = "用户信息更新中，请稍后再试")
        public void updateUser(Long userId) {
            // 模拟业务逻辑
        }

        @Idempotent(key = "#key", prefix = "test")
        public String customPrefix(String key) {
            return "自定义前缀成功";
        }

        @Idempotent(key = "#key", expireTime = 2, timeUnit = TimeUnit.SECONDS)
        public String shortExpire(String key) {
            return "短期幂等成功";
        }

        @Idempotent(key = "#key")
        public String businessError(String key) {
            throw new RuntimeException("业务异常");
        }

        @Idempotent(key = "#order.orderId")
        public String createOrderWithObject(TestOrder order) {
            return "订单创建成功: " + order.getOrderId();
        }
    }

    /**
     * 测试订单对象
     */
    static class TestOrder {
        private String orderId;
        private Long userId;

        public TestOrder(String orderId, Long userId) {
            this.orderId = orderId;
            this.userId = userId;
        }

        public String getOrderId() {
            return orderId;
        }

        public Long getUserId() {
            return userId;
        }
    }

    /**
     * 测试配置类
     */
    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.context.annotation.Import({
        com.xpcjsu.sunshinemall.framework.idempotent.config.IdempotentConfig.class,
        com.xpcjsu.sunshinemall.framework.cache.config.RedisConfig.class
    })
    static class TestConfig {
    }
}
