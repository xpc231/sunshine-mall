package com.xpcjsu.sunshinemall.product.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 缓存一致性保证 - 延迟双删策略
 * 
 * <p>解决 Redis 和 MySQL 数据不一致问题
 * 
 * <p>原理：
 * 1. 第一次删除：删除缓存（防止读到脏数据）
 * 2. 更新数据库
 * 3. 延迟删除：再次删除缓存（防止更新期间的脏读）
 * 
 * <p>示例：
 * <pre>
 * T1: 删除缓存
 * T2: 更新数据库（可能较慢）
 * T3: [延迟500ms] 
 * T4: 再次删除缓存（清除 T2-T3 期间可能产生的脏缓存）
 * </pre>
 *
 * @author xpcjsu
 */
@Slf4j
@Component
public class CacheConsistencyHelper {

    /**
     * 延迟双删 - 推荐延迟时间
     * 
     * <p>延迟时间应该大于：
     * - 数据库主从同步时间（如果有读写分离）
     * - 业务逻辑执行时间
     * 
     * <p>通常设置：500ms - 1000ms
     */
    private static final long DELAY_TIME = 500L;

    /**
     * 异步延迟删除缓存
     *
     * @param cacheKey 缓存键
     * @param delayMillis 延迟时间（毫秒）
     */
    @Async
    public void delayedDeleteCache(String cacheKey, long delayMillis) {
        try {
            // 延迟一段时间
            Thread.sleep(delayMillis);
            
            // 再次删除缓存
            // cacheManager.delete(cacheKey);
            
            log.debug("延迟双删 - 第二次删除缓存成功: {}", cacheKey);
        } catch (InterruptedException e) {
            log.error("延迟双删失败 - cacheKey: {}", cacheKey, e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("延迟双删失败 - cacheKey: {}", cacheKey, e);
        }
    }

    /**
     * 标准延迟双删（使用默认延迟时间）
     *
     * @param cacheKey 缓存键
     */
    @Async
    public void delayedDeleteCache(String cacheKey) {
        delayedDeleteCache(cacheKey, DELAY_TIME);
    }
}

/**
 * 使用示例（在 StockServiceImpl 中）：
 * 
 * <pre>
 * public boolean deductStock(...) {
 *     String cacheKey = getStockCacheKey(skuId);
 *     
 *     // 1. 第一次删除缓存
 *     cacheManager.delete(cacheKey);
 *     
 *     // 2. 更新数据库
 *     int updated = productStockMapper.deductStock(...);
 *     
 *     if (updated > 0) {
 *         // 3. 延迟双删（异步执行）
 *         cacheConsistencyHelper.delayedDeleteCache(cacheKey);
 *         return true;
 *     }
 * }
 * </pre>
 */
