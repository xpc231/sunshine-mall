package com.xpcjsu.sunshinemall.framework.common.feign.fallback;

import com.xpcjsu.sunshinemall.framework.common.feign.clients.LogisticsClient;
import com.xpcjsu.sunshinemall.framework.common.feign.dto.LogisticsCreateShipmentRequest;
import com.xpcjsu.sunshinemall.framework.common.feign.dto.LogisticsShipmentDTO;
import com.xpcjsu.sunshinemall.framework.convention.errorcode.BusinessErrorCode;
import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

@Slf4j
public class LogisticsClientFallbackFactory implements FallbackFactory<LogisticsClient> {

    @Override
    public LogisticsClient create(Throwable cause) {
        log.error("LogisticsClient 调用降级，原因: {}", cause == null ? "unknown" : cause.getMessage(), cause);
        return new LogisticsClient() {
            @Override
            public Result<LogisticsShipmentDTO> createShipment(LogisticsCreateShipmentRequest request) {
                return Result.failure(BusinessErrorCode.SYSTEM_BUSY, "物流服务不可用，创建运单失败，请稍后重试");
            }

            @Override
            public Result<LogisticsShipmentDTO> getByOrderNo(String orderNo) {
                return Result.failure(BusinessErrorCode.SYSTEM_BUSY, "物流服务繁忙，暂时无法查询运单，请稍后重试");
            }

            @Override
            public Result<LogisticsShipmentDTO> getByShipmentNo(String shipmentNo) {
                return Result.failure(BusinessErrorCode.SYSTEM_BUSY, "物流服务繁忙，暂时无法查询运单，请稍后重试");
            }
        };
    }
}