package com.xpcjsu.sunshinemall.order.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "OrderDeliverRequest", description = "订单发货请求")
public class OrderDeliverRequest {
    @Schema(description = "物流公司编码")
    private String carrierCode;
    @Schema(description = "物流公司名称")
    private String carrierName;
    @Schema(description = "发件地址")
    private String senderAddress;
    @Schema(description = "可选：物流追踪编号")
    private String trackingCode;
}