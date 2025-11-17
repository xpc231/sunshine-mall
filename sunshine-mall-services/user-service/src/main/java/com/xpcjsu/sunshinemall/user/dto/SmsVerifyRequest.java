package com.xpcjsu.sunshinemall.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SmsVerifyRequest {
    @NotBlank
    private String phone;
    @NotBlank
    private String code;
}