package com.xpcjsu.sunshinemall.product.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁工具类
 * <p>
 * 基于Redisson实现分布式锁，用于防止并发操作导致的数据不一致问题。
 * Redisson提供了更完善的分布式锁实现，包括：
 * - 自动续期（watchdog机制）
 * - 可重入锁
 * - 更完善的异常处理
 * - 支持多种锁类型
 *
 * @author xpcjsu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLock {

    private final RedissonClient redissonClient;

    /**
     * 锁的默认过期时间（秒）
     */
    private static final long DEFAULT_LOCK_EXPIRE_TIME = 30L;

    /**
     * 锁的键前缀
     */
    private static final String LOCK_KEY_PREFIX = "lock:";

    /**
     * 获取分布式锁并执行操作
     * <p>
     * 如果获取锁失败，会抛出异常。
     * 执行完成后会自动释放锁。
     * Redisson会自动续期（watchdog机制），防止业务执行时间过长导致锁过期。
     *
     * @param lockKey 锁的键
     * @param supplier 需要执行的操作
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> supplier) {
        return executeWithLock(lockKey, DEFAULT_LOCK_EXPIRE_TIME, supplier);
    }

    /**
     * 获取分布式锁并执行操作（指定过期时间）
     * <p>
     * 注意：Redisson的watchdog机制会在锁过期前自动续期，
     * 所以即使业务执行时间超过expireTime，锁也不会过期。
     * 只有在业务执行完成后，锁才会被释放。
     */
    public <T> T executeWithLock(String lockKey, long expireTime, Supplier<T> supplier) {
        String fullLockKey = LOCK_KEY_PREFIX + lockKey;
        RLock lock = redissonClient.getLock(fullLockKey);

        try {
            // 尝试获取锁，如果获取失败会立即返回false
            boolean lockAcquired = lock.tryLock(0, expireTime, TimeUnit.SECONDS);
            if (!lockAcquired) {
                log.warn("获取分布式锁失败 - lockKey: {}", fullLockKey);
                throw new RuntimeException("获取分布式锁失败，请稍后重试");
            }

            log.debug("获取分布式锁成功 - lockKey: {}", fullLockKey);

            try {
                // 执行操作
                return supplier.get();
            } finally {
                // 释放锁（Redisson会自动处理，但显式释放更安全）
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.debug("释放分布式锁成功 - lockKey: {}", fullLockKey);
                }
            }

        } catch (InterruptedException e) {//获取锁的操作被中断
            // 恢复当前线程的中断状态
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断 - lockKey: {}", fullLockKey, e);
            throw new RuntimeException("获取分布式锁被中断", e);

        } catch (Exception e) {
            // 发生异常时也要释放锁
            if (lock.isHeldByCurrentThread()) {
                try {
                    lock.unlock();
                } catch (Exception unlockException) {
                    log.error("释放分布式锁异常 - lockKey: {}", fullLockKey, unlockException);
                }
            }
            throw e;
        }
    }

    /**
     * 获取分布式锁并执行操作（无返回值）
     *
     * @param lockKey 锁的键
     * @param runnable 需要执行的操作
     */
    public void executeWithLock(String lockKey, Runnable runnable) {
        executeWithLock(lockKey, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 获取分布式锁并执行操作（无返回值，指定过期时间）
     */
    public void executeWithLock(String lockKey, long expireTime, Runnable runnable) {
        executeWithLock(lockKey, expireTime, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 尝试获取锁（非阻塞）
     */
    public boolean tryLock(String lockKey) {
        return tryLock(lockKey, DEFAULT_LOCK_EXPIRE_TIME);
    }

    /**
     * 尝试获取锁（非阻塞，指定过期时间）
     */
    public boolean tryLock(String lockKey, long expireTime) {
        String fullLockKey = LOCK_KEY_PREFIX + lockKey;
        RLock lock = redissonClient.getLock(fullLockKey);
        
        try {
            return lock.tryLock(0, expireTime, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("尝试获取分布式锁被中断 - lockKey: {}", fullLockKey, e);
            return false;
        }
    }

}

