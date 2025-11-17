package com.xpcjsu.sunshinemall.framework.common.feign.clients;

import com.xpcjsu.sunshinemall.framework.common.feign.dto.LogisticsCreateShipmentRequest;
import com.xpcjsu.sunshinemall.framework.common.feign.dto.LogisticsShipmentDTO;
import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import com.xpcjsu.sunshinemall.framework.common.feign.config.FeignClientsConfig;
import com.xpcjsu.sunshinemall.framework.common.feign.fallback.LogisticsClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "logistics-service",
        contextId = "logisticsClient",
        fallbackFactory = LogisticsClientFallbackFactory.class,
        configuration = FeignClientsConfig.class
)
public interface LogisticsClient {

    @PostMapping("/shipments")
    Result<LogisticsShipmentDTO> createShipment(@RequestBody LogisticsCreateShipmentRequest request);

    @GetMapping("/shipments/by-order/{orderNo}")
    Result<LogisticsShipmentDTO> getByOrderNo(@PathVariable("orderNo") String orderNo);

    @GetMapping("/shipments/{shipmentNo}")
    Result<LogisticsShipmentDTO> getByShipmentNo(@PathVariable("shipmentNo") String shipmentNo);
}