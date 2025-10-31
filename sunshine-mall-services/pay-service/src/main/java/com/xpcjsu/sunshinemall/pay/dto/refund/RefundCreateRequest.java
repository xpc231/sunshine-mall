package com.xpcjsu.sunshinemall.pay.dto.refund;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "RefundCreateRequest", description = "创建退款请求体")
public class RefundCreateRequest {
    @NotBlank(message = "订单编号不能为空")
    private String orderNo;

    @NotNull(message = "退款金额不能为空")
    private BigDecimal amount;

    private String reason;
}