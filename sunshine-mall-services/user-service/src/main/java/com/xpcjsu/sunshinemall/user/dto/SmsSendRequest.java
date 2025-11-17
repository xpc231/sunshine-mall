package com.xpcjsu.sunshinemall.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SmsSendRequest {
    @NotBlank
    private String phone;
}