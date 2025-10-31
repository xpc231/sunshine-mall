package com.xpcjsu.sunshinemall.order.client;

import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 库存服务 Feign客户端
 */
@FeignClient(name = "product-service", contextId = "stockClient")
public interface StockClient {

    /** 检查库存是否充足 */
    @GetMapping("/api/stock/check")
    Result<Boolean> checkStock(@RequestParam("skuId") Long skuId,
                               @RequestParam("quantity") Integer quantity);

    /** 预占库存 */
    @PostMapping("/api/stock/lock")
    Result<Void> lockStock(@RequestParam("skuId") Long skuId,
                           @RequestParam("quantity") Integer quantity,
                           @RequestParam(value = "orderId", required = false) Long orderId,
                           @RequestParam(value = "remark", required = false) String remark);

    /** 释放预占库存 */
    @PostMapping("/api/stock/unlock")
    Result<Void> unlockStock(@RequestParam("skuId") Long skuId,
                             @RequestParam("quantity") Integer quantity,
                             @RequestParam(value = "orderId", required = false) Long orderId,
                             @RequestParam(value = "remark", required = false) String remark);

    /** 确认扣减（一般在支付成功后） */
    @PostMapping("/api/stock/confirm-deduct")
    Result<Void> confirmDeduct(@RequestParam("skuId") Long skuId,
                               @RequestParam("quantity") Integer quantity,
                               @RequestParam(value = "orderId", required = false) Long orderId,
                               @RequestParam(value = "remark", required = false) String remark);
}