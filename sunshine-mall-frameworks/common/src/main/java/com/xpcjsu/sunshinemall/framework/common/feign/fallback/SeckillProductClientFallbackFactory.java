package com.xpcjsu.sunshinemall.framework.common.feign.fallback;

import com.xpcjsu.sunshinemall.framework.common.feign.clients.SeckillProductClient;
import com.xpcjsu.sunshinemall.framework.convention.errorcode.BusinessErrorCode;
import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Map;

/**
 * 秒杀商品客户端降级工厂
 *
 * @author xpcjsu
 */
@Slf4j
public class SeckillProductClientFallbackFactory implements FallbackFactory<SeckillProductClient> {

    @Override
    public SeckillProductClient create(Throwable cause) {
        log.error("SeckillProductClient 调用降级，原因: {}", cause == null ? "unknown" : cause.getMessage(), cause);
        
        return new SeckillProductClient() {
            @Override
            public Result<Map<String, Object>> getSeckillProductBySkuId(Long skuId) {
                return Result.failure(BusinessErrorCode.SYSTEM_BUSY, "秒杀商品服务繁忙，暂时无法查询秒杀商品，请稍后重试");
            }

            @Override
            public Result<Void> deductSeckillStock(Long seckillProductId, Integer quantity) {
                return Result.failure(BusinessErrorCode.SYSTEM_BUSY, "秒杀商品服务不可用，库存扣减失败，请稍后重试");
            }

            @Override
            public Result<Void> rollbackSeckillStock(Long seckillProductId, Integer quantity) {
                return Result.failure(BusinessErrorCode.SYSTEM_BUSY, "秒杀商品服务不可用，回滚库存失败，请稍后重试");
            }
        };
    }
}

