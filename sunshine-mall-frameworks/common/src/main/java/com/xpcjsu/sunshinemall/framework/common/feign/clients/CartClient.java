package com.xpcjsu.sunshinemall.framework.common.feign.clients;

import com.xpcjsu.sunshinemall.framework.common.feign.fallback.CartClientFallbackFactory;
import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 购物车服务 Feign 客户端（抽取到 frameworks/common）
 */
@FeignClient(
        name = "cart-service",
        contextId = "cartClient",
        fallbackFactory = CartClientFallbackFactory.class
)
public interface CartClient {

    /** 删除购物车条目（按SKU） */
    @DeleteMapping("/api/cart/items/{skuId}")
    Result<Void> removeItem(@RequestHeader("userId") Long userId,
                            @PathVariable("skuId") Long skuId);
}