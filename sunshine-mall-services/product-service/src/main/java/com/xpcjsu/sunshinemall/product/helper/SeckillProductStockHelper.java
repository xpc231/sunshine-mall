package com.xpcjsu.sunshinemall.product.helper;

import com.xpcjsu.sunshinemall.framework.cache.core.CacheManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 秒杀商品库存辅助类
 * <p>
 * 封装秒杀商品库存相关的Redis操作，包括预扣减、回滚等
 *
 * @author xpcjsu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillProductStockHelper {

    private final CacheManager cacheManager;

    /**
     * 秒杀库存扣减Lua脚本
     * 功能：检查库存是否充足，如果充足则扣减，否则返回-1
     * 返回值：扣减后的库存数量（>=0表示成功，-1表示库存不足）
     */
    private static final String DEDUCT_STOCK_LUA_SCRIPT =
            "local stock = redis.call('get', KEYS[1])\n" +
            "if stock == false then\n" +
            "    return -1\n" +
            "end\n" +
            "local stockNum = tonumber(stock)\n" +
            "local deductNum = tonumber(ARGV[1])\n" +
            "if stockNum < deductNum then\n" +
            "    return -1\n" +
            "end\n" +
            "local remaining = redis.call('decrby', KEYS[1], deductNum)\n" +
            "return remaining";

    /**
     * Redis预扣减（使用Lua脚本保证原子性）
     *
     * @param stockKey 库存缓存键
     * @param quantity 扣减数量
     * @return 扣减后的剩余库存（>=0表示成功，-1表示库存不足，null表示Redis异常）
     */
    public Long tryRedisPreDeduct(String stockKey, Integer quantity) {
        try {
            List<String> keys = new ArrayList<>();
            keys.add(stockKey);
            List<Object> args = new ArrayList<>();
            args.add(quantity);

            Long result = cacheManager.executeScriptAsLong(DEDUCT_STOCK_LUA_SCRIPT, keys, args);
            return result;
        } catch (Exception e) {
            log.warn("Redis预扣减异常，降级到数据库 - stockKey: {}, quantity: {}", stockKey, quantity, e);
            return null; // 返回null表示Redis异常，降级到数据库
        }
    }

    /**
     * 回滚Redis库存
     *
     * @param stockKey 库存缓存键
     * @param quantity 回滚数量
     */
    public void rollbackRedisStock(String stockKey, Integer quantity) {
        try {
            cacheManager.increment(stockKey, quantity.longValue());
            log.debug("回滚Redis库存成功 - stockKey: {}, quantity: {}", stockKey, quantity);
        } catch (Exception e) {
            log.error("回滚Redis库存失败 - stockKey: {}, quantity: {}", stockKey, quantity, e);
            // 回滚失败不影响主流程，记录日志即可
        }
    }
}

