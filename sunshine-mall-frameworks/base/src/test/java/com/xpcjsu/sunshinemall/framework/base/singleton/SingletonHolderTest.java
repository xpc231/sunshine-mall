package com.xpcjsu.sunshinemall.framework.base.singleton;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SingletonHolder测试类
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
@Execution(ExecutionMode.CONCURRENT)
class SingletonHolderTest {

    @BeforeEach
    void setUp() {
        // 每个测试前清空实例
        SingletonHolder.clear();
    }

    @AfterEach
    void tearDown() {
        // 每个测试后清空实例
        SingletonHolder.clear();
    }

    @Test
    void testGetInstanceWithSupplier() {
        // 测试使用Supplier创建实例
        TestService instance1 = SingletonHolder.getInstance(TestService.class, TestService::new);
        TestService instance2 = SingletonHolder.getInstance(TestService.class, TestService::new);
        
        assertNotNull(instance1);
        assertNotNull(instance2);
        assertSame(instance1, instance2, "应该返回同一个实例");
        assertEquals(1, SingletonHolder.size());
    }

    @Test
    void testGetInstanceWithDefaultConstructor() {
        // 测试使用默认构造器创建实例
        TestService instance1 = SingletonHolder.getInstance(TestService.class);
        TestService instance2 = SingletonHolder.getInstance(TestService.class);
        
        assertNotNull(instance1);
        assertNotNull(instance2);
        assertSame(instance1, instance2, "应该返回同一个实例");
        assertEquals(1, SingletonHolder.size());
    }

    @Test
    void testContainsInstance() {
        // 测试实例存在性检查
        assertFalse(SingletonHolder.containsInstance(TestService.class));
        
        SingletonHolder.getInstance(TestService.class);
        assertTrue(SingletonHolder.containsInstance(TestService.class));
    }

    @Test
    void testRemoveInstance() {
        // 测试移除实例
        TestService instance = SingletonHolder.getInstance(TestService.class);
        assertNotNull(instance);
        assertEquals(1, SingletonHolder.size());
        
        TestService removed = SingletonHolder.removeInstance(TestService.class);
        assertSame(instance, removed, "应该返回被移除的实例");
        assertEquals(0, SingletonHolder.size());
        assertFalse(SingletonHolder.containsInstance(TestService.class));
    }

    @Test
    void testClear() {
        // 测试清空所有实例
        SingletonHolder.getInstance(TestService.class);
        SingletonHolder.getInstance(AnotherService.class);
        assertEquals(2, SingletonHolder.size());
        
        SingletonHolder.clear();
        assertEquals(0, SingletonHolder.size());
        assertFalse(SingletonHolder.containsInstance(TestService.class));
        assertFalse(SingletonHolder.containsInstance(AnotherService.class));
    }

    @Test
    void testNullParameterHandling() {
        // 测试null参数处理
        assertThrows(IllegalArgumentException.class, () -> 
            SingletonHolder.getInstance(null, TestService::new));
            
        assertThrows(IllegalArgumentException.class, () -> 
            SingletonHolder.getInstance(TestService.class, null));
            
        assertFalse(SingletonHolder.containsInstance(null));
        assertNull(SingletonHolder.removeInstance(null));
    }

    @Test
    void testConstructorException() {
        // 测试构造器异常处理
        assertThrows(RuntimeException.class, () -> 
            SingletonHolder.getInstance(InvalidService.class));
    }

    @Test
    void testSupplierException() {
        // 测试Supplier异常处理
        assertThrows(RuntimeException.class, () -> 
            SingletonHolder.getInstance(TestService.class, () -> {
                throw new RuntimeException("Test exception");
            }));
    }

    @Test
    void testThreadSafety() throws InterruptedException {
        // 测试线程安全性
        final int threadCount = 10;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicInteger createdCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    SingletonHolder.getInstance(TestService.class, () -> {
                        createdCount.incrementAndGet();
                        return new TestService();
                    });
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        assertEquals(1, createdCount.get(), "只应该创建一个实例");
        assertEquals(1, SingletonHolder.size());
    }

    // 测试用的服务类
    public static class TestService {
        public TestService() {
            // 默认构造器
        }
    }

    public static class AnotherService {
        public AnotherService() {
            // 默认构造器
        }
    }

    // 没有无参构造器的类，用于测试异常情况
    public static class InvalidService {
        public InvalidService(String param) {
            // 只有有参构造器
        }
    }
}