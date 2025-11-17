package com.xpcjsu.sunshinemall.framework.common.mq.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEventMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String eventType;

    private Long orderId;

    private String orderNo;

    private Long userId;

    private LocalDateTime occurTime;

    private String extra;
}