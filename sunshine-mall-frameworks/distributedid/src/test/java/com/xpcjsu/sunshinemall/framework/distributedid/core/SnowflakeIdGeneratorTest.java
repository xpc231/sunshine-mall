package com.xpcjsu.sunshinemall.framework.distributedid.core;

import com.xpcjsu.sunshinemall.framework.base.singleton.SingletonHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 雪花算法ID生成器测试
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
class SnowflakeIdGeneratorTest {

    private SnowflakeIdGenerator generator;

    @BeforeEach
    void setUp() {
        // 清理单例缓存，确保测试独立性
        SingletonHolder.clear();
        
        // 设置测试配置
        System.setProperty("distributedid.datacenter-id", "1");
        System.setProperty("distributedid.worker-id", "1");
        
        generator = SingletonHolder.getInstance(SnowflakeIdGenerator.class);
    }

    @AfterEach
    void tearDown() {
        // 清理系统属性
        System.clearProperty("distributedid.datacenter-id");
        System.clearProperty("distributedid.worker-id");
        
        // 清理单例缓存
        SingletonHolder.clear();
    }

    @Test
    void testNextId() {
        // 生成ID
        long id1 = generator.nextId();
        long id2 = generator.nextId();

        // 验证ID生成
        assertTrue(id1 > 0);
        assertTrue(id2 > 0);
        assertTrue(id2 > id1); // ID递增
    }

    @Test
    void testIdUniqueness() {
        // 生成1万个ID，验证唯一性
        Set<Long> idSet = new HashSet<>();
        int count = 10000;

        for (int i = 0; i < count; i++) {
            long id = generator.nextId();
            assertTrue(idSet.add(id), "ID重复: " + id);
        }

        assertEquals(count, idSet.size());
    }

    @Test
    void testConcurrentIdGeneration() throws InterruptedException {
        // 并发生成ID，验证线程安全
        int threadCount = 10;
        int idsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        Set<Long> idSet = new HashSet<>();
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < idsPerThread; j++) {
                        long id = generator.nextId();
                        synchronized (idSet) {
                            if (!idSet.add(id)) {
                                errorCount.incrementAndGet();
                                System.err.println("ID重复: " + id);
                            }
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // 验证结果
        assertEquals(0, errorCount.get(), "存在重复ID");
        assertEquals(threadCount * idsPerThread, idSet.size());
    }

    @Test
    void testParseId() {
        // 生成ID并解析
        long id = generator.nextId();
        String parsed = generator.parseId(id);

        // 验证解析结果包含必要信息
        assertNotNull(parsed);
        assertTrue(parsed.contains("ID: " + id));
        assertTrue(parsed.contains("DatacenterId: 1"));
        assertTrue(parsed.contains("WorkerId: 1"));
    }

    @Test
    void testGetWorkerId() {
        assertEquals(1, generator.getWorkerId());
    }

    @Test
    void testGetDatacenterId() {
        assertEquals(1, generator.getDatacenterId());
    }

    @Test
    void testInvalidDatacenterId() {
        // 清理单例
        SingletonHolder.clear();
        
        // 设置无效的DatacenterId
        System.setProperty("distributedid.datacenter-id", "32");
        System.setProperty("distributedid.worker-id", "1");

        // 验证异常
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> SingletonHolder.getInstance(SnowflakeIdGenerator.class)
        );

        assertTrue(exception.getMessage().contains("DatacenterId 必须在 0-31 之间"));
    }

    @Test
    void testInvalidWorkerId() {
        // 清理单例
        SingletonHolder.clear();
        
        // 设置无效的WorkerId
        System.setProperty("distributedid.datacenter-id", "1");
        System.setProperty("distributedid.worker-id", "32");

        // 验证异常
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> SingletonHolder.getInstance(SnowflakeIdGenerator.class)
        );

        assertTrue(exception.getMessage().contains("WorkerId 必须在 0-31 之间"));
    }

    @Test
    void testDefaultConfiguration() {
        // 清理单例和系统属性
        SingletonHolder.clear();
        System.clearProperty("distributedid.datacenter-id");
        System.clearProperty("distributedid.worker-id");

        // 使用默认配置创建生成器
        SnowflakeIdGenerator defaultGenerator = SingletonHolder.getInstance(SnowflakeIdGenerator.class);

        // 验证默认值
        assertEquals(0, defaultGenerator.getDatacenterId());
        assertEquals(0, defaultGenerator.getWorkerId());

        // 验证能正常生成ID
        long id = defaultGenerator.nextId();
        assertTrue(id > 0);
    }

    @Test
    void testSingletonBehavior() {
        // 验证单例行为
        SnowflakeIdGenerator instance1 = SingletonHolder.getInstance(SnowflakeIdGenerator.class);
        SnowflakeIdGenerator instance2 = SingletonHolder.getInstance(SnowflakeIdGenerator.class);

        assertSame(instance1, instance2);
    }

    @Test
    void testIdPerformance() {
        // 性能测试：生成10万个ID
        int count = 100000;
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            generator.nextId();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.printf("生成 %d 个ID耗时: %d ms, 平均: %.2f μs/ID%n",
            count, duration, (duration * 1000.0) / count);

        // 验证性能：10万个ID应在1秒内完成
        assertTrue(duration < 1000, "ID生成性能不达标");
    }
}
