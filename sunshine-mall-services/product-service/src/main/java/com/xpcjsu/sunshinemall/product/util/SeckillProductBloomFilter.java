package com.xpcjsu.sunshinemall.product.util;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.xpcjsu.sunshinemall.product.mapper.SeckillProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 秒杀商品布隆过滤器
 * <p>
 * 功能：
 * 1. 防止缓存穿透：在查询前先检查布隆过滤器，如果不存在则直接返回
 * 2. 减少数据库查询：过滤掉大部分不存在的ID请求
 * 3. 自动初始化：应用启动时自动加载所有秒杀商品ID到布隆过滤器
 * <p>
 * 原理：
 * - 布隆过滤器是一种概率型数据结构，可以快速判断元素是否"可能存在"或"一定不存在"
 * - 优点：空间效率高，查询速度快
 * - 缺点：可能存在误判（将不存在的元素判断为存在），但不会将存在的元素判断为不存在
 * - 适用场景：缓存穿透防护、防止恶意查询
 *
 * @author xpcjsu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillProductBloomFilter implements CommandLineRunner {

    private final SeckillProductMapper seckillProductMapper;

    /**
     * 布隆过滤器实例
     * <p>
     * 参数说明：
     * - expectedInsertions: 预期插入的元素数量（10万）
     * - fpp: 误判率（0.01，即1%）
     */
    private final BloomFilter<String> bloomFilter = BloomFilter.create(
            Funnels.stringFunnel(StandardCharsets.UTF_8),
            100_000, // 预期插入10万个元素（秒杀商品通常不会太多）
            0.01 // 误判率1%
    );

    /**
     * 布隆过滤器初始化标志
     */
    private volatile boolean initialized = false;

    /**
     * 应用启动时自动初始化布隆过滤器
     * <p>
     * 从数据库加载所有秒杀商品ID和SKU ID，添加到布隆过滤器中
     */
    @Override
    public void run(String... args) {
        try {
            log.info("开始初始化秒杀商品布隆过滤器...");
            initializeBloomFilter();
            log.info("秒杀商品布隆过滤器初始化完成 - 已加载 {} 个元素", getEstimatedSize());
        } catch (Exception e) {
            log.error("初始化秒杀商品布隆过滤器失败", e);
        }
    }

    /**
     * 初始化布隆过滤器
     * <p>
     * 加载所有秒杀商品ID和SKU ID到布隆过滤器
     */
    public void initializeBloomFilter() {
        if (initialized) {
            log.warn("布隆过滤器已经初始化，跳过重复初始化");
            return;
        }

        try {
            // 查询所有秒杀商品ID
            List<Long> seckillProductIds = seckillProductMapper.selectAllSeckillProductIds();
            log.info("查询到 {} 个秒杀商品ID", seckillProductIds.size());

            // 查询所有秒杀商品的SKU ID
            List<Long> skuIds = seckillProductMapper.selectAllSkuIds();
            log.info("查询到 {} 个秒杀商品SKU ID", skuIds.size());

            // 添加到布隆过滤器
            int count = 0;
            for (Long id : seckillProductIds) {
                bloomFilter.put("seckill:" + id);
                count++;
            }

            for (Long skuId : skuIds) {
                bloomFilter.put("sku:" + skuId);
                count++;
            }

            initialized = true;
            log.info("布隆过滤器初始化完成 - 共加载 {} 个元素", count);
        } catch (Exception e) {
            log.error("初始化布隆过滤器失败", e);
            throw new RuntimeException("初始化布隆过滤器失败", e);
        }
    }

    /**
     * 检查秒杀商品ID是否可能存在
     * <p>
     * 第二道防线：布隆过滤器拦截，如果不存在则直接返回false
     * <p>
     * 注意：布隆过滤器可能误判（将不存在的判断为存在），但不会将存在的判断为不存在
     * 因此，如果返回false，则一定不存在；如果返回true，则可能存在
     *
     * @param id 秒杀商品ID
     * @return true-可能存在, false-一定不存在
     */
    public boolean mightExist(Long id) {
        if (!initialized) {
            log.warn("布隆过滤器未初始化，返回true允许查询");
            return true; // 未初始化时，允许查询（降级处理）
        }

        if (id == null) {
            return false;
        }

        boolean mightExist = bloomFilter.mightContain("seckill:" + id);
        if (!mightExist) {
            log.debug("布隆过滤器拦截 - 秒杀商品ID不存在: {}", id);
        }
        return mightExist;
    }

    /**
     * 检查SKU ID是否可能存在
     *
     * @param skuId SKU ID
     * @return true-可能存在, false-一定不存在
     */
    public boolean skuMightExist(Long skuId) {
        if (!initialized) {
            log.warn("布隆过滤器未初始化，返回true允许查询");
            return true; // 未初始化时，允许查询（降级处理）
        }

        if (skuId == null) {
            return false;
        }

        boolean mightExist = bloomFilter.mightContain("sku:" + skuId);
        if (!mightExist) {
            log.debug("布隆过滤器拦截 - SKU ID不存在: {}", skuId);
        }
        return mightExist;
    }

    /**
     * 添加秒杀商品ID到布隆过滤器
     * <p>
     * 当创建新的秒杀商品时，需要添加到布隆过滤器
     *
     * @param id 秒杀商品ID
     */
    public void addSeckillProductId(Long id) {
        if (id != null && initialized) {
            bloomFilter.put("seckill:" + id);
            log.debug("添加秒杀商品ID到布隆过滤器 - id: {}", id);
        }
    }

    /**
     * 添加SKU ID到布隆过滤器
     * <p>
     * 当创建新的秒杀商品时，需要添加SKU ID到布隆过滤器
     *
     * @param skuId SKU ID
     */
    public void addSkuId(Long skuId) {
        if (skuId != null && initialized) {
            bloomFilter.put("sku:" + skuId);
            log.debug("添加SKU ID到布隆过滤器 - skuId: {}", skuId);
        }
    }

    /**
     * 获取布隆过滤器的预估元素数量
     * <p>
     * 注意：这是预估值，不是精确值
     *
     * @return 预估元素数量
     */
    public long getEstimatedSize() {
        // Guava的BloomFilter没有直接提供获取元素数量的方法
        // 这里返回一个估算值（实际应该维护一个计数器）
        return bloomFilter.approximateElementCount();
    }

    /**
     * 检查布隆过滤器是否已初始化
     *
     * @return true-已初始化, false-未初始化
     */
    public boolean isInitialized() {
        return initialized;
    }
}

