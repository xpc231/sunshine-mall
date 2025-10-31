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
@Schema(name = "PayCreateResponse", description = "创建支付返回体")
public class PayCreateResponse {
    private Long payId;
    private String payNo;
    private String payUrl; // 模拟返回支付链接或二维码地址
}