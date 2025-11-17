package com.xpcjsu.sunshinemall.framework.common.feign.dto;

import lombok.Data;

@Data
public class LogisticsShipmentDTO {
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