package com.xpcjsu.sunshinemall.framework.common.feign.fallback;

import com.xpcjsu.sunshinemall.framework.common.feign.clients.CartClient;
import com.xpcjsu.sunshinemall.framework.convention.errorcode.BusinessErrorCode;
import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

/**
 * 购物车客户端降级工厂：写操作严格失败，避免产生“假删除”。
 * Bean 在 FeignClientsConfig 中以 @Bean 方式注册。
 */
@Slf4j
public class CartClientFallbackFactory implements FallbackFactory<CartClient> {
    @Override
    public CartClient create(Throwable cause) {
        log.error("CartClient 调用降级，原因: {}", cause == null ? "unknown" : cause.getMessage(), cause);
        return new CartClient() {
            @Override
            public Result<Void> removeItem(Long userId, Long skuId) {
                return Result.failure(BusinessErrorCode.SYSTEM_BUSY, "购物车服务不可用，删除失败，请稍后重试");
            }
        };
    }
}