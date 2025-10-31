package com.xpcjsu.sunshinemall.order.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用SKU数量请求DTO
 * 用于购物车操作和订单创建等场景
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "SkuQuantityRequest", description = "SKU数量请求")
public class SkuQuantityRequest {

    @NotNull(message = "SKU ID不能为空")
    @Schema(description = "SKU ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long skuId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量最小为1")
    @Schema(description = "购买数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;
}