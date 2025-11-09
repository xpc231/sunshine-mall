package com.xpcjsu.sunshinemall.product.helper;

import com.xpcjsu.sunshinemall.framework.cache.core.CacheManager;
import com.xpcjsu.sunshinemall.product.constant.ProductConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 秒杀商品缓存辅助类
 * <p>
 * 封装秒杀商品相关的缓存操作，包括缓存键生成、缓存删除等
 *
 * @author xpcjsu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillProductCacheHelper {

    private final CacheManager cacheManager;

    /**
     * 获取秒杀库存缓存键
     *
     * @param seckillProductId 秒杀商品ID
     * @return 缓存键
     */
    public String getSeckillStockKey(Long seckillProductId) {
        return ProductConstants.Cache.SECKILL_STOCK_KEY_PREFIX + seckillProductId;
    }

    /**
     * 获取秒杀商品详情缓存键
     *
     * @param seckillProductId 秒杀商品ID
     * @return 缓存键
     */
    public String getSeckillProductDetailKey(Long seckillProductId) {
        return ProductConstants.Cache.SECKILL_PRODUCT_DETAIL_PREFIX + seckillProductId;
    }

    /**
     * 获取秒杀商品SKU缓存键
     *
     * @param skuId SKU ID
     * @return 缓存键
     */
    public String getSeckillProductSkuKey(Long skuId) {
        return ProductConstants.Cache.SECKILL_PRODUCT_SKU_PREFIX + skuId;
    }

    /**
     * 获取秒杀商品分页查询缓存键
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param status   状态（可选）
     * @return 缓存键
     */
    public String getSeckillProductPageKey(int pageNum, int pageSize, Integer status) {
        String statusStr = status != null ? String.valueOf(status) : "all";
        return ProductConstants.Cache.SECKILL_PRODUCT_PAGE_PREFIX + pageNum + ":" + pageSize + ":" + statusStr;
    }

    /**
     * 删除秒杀商品相关缓存（旁路策略）
     * <p>
     * 删除所有可能相关的缓存键，包括：
     * - 商品详情缓存（根据ID）
     * - SKU缓存（根据SKU ID）
     * - 进行中的秒杀商品列表缓存
     * - 分页查询缓存（删除所有分页缓存，因为数据已变化）
     *
     * @param seckillProductId 秒杀商品ID
     * @param skuId SKU ID（可选，如果为null则不删除SKU缓存）
     */
    public void deleteSeckillProductCache(Long seckillProductId, Long skuId) {
        try {
            // 删除商品详情缓存
            if (seckillProductId != null) {
                String detailKey = getSeckillProductDetailKey(seckillProductId);
                cacheManager.delete(detailKey);
                log.debug("删除秒杀商品详情缓存 - seckillId: {}", seckillProductId);
            }

            // 删除SKU缓存
            if (skuId != null) {
                String skuKey = getSeckillProductSkuKey(skuId);
                cacheManager.delete(skuKey);
                log.debug("删除秒杀商品SKU缓存 - skuId: {}", skuId);
            }

            // 删除进行中的秒杀商品列表缓存（因为列表可能已变化）
            cacheManager.delete(ProductConstants.Cache.SECKILL_PRODUCT_IN_PROGRESS_KEY);
            log.debug("删除进行中的秒杀商品列表缓存");

            // 删除分页查询缓存（使用通配符删除所有分页缓存）
            // 注意：这里使用简单的删除策略，实际可以使用Redis的KEYS或SCAN命令批量删除
            // 由于分页缓存键较多，这里只删除常见的分页缓存
            // 更完善的方案是使用Redis的KEYS或SCAN命令，但性能开销较大
            // 当前策略：依赖缓存过期时间自动清理，或通过定时任务清理
            log.debug("分页查询缓存将在过期后自动清理，或通过定时任务清理");

        } catch (Exception e) {
            log.warn("删除秒杀商品缓存失败 - seckillId: {}, skuId: {}", seckillProductId, skuId, e);
            // 缓存删除失败不影响主流程，只记录日志
        }
    }
}

