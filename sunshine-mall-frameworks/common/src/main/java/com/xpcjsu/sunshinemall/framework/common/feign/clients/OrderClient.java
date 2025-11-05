package com.xpcjsu.sunshinemall.framework.common.feign.clients;

import com.xpcjsu.sunshinemall.framework.common.feign.dto.OrderPaySuccessRequest;
import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import com.xpcjsu.sunshinemall.framework.common.feign.fallback.OrderClientFallbackFactory;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 订单服务 Feign 客户端（抽取到 frameworks/common）
 */
@FeignClient(
        name = "order-service",
        contextId = "orderClient",
        fallbackFactory = OrderClientFallbackFactory.class
)
public interface OrderClient {

    /** 通知订单支付成功 */
    @PostMapping("/api/order/pay/success")
    Result<Void> notifyOrderPaySuccess(@RequestHeader("userId") Long userId,
                                       @RequestBody @Valid OrderPaySuccessRequest request);
}