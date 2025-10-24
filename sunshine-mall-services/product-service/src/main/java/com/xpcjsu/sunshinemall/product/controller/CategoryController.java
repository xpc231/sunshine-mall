package com.xpcjsu.sunshinemall.product.controller;

import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import com.xpcjsu.sunshinemall.product.dto.CategoryDTO;
import com.xpcjsu.sunshinemall.product.dto.CategoryTreeNodeDTO;
import com.xpcjsu.sunshinemall.product.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 商品分类控制器
 *
 * @author xpcjsu
 */
@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
@Validated
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 创建分类
     */
    @PostMapping
    public Result<Long> createCategory(@Valid @RequestBody CategoryDTO categoryDTO) {
        Long categoryId = categoryService.createCategory(categoryDTO);
        return Result.success(categoryId, "创建成功");
    }

    /**
     * 更新分类
     */
    @PutMapping("/{id}")
    public Result<Void> updateCategory(@PathVariable Long id, 
                                       @Valid @RequestBody CategoryDTO categoryDTO) {
        categoryDTO.setId(id);
        boolean success = categoryService.updateCategory(categoryDTO);
        return success ? Result.success(null, "更新成功") 
                       : Result.failure("UPDATE_FAILED", "更新失败");
    }

    /**
     * 删除分类
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        boolean success = categoryService.deleteCategory(id);
        return success ? Result.success(null, "删除成功") 
                       : Result.failure("DELETE_FAILED", "删除失败");
    }

    /**
     * 根据ID获取分类
     */
    @GetMapping("/{id}")
    public Result<CategoryDTO> getCategoryById(@PathVariable Long id) {
        CategoryDTO category = categoryService.getCategoryById(id);
        return Result.success(category);
    }

    /**
     * 获取分类树（只返回启用的分类）
     */
    @GetMapping("/tree")
    public Result<List<CategoryTreeNodeDTO>> getCategoryTree() {
        List<CategoryTreeNodeDTO> tree = categoryService.getCategoryTree();
        return Result.success(tree);
    }

    /**
     * 获取分类树（包含禁用的分类，用于管理后台）
     */
    @GetMapping("/tree/all")
    public Result<List<CategoryTreeNodeDTO>> getCategoryTreeAll() {
        List<CategoryTreeNodeDTO> tree = categoryService.getCategoryTreeAll();
        return Result.success(tree);
    }

    /**
     * 根据父ID获取子分类列表
     */
    @GetMapping("/list/{parentId}")
    public Result<List<CategoryDTO>> getCategoryListByParentId(@PathVariable Long parentId) {
        List<CategoryDTO> categoryList = categoryService.getCategoryListByParentId(parentId);
        return Result.success(categoryList);
    }

}
