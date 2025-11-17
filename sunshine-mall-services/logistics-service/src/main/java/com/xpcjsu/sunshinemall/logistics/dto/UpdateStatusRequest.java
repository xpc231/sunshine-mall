package com.xpcjsu.sunshinemall.logistics.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateStatusRequest {
    @NotBlank
    private String status;
    private String trackingCode;
    private String remark;
}