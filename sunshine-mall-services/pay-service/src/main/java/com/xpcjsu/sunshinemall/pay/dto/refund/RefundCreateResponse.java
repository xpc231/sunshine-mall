package com.xpcjsu.sunshinemall.pay.dto.refund;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "RefundCreateResponse", description = "创建退款返回体")
public class RefundCreateResponse {
    private Long refundId;
    private String refundNo;
    private Integer status; // 对应 RefundStatus 枚举值
}