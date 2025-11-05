package com.xpcjsu.sunshinemall.framework.common.feign.clients;

import com.xpcjsu.sunshinemall.framework.common.feign.dto.ProductSkuDTO;
import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import com.xpcjsu.sunshinemall.framework.common.feign.fallback.ProductSkuClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 商品SKU Feign客户端（抽取到 frameworks/common）
 */
@FeignClient(
        name = "product-service",
        contextId = "productSkuClient",
        fallbackFactory = ProductSkuClientFallbackFactory.class
)
public interface ProductSkuClient {

    /** 根据ID获取SKU */
    @GetMapping("/api/sku/{id}")
    Result<ProductSkuDTO> getSkuById(@PathVariable("id") Long id);
}