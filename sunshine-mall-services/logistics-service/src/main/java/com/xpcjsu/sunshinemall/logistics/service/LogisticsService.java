package com.xpcjsu.sunshinemall.logistics.service;

import com.xpcjsu.sunshinemall.logistics.dto.AddEventRequest;
import com.xpcjsu.sunshinemall.logistics.dto.ShipmentDTO;
import com.xpcjsu.sunshinemall.logistics.dto.UpdateStatusRequest;
import com.xpcjsu.sunshinemall.logistics.dto.CreateShipmentRequest;
import com.xpcjsu.sunshinemall.logistics.dto.EventDTO;

import java.util.List;

public interface LogisticsService {

    ShipmentDTO createShipment(CreateShipmentRequest request);

    ShipmentDTO updateStatus(String shipmentNo, UpdateStatusRequest request);

    Boolean addEvent(String shipmentNo, AddEventRequest request);

    ShipmentDTO getByShipmentNo(String shipmentNo);

    ShipmentDTO getByOrderNo(String orderNo);

    List<EventDTO> listEvents(String shipmentNo);

}