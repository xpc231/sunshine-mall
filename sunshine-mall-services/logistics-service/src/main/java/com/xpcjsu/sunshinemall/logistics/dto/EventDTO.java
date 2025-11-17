package com.xpcjsu.sunshinemall.logistics.dto;

import lombok.Data;

@Data
public class EventDTO {
    private String status;
    private String eventTime;
    private String location;
    private String message;
}