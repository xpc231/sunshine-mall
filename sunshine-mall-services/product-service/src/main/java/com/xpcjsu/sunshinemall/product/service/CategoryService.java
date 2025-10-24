package com.xpcjsu.sunshinemall.product.service;

import com.xpcjsu.sunshinemall.product.dto.CategoryDTO;
import com.xpcjsu.sunshinemall.product.dto.CategoryTreeNodeDTO;

import java.util.List;

/**
 * 商品分类服务接口
 *
 * @author xpcjsu
 */
public interface CategoryService {

    /**
     * 创建分类
     *
     * @param categoryDTO 分类信息
     * @return 分类ID
     */
    Long createCategory(CategoryDTO categoryDTO);

    /**
     * 更新分类
     *
     * @param categoryDTO 分类信息
     * @return 是否成功
     */
    boolean updateCategory(CategoryDTO categoryDTO);

    /**
     * 删除分类
     *
     * @param categoryId 分类ID
     * @return 是否成功
     */
    boolean deleteCategory(Long categoryId);

    /**
     * 根据ID获取分类
     *
     * @param categoryId 分类ID
     * @return 分类信息
     */
    CategoryDTO getCategoryById(Long categoryId);

    /**
     * 获取分类树（所有启用的分类）
     *
     * @return 分类树
     */
    List<CategoryTreeNodeDTO> getCategoryTree();

    /**
     * 获取分类树（包含禁用的分类，用于管理后台）
     *
     * @return 分类树
     */
    List<CategoryTreeNodeDTO> getCategoryTreeAll();

    /**
     * 根据父ID获取子分类列表
     *
     * @param parentId 父分类ID
     * @return 子分类列表
     */
    List<CategoryDTO> getCategoryListByParentId(Long parentId);

}
