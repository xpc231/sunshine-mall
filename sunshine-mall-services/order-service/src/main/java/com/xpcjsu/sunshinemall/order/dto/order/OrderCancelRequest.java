package com.xpcjsu.sunshinemall.order.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
@Schema(name = "OrderCancelRequest", description = "订单取消请求体")
public class OrderCancelRequest {
    @NotBlank
    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "取消原因")
    private String reason;
}