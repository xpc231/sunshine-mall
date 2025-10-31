package com.xpcjsu.sunshinemall.pay.client;

import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import com.xpcjsu.sunshinemall.pay.dto.OrderPaySuccessRequest;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "order-service")
public interface OrderClient {

    @PostMapping("/api/order/pay/success")
    Result<Void> notifyOrderPaySuccess(@RequestHeader("userId") Long userId,
                                       @RequestBody @Valid OrderPaySuccessRequest request);
}