package com.xpcjsu.sunshinemall.order.dto.external;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 外部商品SKU DTO（与 product-service 的 ProductSkuDTO 字段对齐）
 */
@Data
public class ProductSkuDTO {
    private Long id;
    private Long productId;
    private String skuCode;
    private String skuName;
    private Map<String, String> specMap;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private BigDecimal costPrice;
    private String skuImage;
    private BigDecimal weight;
    private Integer status; // 0-禁用，1-启用
    private Integer availableStock;
}