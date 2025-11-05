package com.xpcjsu.sunshinemall.framework.common.feign.fallback;

import com.xpcjsu.sunshinemall.framework.common.feign.clients.OrderClient;
import com.xpcjsu.sunshinemall.framework.common.feign.dto.OrderPaySuccessRequest;
import com.xpcjsu.sunshinemall.framework.convention.errorcode.BusinessErrorCode;
import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

@Slf4j
public class OrderClientFallbackFactory implements FallbackFactory<OrderClient> {
    @Override
    public OrderClient create(Throwable cause) {
        log.error("OrderClient 调用降级，原因: {}", cause == null ? "unknown" : cause.getMessage(), cause);
        return new OrderClient() {
            @Override
            public Result<Void> notifyOrderPaySuccess(Long userId, OrderPaySuccessRequest request) {
                return Result.failure(BusinessErrorCode.SYSTEM_BUSY, "订单服务繁忙，支付成功通知暂时不可用，请稍后重试");
            }
        };
    }
}