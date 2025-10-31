package com.xpcjsu.sunshinemall.pay.dto.pay;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PayQueryResponse", description = "查询支付返回体")
public class PayQueryResponse {
    private Long payId;
    private String payNo;
    private Integer status; // 对应 PayStatus 枚举值
    private String channelOrderNo; // 三方渠道订单号（模拟）
}