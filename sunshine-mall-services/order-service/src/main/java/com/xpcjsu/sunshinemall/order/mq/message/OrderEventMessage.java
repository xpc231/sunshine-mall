package com.xpcjsu.sunshinemall.order.mq.message;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单事件消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "OrderEventMessage", description = "订单事件消息体")
public class OrderEventMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "事件类型，如 CREATED、CANCELLED")
    private String eventType;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "事件发生时间")
    private LocalDateTime occurTime;

    @Schema(description = "扩展信息")
    private String extra;
}