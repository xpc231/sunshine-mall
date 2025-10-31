package com.xpcjsu.sunshinemall.pay.dto.pay;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PayMockCallbackRequest", description = "模拟支付成功回调请求体")
public class PayMockCallbackRequest {
    @NotBlank(message = "payNo不能为空")
    private String payNo;

    /** 模拟第三方渠道交易号 */
    private String channelTradeNo;
}