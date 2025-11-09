package com.xpcjsu.sunshinemall.product.util;

import com.xpcjsu.sunshinemall.product.constant.ProductConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * 缓存过期时间管理类
 * <p>
 * 实现分层过期策略和随机过期时间，解决缓存雪崩问题
 * <p>
 * 功能：
 * 1. 分层过期策略：不同类型缓存使用不同过期时间
 * 2. 随机过期时间：在基础过期时间上增加随机值（±10%），避免大量缓存同时过期
 *
 * @author xpcjsu
 */
@Slf4j
@Component
public class CacheExpireTimeManager {

    /**
     * 随机数生成器
     */
    private static final Random RANDOM = new Random();

    /**
     * 随机波动范围（百分比）
     * 例如：0.1 表示 ±10%
     */
    private static final double RANDOM_RANGE = 0.1;

    /**
     * 缓存类型枚举
     */
    public enum CacheType {
        /** 秒杀商品详情缓存 */
        SECKILL_PRODUCT_DETAIL,
        /** 秒杀商品SKU缓存 */
        SECKILL_PRODUCT_SKU,
        /** 秒杀商品分页查询缓存 */
        SECKILL_PRODUCT_PAGE,
        /** 进行中的秒杀商品列表缓存 */
        SECKILL_PRODUCT_IN_PROGRESS,
        /** 秒杀库存缓存 */
        SECKILL_STOCK,
        /** 空值缓存（防止缓存穿透） */
        NULL_VALUE
    }

    /**
     * 获取基础过期时间（秒）
     * <p>
     * 根据缓存类型返回对应的基础过期时间
     *
     * @param cacheType 缓存类型
     * @return 基础过期时间（秒）
     */
    public long getBaseExpireTime(CacheType cacheType) {
        switch (cacheType) {
            case SECKILL_PRODUCT_DETAIL:
                return ProductConstants.Cache.SECKILL_PRODUCT_CACHE_EXPIRE; // 30分钟
            case SECKILL_PRODUCT_SKU:
                return ProductConstants.Cache.SECKILL_PRODUCT_CACHE_EXPIRE; // 30分钟
            case SECKILL_PRODUCT_PAGE:
                return ProductConstants.Cache.SECKILL_PRODUCT_PAGE_CACHE_EXPIRE; // 5分钟
            case SECKILL_PRODUCT_IN_PROGRESS:
                return ProductConstants.Cache.SECKILL_PRODUCT_IN_PROGRESS_CACHE_EXPIRE; // 5分钟
            case SECKILL_STOCK:
                // 秒杀库存缓存通常根据秒杀结束时间动态计算，这里返回默认值
                return 1800L; // 30分钟
            case NULL_VALUE:
                return ProductConstants.Cache.NULL_VALUE_CACHE_EXPIRE_TIME; // 5分钟
            default:
                log.warn("未知的缓存类型，使用默认过期时间 - cacheType: {}", cacheType);
                return 300L; // 默认5分钟
        }
    }

    /**
     * 获取随机过期时间（秒）
     * <p>
     * 在基础过期时间上增加随机值（±10%），避免大量缓存同时过期
     * <p>
     * 公式：expireTime = baseExpireTime * (1 + random(-0.1, 0.1))
     * <p>
     * 例如：基础过期时间1800秒，随机后可能在1620-1980秒之间
     *
     * @param cacheType 缓存类型
     * @return 随机过期时间（秒），最小为1秒
     */
    public long getRandomExpireTime(CacheType cacheType) {
        long baseExpireTime = getBaseExpireTime(cacheType);
        return getRandomExpireTime(baseExpireTime);
    }

    /**
     * 获取随机过期时间（秒）
     * <p>
     * 在指定基础过期时间上增加随机值（±10%）
     *
     * @param baseExpireTime 基础过期时间（秒）
     * @return 随机过期时间（秒），最小为1秒
     */
    public long getRandomExpireTime(long baseExpireTime) {
        if (baseExpireTime <= 0) {
            log.warn("基础过期时间无效，使用默认值 - baseExpireTime: {}", baseExpireTime);
            return 300L; // 默认5分钟
        }

        // 计算随机波动值：-RANDOM_RANGE 到 +RANDOM_RANGE
        // 例如：RANDOM_RANGE = 0.1，则波动范围为 -0.1 到 +0.1
        double randomFactor = (RANDOM.nextDouble() * 2 - 1) * RANDOM_RANGE;
        
        // 计算随机过期时间：baseExpireTime * (1 + randomFactor)
        long randomExpireTime = Math.round(baseExpireTime * (1 + randomFactor));
        
        // 确保最小值为1秒
        if (randomExpireTime < 1) {
            randomExpireTime = 1L;
        }

        log.debug("生成随机过期时间 - baseExpireTime: {}s, randomFactor: {}%, randomExpireTime: {}s", 
                baseExpireTime, String.format("%.2f", randomFactor * 100), randomExpireTime);
        
        return randomExpireTime;
    }

    /**
     * 获取随机过期时间（秒），支持永不过期
     * <p>
     * 如果 baseExpireTime 为 -1，表示永不过期，直接返回 -1
     *
     * @param baseExpireTime 基础过期时间（秒），-1表示永不过期
     * @return 随机过期时间（秒），-1表示永不过期
     */
    public long getRandomExpireTimeWithNeverExpire(long baseExpireTime) {
        if (baseExpireTime == -1L) {
            return -1L; // 永不过期
        }
        return getRandomExpireTime(baseExpireTime);
    }

    /**
     * 获取空值缓存的随机过期时间（秒）
     * <p>
     * 空值缓存使用较短的过期时间，并添加随机值
     *
     * @param baseExpireTime 基础过期时间（秒），如果为-1则使用默认值
     * @return 随机过期时间（秒）
     */
    public long getNullValueRandomExpireTime(long baseExpireTime) {
        long nullValueBaseExpireTime;
        
        if (baseExpireTime == -1L) {
            // 如果基础过期时间为-1（永不过期），使用默认空值缓存过期时间
            nullValueBaseExpireTime = ProductConstants.Cache.NULL_VALUE_CACHE_EXPIRE_TIME;
        } else {
            // 使用基础过期时间的1/6，但不超过默认空值缓存过期时间
            nullValueBaseExpireTime = Math.min(baseExpireTime / 6, ProductConstants.Cache.NULL_VALUE_CACHE_EXPIRE_TIME);
        }
        
        return getRandomExpireTime(nullValueBaseExpireTime);
    }
}

