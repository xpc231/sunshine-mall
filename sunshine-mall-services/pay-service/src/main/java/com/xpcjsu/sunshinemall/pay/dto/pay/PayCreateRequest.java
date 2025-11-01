package com.xpcjsu.sunshinemall.pay.dto.pay;

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
@Schema(name = "PayCreateRequest", description = "创建支付请求体")
public class PayCreateRequest {

    @NotBlank(message = "订单编号不能为空")
    private String orderNo;

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotNull(message = "支付金额不能为空")
    private BigDecimal amount;

    @NotNull(message = "支付方式不能为空")
    private Integer payType; // 1-支付宝，2-微信，3-银联

    private String subject;
    private String body;

    @NotBlank(message = "clientToken不能为空")
    private String clientToken;
}