package com.xpcjsu.sunshinemall.product.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 分类树节点 DTO
 *
 * @author xpcjsu
 */
@Data
public class CategoryTreeNodeDTO {

    /**
     * 分类ID
     */
    private Long id;

    /**
     * 父分类ID
     */
    private Long parentId;

    /**
     * 分类名称
     */
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

    /**
     * 子分类列表
     */
    private List<CategoryTreeNodeDTO> children = new ArrayList<>();

}
