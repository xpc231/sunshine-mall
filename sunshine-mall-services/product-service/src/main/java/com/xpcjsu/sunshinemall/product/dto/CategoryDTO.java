package com.xpcjsu.sunshinemall.product.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 商品分类 DTO
 *
 * @author xpcjsu
 */
@Data
public class CategoryDTO {

    /**
     * 分类ID
     */
    private Long id;

    /**
     * 父分类ID（0表示顶级分类）
     */
    @NotNull(message = "父分类ID不能为空")
    private Long parentId;

    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    private String name;

    /**
     * 分类层级
     */
    private Integer level;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    /**
     * 分类图标URL
     */
    private String iconUrl;

    /**
     * 状态（0-禁用，1-启用）
     */
    private Integer status;

}
