package com.xpcjsu.sunshinemall.order.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 订单支付成功通知请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "OrderPaySuccessRequest", description = "订单支付成功通知请求体")
public class OrderPaySuccessRequest {

    @Schema(description = "订单编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "订单编号不能为空")
    private String orderNo;

    @Schema(description = "支付流水号/交易号，可选")
    private String paySn;

    @Schema(description = "支付金额，可选")
    private BigDecimal payAmount;
}