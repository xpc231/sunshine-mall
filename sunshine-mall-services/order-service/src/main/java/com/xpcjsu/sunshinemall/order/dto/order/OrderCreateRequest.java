package com.xpcjsu.sunshinemall.order.dto.order;

import com.xpcjsu.sunshinemall.order.dto.common.SkuQuantityRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
@Schema(name = "OrderCreateRequest", description = "订单创建请求体")
public class OrderCreateRequest {

    @NotEmpty(message = "购买条目列表不能为空")
    @Schema(description = "购买条目列表")
    private List<SkuQuantityRequest> items;

    @Schema(description = "支付方式（1-支付宝，2-微信，3-银联）")
    private Integer payType = 1;

    @Schema(description = "订单来源（1-PC，2-APP，3-小程序）")
    private Integer sourceType = 1;

    @Schema(description = "订单类型（0-普通订单，1-秒杀订单）")
    private Integer orderType = 0;

    @Schema(description = "收货人姓名")
    private String receiverName;

    @Schema(description = "收货人电话")
    private String receiverPhone;

    @Schema(description = "收货人省份")
    private String receiverProvince;

    @Schema(description = "收货人城市")
    private String receiverCity;

    @Schema(description = "收货人区/县")
    private String receiverDistrict;

    @Schema(description = "收货人详细地址")
    private String receiverAddress;

    @Schema(description = "订单备注")
    private String note;

    @NotNull
    @Schema(description = "客户端幂等令牌（如前端生成的UUID）")
    private String clientToken;
}