package com.xpcjsu.sunshinemall.logistics.controller;

import com.xpcjsu.sunshinemall.framework.base.exception.BusinessException;
import com.xpcjsu.sunshinemall.framework.convention.errorcode.BusinessErrorCode;
import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import com.xpcjsu.sunshinemall.logistics.dto.AddEventRequest;
import com.xpcjsu.sunshinemall.logistics.dto.CreateShipmentRequest;
import com.xpcjsu.sunshinemall.logistics.dto.EventDTO;
import com.xpcjsu.sunshinemall.logistics.dto.ShipmentDTO;
import com.xpcjsu.sunshinemall.logistics.dto.UpdateStatusRequest;
import com.xpcjsu.sunshinemall.logistics.service.LogisticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logistics")
@Validated
@RequiredArgsConstructor
public class LogisticsController {

    private final LogisticsService logisticsService;

    // 创建运输单
    @PostMapping("/shipments")
    public Result<ShipmentDTO> createShipment(@RequestBody @Valid CreateShipmentRequest request) {
        try {
            ShipmentDTO dto = logisticsService.createShipment(request);
            return Result.success(dto);
        } catch (BusinessException e) {
            return Result.failure(e.getErrorCode() != null ? e.getErrorCode() : BusinessErrorCode.SYSTEM_ERROR, e.getMessage());
        }
    }

    // 更新物流状态
    @PutMapping("/shipments/{shipmentNo}/status")
    public Result<ShipmentDTO> updateStatus(@PathVariable String shipmentNo,
                                            @RequestBody @Valid UpdateStatusRequest request) {
        try {
            ShipmentDTO dto = logisticsService.updateStatus(shipmentNo, request);
            return Result.success(dto);
        } catch (BusinessException e) {
            return Result.failure(e.getErrorCode() != null ? e.getErrorCode() : BusinessErrorCode.SYSTEM_ERROR, e.getMessage());
        }
    }

    // 追加轨迹（状态变化记录）
    @PostMapping("/shipments/{shipmentNo}/events")
    public Result<Boolean> addEvent(@PathVariable String shipmentNo,
                                    @RequestBody @Valid AddEventRequest request) {
        try {
            Boolean ok = logisticsService.addEvent(shipmentNo, request);
            return Result.success(ok);
        } catch (BusinessException e) {
            return Result.failure(e.getErrorCode() != null ? e.getErrorCode() : BusinessErrorCode.SYSTEM_ERROR, e.getMessage());
        }
    }

    // 查询详情
    @GetMapping("/shipments/{shipmentNo}")
    public Result<ShipmentDTO> getByShipmentNo(@PathVariable String shipmentNo) {
        ShipmentDTO dto = logisticsService.getByShipmentNo(shipmentNo);
        if (dto == null) {
            return Result.failure(BusinessErrorCode.ORDER_NOT_FOUND, "运输单不存在");
        }
        return Result.success(dto);
    }

    // 查询详情
    @GetMapping("/shipments/by-order/{orderNo}")
    public Result<ShipmentDTO> getByOrderNo(@PathVariable String orderNo) {
        ShipmentDTO dto = logisticsService.getByOrderNo(orderNo);
        if (dto == null) {
            return Result.failure(BusinessErrorCode.ORDER_NOT_FOUND, "运输单不存在");
        }
        return Result.success(dto);
    }

    // 查询轨迹
    @GetMapping("/shipments/{shipmentNo}/events")
    public Result<List<EventDTO>> listEvents(@PathVariable String shipmentNo) {
        try {
            return Result.success(logisticsService.listEvents(shipmentNo));
        } catch (BusinessException e) {
            return Result.failure(e.getErrorCode() != null ? e.getErrorCode() : BusinessErrorCode.SYSTEM_ERROR, e.getMessage());
        }
    }
}