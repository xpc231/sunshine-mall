package com.xpcjsu.sunshinemall.framework.common.feign.fallback;

import com.xpcjsu.sunshinemall.framework.common.feign.clients.ProductSkuClient;
import com.xpcjsu.sunshinemall.framework.common.feign.dto.ProductSkuDTO;
import com.xpcjsu.sunshinemall.framework.convention.errorcode.BusinessErrorCode;
import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

@Slf4j
public class ProductSkuClientFallbackFactory implements FallbackFactory<ProductSkuClient> {
    @Override
    public ProductSkuClient create(Throwable cause) {
        log.error("ProductSkuClient 调用降级，原因: {}", cause == null ? "unknown" : cause.getMessage(), cause);
        return new ProductSkuClient() {
            @Override
            public Result<ProductSkuDTO> getSkuById(Long id) {
                return Result.failure(BusinessErrorCode.SYSTEM_BUSY, "商品服务繁忙，暂时无法查询SKU，请稍后重试");
            }
        };
    }
}