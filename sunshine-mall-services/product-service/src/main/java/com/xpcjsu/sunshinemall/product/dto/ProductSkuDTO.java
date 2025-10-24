package com.xpcjsu.sunshinemall.product.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 商品SKU DTO
 *
 * @author xpcjsu
 */
@Data
public class ProductSkuDTO {

    /**
     * SKU ID
     */
    private Long id;

    /**
     * 商品ID
     */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /**
     * SKU编码
     */
    private String skuCode;

    /**
     * SKU名称
     */
    @NotBlank(message = "SKU名称不能为空")
    private String skuName;

    /**
     * 规格属性（如：{"颜色":"红色","尺码":"XL"}）
     */
    private Map<String, String> specMap;

    /**
     * 销售价格
     */
    @NotNull(message = "销售价格不能为空")
    @DecimalMin(value = "0.01", message = "销售价格必须大于0")
    private BigDecimal price;

    /**
     * 原价
     */
    private BigDecimal originalPrice;

    /**
     * 成本价
     */
    private BigDecimal costPrice;

    /**
     * SKU图片
     */
    private String skuImage;

    /**
     * 重量（单位：kg）
     */
    private BigDecimal weight;

    /**
     * 状态（0-禁用，1-启用）
     */
    private Integer status;

    /**
     * 库存信息（扩展字段）
     */
    private Integer availableStock;

}
