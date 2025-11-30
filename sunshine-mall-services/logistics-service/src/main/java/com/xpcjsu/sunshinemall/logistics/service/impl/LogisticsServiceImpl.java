package com.xpcjsu.sunshinemall.logistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xpcjsu.sunshinemall.framework.base.exception.BusinessException;
import com.xpcjsu.sunshinemall.framework.cache.core.CacheKeyBuilder;
import com.xpcjsu.sunshinemall.framework.cache.core.CacheManager;
import com.xpcjsu.sunshinemall.framework.convention.errorcode.BusinessErrorCode;
import com.xpcjsu.sunshinemall.framework.idempotent.annotation.Idempotent;
import com.xpcjsu.sunshinemall.logistics.dto.AddEventRequest;
import com.xpcjsu.sunshinemall.logistics.dto.EventDTO;
import com.xpcjsu.sunshinemall.logistics.dto.ShipmentDTO;
import com.xpcjsu.sunshinemall.logistics.dto.UpdateStatusRequest;
import com.xpcjsu.sunshinemall.logistics.dto.CreateShipmentRequest;
import com.xpcjsu.sunshinemall.logistics.entity.LogisticsEvent;
import com.xpcjsu.sunshinemall.logistics.entity.LogisticsShipment;
import com.xpcjsu.sunshinemall.logistics.mapper.LogisticsEventMapper;
import com.xpcjsu.sunshinemall.logistics.mapper.LogisticsShipmentMapper;
import com.xpcjsu.sunshinemall.logistics.util.LogisticsIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LogisticsServiceImpl implements com.xpcjsu.sunshinemall.logistics.service.LogisticsService {

    private final LogisticsShipmentMapper shipmentMapper;
    private final LogisticsEventMapper eventMapper;
    private final LogisticsIdGenerator idGenerator;
    private final CacheManager cacheManager;

    // 订单状态的常量集合
    private static final Set<String> STATUSES = Set.of(
            "CREATED", "ACCEPTED", "IN_TRANSIT", "DELIVERED", "CANCELLED", "FAILED"
    );

    @Override
    @Transactional
    @Idempotent(key = "#request.orderNo", prefix = "logistics:ship", expireTime = 120)
    public ShipmentDTO createShipment(CreateShipmentRequest request) {
        // 查询是否已存在物流发货记录
        LogisticsShipment exists = shipmentMapper.selectOne(new LambdaQueryWrapper<LogisticsShipment>()
                .eq(LogisticsShipment::getOrderNo, request.getOrderNo()));

        if (exists != null) {
            return toShipmentDTO(exists);
        }

        if (!STATUSES.contains("CREATED")) {
            throw new BusinessException(BusinessErrorCode.SYSTEM_ERROR, "状态错误");
        }

        LogisticsShipment shipment = new LogisticsShipment();
        shipment.setShipmentNo(String.valueOf(idGenerator.nextId()));
        shipment.setOrderNo(request.getOrderNo());
        shipment.setCarrierCode(request.getCarrierCode());
        shipment.setCarrierName(request.getCarrierName());
        shipment.setStatus("CREATED");
        shipment.setTrackingCode(request.getTrackingCode());
        shipment.setReceiverName(request.getReceiverName());
        shipment.setReceiverPhone(request.getReceiverPhone());
        shipment.setReceiverAddress(request.getReceiverAddress());
        shipment.setSenderAddress(request.getSenderAddress());
        shipmentMapper.insert(shipment);

        String cacheKey = CacheKeyBuilder.build(
                "logistics", "shipment:detail", shipment.getShipmentNo());
        cacheManager.set(cacheKey, toShipmentDTO(shipment), 300);
        return toShipmentDTO(shipment);
    }

    @Override
    @Transactional
    public ShipmentDTO updateStatus(String shipmentNo, UpdateStatusRequest request) {
        if (!STATUSES.contains(request.getStatus())) {
            throw new BusinessException(BusinessErrorCode.SYSTEM_PARAM_ERROR, "非法状态");
        }
        LogisticsShipment shipment = shipmentMapper.selectOne(new LambdaQueryWrapper<LogisticsShipment>()
                .eq(LogisticsShipment::getShipmentNo, shipmentNo));
        if (shipment == null) {
            throw new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND, "运输单不存在");
        }
        if ("DELIVERED".equals(shipment.getStatus())) {
            throw new BusinessException(BusinessErrorCode.ORDER_STATUS_ERROR, "已签收不可变更");
        }
        shipment.setStatus(request.getStatus());
        shipment.setTrackingCode(request.getTrackingCode());
        shipment.setRemark(request.getRemark());
        shipmentMapper.updateById(shipment);
        String cacheKey = CacheKeyBuilder.build("logistics", "shipment:detail", shipmentNo);
        cacheManager.delete(cacheKey);
        return toShipmentDTO(shipment);
    }

    @Override
    @Transactional
    @Idempotent(key = "#request.clientEventId", prefix = "logistics:event", expireTime = 300)
    public Boolean addEvent(String shipmentNo, AddEventRequest request) {
        LogisticsShipment shipment = shipmentMapper.selectOne(new LambdaQueryWrapper<LogisticsShipment>()
                .eq(LogisticsShipment::getShipmentNo, shipmentNo));
        if (shipment == null) {
            throw new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND, "运输单不存在");
        }
        if (request.getStatus() != null && STATUSES.contains(request.getStatus())) {
            shipment.setStatus(request.getStatus());
            shipmentMapper.updateById(shipment);
            cacheManager.delete(CacheKeyBuilder.build("logistics", "shipment:detail", shipmentNo));
        }
        LogisticsEvent event = new LogisticsEvent();
        event.setShipmentNo(shipmentNo);
        event.setStatus(request.getStatus());
        event.setEventTime(parseEventTime(request.getEventTime()));
        event.setLocation(request.getLocation());
        event.setMessage(request.getMessage());
        eventMapper.insert(event);
        return true;
    }

    @Override
    public ShipmentDTO getByShipmentNo(String shipmentNo) {
        String cacheKey = CacheKeyBuilder.build("logistics", "shipment:detail", shipmentNo);
        Object cachedObj = cacheManager.get(cacheKey);
        if (cachedObj != null) {
            if (cachedObj instanceof ShipmentDTO) {
                return (ShipmentDTO) cachedObj;
            }
            if (cachedObj instanceof java.util.Map) {
                java.util.Map<?, ?> m = (java.util.Map<?, ?>) cachedObj;
                ShipmentDTO dto = new ShipmentDTO();
                dto.setShipmentNo((String) m.get("shipmentNo"));
                dto.setOrderNo((String) m.get("orderNo"));
                dto.setCarrierCode((String) m.get("carrierCode"));
                dto.setCarrierName((String) m.get("carrierName"));
                dto.setStatus((String) m.get("status"));
                dto.setTrackingCode((String) m.get("trackingCode"));
                dto.setReceiverName((String) m.get("receiverName"));
                dto.setReceiverPhone((String) m.get("receiverPhone"));
                dto.setReceiverAddress((String) m.get("receiverAddress"));
                dto.setSenderAddress((String) m.get("senderAddress"));
                dto.setRemark((String) m.get("remark"));
                return dto;
            }
            if (cachedObj instanceof String && "NULL".equals(cachedObj)) {
                return null;
            }
        }
        LogisticsShipment shipment = shipmentMapper.selectOne(new LambdaQueryWrapper<LogisticsShipment>()
                .eq(LogisticsShipment::getShipmentNo, shipmentNo));
        if (shipment == null) {
            cacheManager.set(cacheKey, "NULL", 30);
            return null;
        }
        ShipmentDTO dto = toShipmentDTO(shipment);
        cacheManager.set(cacheKey, dto, 300);
        return dto;
    }

    @Override
    public ShipmentDTO getByOrderNo(String orderNo) {
        LogisticsShipment shipment = shipmentMapper.selectOne(new LambdaQueryWrapper<LogisticsShipment>()
                .eq(LogisticsShipment::getOrderNo, orderNo));
        if (shipment == null) {
            return null;
        }
        return getByShipmentNo(shipment.getShipmentNo());
    }

    @Override
    public List<EventDTO> listEvents(String shipmentNo) {
        List<LogisticsEvent> events = eventMapper.selectList(new LambdaQueryWrapper<LogisticsEvent>()
                .eq(LogisticsEvent::getShipmentNo, shipmentNo)
                .orderByDesc(LogisticsEvent::getEventTime));
        if (events == null || events.isEmpty()) {
            return new ArrayList<>();
        }
        return events.stream().map(this::toEventDTO).collect(Collectors.toList());
    }

    private ShipmentDTO toShipmentDTO(LogisticsShipment shipment) {
        ShipmentDTO dto = new ShipmentDTO();
        dto.setShipmentNo(shipment.getShipmentNo());
        dto.setOrderNo(shipment.getOrderNo());
        dto.setCarrierCode(shipment.getCarrierCode());
        dto.setCarrierName(shipment.getCarrierName());
        dto.setStatus(shipment.getStatus());
        dto.setTrackingCode(shipment.getTrackingCode());
        dto.setReceiverName(shipment.getReceiverName());
        dto.setReceiverPhone(shipment.getReceiverPhone());
        dto.setReceiverAddress(shipment.getReceiverAddress());
        dto.setSenderAddress(shipment.getSenderAddress());
        dto.setRemark(shipment.getRemark());
        return dto;
    }

    private EventDTO toEventDTO(LogisticsEvent event) {
        EventDTO dto = new EventDTO();
        dto.setStatus(event.getStatus());
        dto.setEventTime(event.getEventTime() != null ? event.getEventTime().toString() : null);
        dto.setLocation(event.getLocation());
        dto.setMessage(event.getMessage());
        return dto;
    }

    private LocalDateTime parseEventTime(String eventTime) {
        if (eventTime == null || eventTime.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(eventTime);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}