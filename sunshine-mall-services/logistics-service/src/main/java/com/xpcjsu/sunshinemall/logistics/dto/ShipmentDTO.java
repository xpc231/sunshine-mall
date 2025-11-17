package com.xpcjsu.sunshinemall.logistics.dto;

import lombok.Data;

@Data
public class ShipmentDTO {
    private String shipmentNo;
    private String orderNo;
    private String carrierCode;
    private String carrierName;
    private String status;
    private String trackingCode;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String senderAddress;
    private String remark;
}