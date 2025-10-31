package com.xpcjsu.sunshinemall.order.client;

import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import com.xpcjsu.sunshinemall.order.dto.external.ProductSkuDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 商品SKU Feign客户端

 */

/*
@FeignClient(name = "product-service", contextId = "productSkuClient")
public interface ProductSkuClient {

* 根据ID获取SKU

    @GetMapping("/api/sku/{id}")
    Result<ProductSkuDTO> getSkuById(@PathVariable("id") Long id);
}*/

@FeignClient("product-service")
public interface ProductSkuClient {

    @GetMapping("/api/sku/{id}")
    Result<ProductSkuDTO> getSkuById(@PathVariable("id") Long id);

}