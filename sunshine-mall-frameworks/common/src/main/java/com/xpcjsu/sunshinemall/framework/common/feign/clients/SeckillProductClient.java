package com.xpcjsu.sunshinemall.framework.common.feign.clients;

import com.xpcjsu.sunshinemall.framework.common.feign.fallback.SeckillProductClientFallbackFactory;
import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 秒杀商品 Feign客户端
 *
 * @author xpcjsu
 */
@FeignClient(
        name = "product-service",
        contextId = "seckillProductClient",
        fallbackFactory = SeckillProductClientFallbackFactory.class
)
public interface SeckillProductClient {

    /**
     * 根据SKU ID获取秒杀商品
     *
     * @param skuId SKU ID
     * @return 秒杀商品信息（使用Map接收，因为SeckillProductDTO在common模块中可能不存在）
     */
    @GetMapping("/api/seckill/product/sku/{skuId}")
    Result<Map<String, Object>> getSeckillProductBySkuId(@PathVariable("skuId") Long skuId);

    /**
     * 扣减秒杀库存
     *
     * @param seckillProductId 秒杀商品ID
     * @param quantity 扣减数量
     * @return 操作结果
     */
    @PostMapping("/api/seckill/product/{seckillProductId}/deduct-stock")
    Result<Void> deductSeckillStock(@PathVariable("seckillProductId") Long seckillProductId,
                                    @RequestParam("quantity") Integer quantity);

    /**
     * 回滚秒杀库存
     *
     * @param seckillProductId 秒杀商品ID
     * @param quantity 回滚数量
     * @return 操作结果
     */
    @PostMapping("/api/seckill/product/{seckillProductId}/rollback-stock")
    Result<Void> rollbackSeckillStock(@PathVariable("seckillProductId") Long seckillProductId,
                                     @RequestParam("quantity") Integer quantity);

}

