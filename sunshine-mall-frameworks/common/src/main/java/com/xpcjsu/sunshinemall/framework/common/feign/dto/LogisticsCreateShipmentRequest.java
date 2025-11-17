package com.xpcjsu.sunshinemall.framework.common.feign.dto;

import lombok.Data;

@Data
public class LogisticsCreateShipmentRequest {
    private String orderNo;
    private String carrierCode;
    private String carrierName;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String senderAddress;
    private String trackingCode;
}