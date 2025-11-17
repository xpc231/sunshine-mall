package com.xpcjsu.sunshinemall.logistics.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateShipmentRequest {
    @NotBlank
    private String orderNo;
    @NotBlank
    private String carrierCode;
    @NotBlank
    private String carrierName;
    @NotBlank
    private String receiverName;
    @NotBlank
    private String receiverPhone;
    @NotBlank
    private String receiverAddress;
    @NotBlank
    private String senderAddress;
    private String trackingCode;
}