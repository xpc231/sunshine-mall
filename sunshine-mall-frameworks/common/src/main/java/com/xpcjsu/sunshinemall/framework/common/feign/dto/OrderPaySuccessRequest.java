package com.xpcjsu.sunshinemall.framework.common.feign.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 订单支付成功通知请求（抽取为通用 DTO）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class OrderPaySuccessRequest {


    @NotBlank(message = "订单编号不能为空")
    private String orderNo;

    private String paySn;

    private BigDecimal payAmount;
}