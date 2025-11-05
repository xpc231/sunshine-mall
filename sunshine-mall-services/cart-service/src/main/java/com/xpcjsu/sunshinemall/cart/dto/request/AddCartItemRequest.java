package com.xpcjsu.sunshinemall.cart.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "AddCartItemRequest", description = "新增购物车条目请求")
public class AddCartItemRequest {

    @NotNull
    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "商品图片")
    private String productImage;

    @NotNull
    @Schema(description = "SKU ID")
    private Long skuId;

    @Schema(description = "SKU编码")
    private String skuCode;

    @Schema(description = "SKU名称")
    private String skuName;

    @Schema(description = "SKU规格JSON")
    private String skuSpec;

    @NotNull
    @Schema(description = "单价")
    private BigDecimal price;

    @Min(1)
    @Schema(description = "数量，最小为1")
    private Integer quantity;
}