package com.xpcjsu.sunshinemall.framework.common.feign.fallback;

import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import com.xpcjsu.sunshinemall.framework.convention.errorcode.BusinessErrorCode;
import com.xpcjsu.sunshinemall.framework.common.feign.clients.StockClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * 库存客户端降级工厂：
 * - 读操作返回友好失败提示或默认值；
 * - 写操作严格失败，避免“假成功”。
 */
@Slf4j
public class StockClientFallbackFactory implements FallbackFactory<StockClient> {

    @Override
    public StockClient create(Throwable cause) {
        // 统一记录降级原因
        log.error("StockClient 调用降级，原因: {}", cause == null ? "unknown" : cause.getMessage(), cause);

        return new StockClient() {
            @Override
            public Result<Boolean> checkStock(Long skuId, Integer quantity) {
                // 读操作允许返回默认值或友好提示，这里返回失败并提示稍后重试
                return Result.failure(BusinessErrorCode.SYSTEM_BUSY, "库存服务繁忙，暂时无法校验库存，请稍后重试");
            }

            @Override
            public Result<Void> lockStock(Long skuId, Integer quantity, Long orderId, String remark) {
                // 写操作严格失败，提示稍后重试
                return Result.failure(BusinessErrorCode.SYSTEM_BUSY, "库存服务不可用，预占失败，请稍后重试");
            }

            @Override
            public Result<Void> unlockStock(Long skuId, Integer quantity, Long orderId, String remark) {
                return Result.failure(BusinessErrorCode.SYSTEM_BUSY, "库存服务不可用，释放失败，请稍后重试");
            }

            @Override
            public Result<Void> confirmDeduct(Long skuId, Integer quantity, Long orderId, String remark) {
                return Result.failure(BusinessErrorCode.SYSTEM_BUSY, "库存服务不可用，扣减确认失败，请稍后重试");
            }
        };
    }
}