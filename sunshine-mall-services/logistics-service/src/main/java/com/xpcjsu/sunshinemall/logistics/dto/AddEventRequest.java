package com.xpcjsu.sunshinemall.logistics.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddEventRequest {
    @NotBlank
    private String status;
    private String eventTime;
    private String location;
    private String message;
    private String clientEventId;
}