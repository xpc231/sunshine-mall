package com.xpcjsu.sunshinemall.product.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 商品 DTO
 *
 * @author xpcjsu
 */
@Data
public class ProductDTO {

    /**
     * 商品ID
     */
    private Long id;

    /**
     * 分类ID
     */
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    /**
     * 商品名称
     */
    @NotBlank(message = "商品名称不能为空")
    private String productName;

    /**
     * 商品编码
     */
    private String productCode;

    /**
     * 品牌ID
     */
    private Long brandId;

    /**
     * 主图URL
     */
    private String mainImage;

    /**
     * 副图URL列表
     */
    private List<String> subImages;

    /**
     * 商品详情描述
     */
    private String detail;

    /**
     * 商品状态（0-下架，1-上架，2-预售）
     */
    private Integer status;

    /**
     * 销售数量
     */
    private Integer saleCount;

    /**
     * 浏览次数
     */
    private Integer viewCount;

}
