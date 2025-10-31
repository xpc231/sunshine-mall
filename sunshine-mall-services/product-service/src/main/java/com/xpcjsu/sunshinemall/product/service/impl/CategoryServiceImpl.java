package com.xpcjsu.sunshinemall.product.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xpcjsu.sunshinemall.framework.base.exception.BusinessException;
import com.xpcjsu.sunshinemall.framework.base.exception.ValidationException;
import com.xpcjsu.sunshinemall.framework.cache.core.CacheManager;
import com.xpcjsu.sunshinemall.framework.distributedid.core.SnowflakeIdGenerator;
import com.xpcjsu.sunshinemall.product.constant.ProductConstants;
import com.xpcjsu.sunshinemall.product.dto.CategoryDTO;
import com.xpcjsu.sunshinemall.product.dto.CategoryTreeNodeDTO;
import com.xpcjsu.sunshinemall.product.entity.Category;
import com.xpcjsu.sunshinemall.product.mapper.CategoryMapper;
import com.xpcjsu.sunshinemall.product.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品分类服务实现
 *
 * @author xpcjsu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final CacheManager cacheManager;
    private final SnowflakeIdGenerator idGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCategory(CategoryDTO categoryDTO) {
        // 参数验证
        validateCategory(categoryDTO);

        // 检查同级分类名称是否重复
        if (categoryMapper.exists(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, categoryDTO.getParentId())
                .eq(Category::getName, categoryDTO.getName()))) {
            throw new BusinessException("CATEGORY_NAME_EXISTS", "同级分类名称已存在");
        }

        // 生成分类ID
        Long categoryId = idGenerator.nextId();

        // 转换为实体
        Category category = BeanUtil.copyProperties(categoryDTO, Category.class);
        category.setId(categoryId);

        // 设置默认值
        if (category.getStatus() == null) {
            category.setStatus(ProductConstants.Category.STATUS_ENABLED);
        }
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }

        // 保存到数据库
        categoryMapper.insert(category);
        
        log.info("创建分类成功 - categoryId: {}, name: {}", categoryId, category.getName());
        return categoryId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateCategory(CategoryDTO categoryDTO) {
        if (categoryDTO.getId() == null) {
            throw new ValidationException("CATEGORY_ID_REQUIRED", "分类ID不能为空");
        }

        // 验证分类是否存在
        Category existingCategory = categoryMapper.selectById(categoryDTO.getId());
        if (existingCategory == null) {
            throw new BusinessException("CATEGORY_NOT_FOUND", "分类不存在");
        }

        // 检查是否修改了父ID（不允许修改父分类）
        if (categoryDTO.getParentId() != null && 
            !categoryDTO.getParentId().equals(existingCategory.getParentId())) {
            throw new BusinessException("CANNOT_CHANGE_PARENT", "不允许修改父分类");
        }

        // 检查同级分类名称是否重复
        if (StringUtils.hasText(categoryDTO.getName()) && 
            !categoryDTO.getName().equals(existingCategory.getName())) {
            if (categoryMapper.exists(new LambdaQueryWrapper<Category>()
                    .eq(Category::getParentId, existingCategory.getParentId())
                    .eq(Category::getName, categoryDTO.getName())
                    .ne(Category::getId, categoryDTO.getId()))) {
                throw new BusinessException("CATEGORY_NAME_EXISTS", "同级分类名称已存在");
            }
        }

        // 更新分类
        Category category = BeanUtil.copyProperties(categoryDTO, Category.class);
        int updated = categoryMapper.updateById(category);

        if (updated > 0) {
            log.info("更新分类成功 - categoryId: {}", categoryDTO.getId());
        }

        return updated > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCategory(Long categoryId) {
        if (categoryId == null) {
            throw new ValidationException("CATEGORY_ID_REQUIRED", "分类ID不能为空");
        }

        // 检查是否有子分类
        if (categoryMapper.exists(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, categoryId))) {
            throw new BusinessException("HAS_CHILDREN_CATEGORY", "该分类下有子分类,不能删除");
        }

        // TODO: 检查是否有关联商品（需要在商品模块完成后实现）

        // 删除分类（逻辑删除）
        int deleted = categoryMapper.deleteById(categoryId);

        if (deleted > 0) {
            log.info("删除分类成功 - categoryId: {}", categoryId);
        }

        return deleted > 0;
    }

    @Override
    public CategoryDTO getCategoryById(Long categoryId) {
        if (categoryId == null) {
            throw new ValidationException("CATEGORY_ID_REQUIRED", "分类ID不能为空");
        }

        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException("CATEGORY_NOT_FOUND", "分类不存在");
        }

        return BeanUtil.copyProperties(category, CategoryDTO.class);
    }

    @Override
    public List<CategoryTreeNodeDTO> getCategoryTree() {
        // 先从缓存获取
        String cacheValue = cacheManager.get(ProductConstants.Cache.CATEGORY_TREE_KEY, String.class);
        if (StringUtils.hasText(cacheValue)) {
            return JSONUtil.toList(cacheValue, CategoryTreeNodeDTO.class);
        }

        // 查询所有启用的分类
        List<Category> categoryList = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getStatus, ProductConstants.Category.STATUS_ENABLED)
                        .orderByAsc(Category::getSortOrder, Category::getId)
        );

        // 构建分类树
        List<CategoryTreeNodeDTO> tree = buildCategoryTree(categoryList);

        // 缓存分类树
        cacheManager.set(
                ProductConstants.Cache.CATEGORY_TREE_KEY,
                JSONUtil.toJsonStr(tree),
                ProductConstants.Cache.CATEGORY_CACHE_EXPIRE
        );

        return tree;
    }

    @Override
    public List<CategoryTreeNodeDTO> getCategoryTreeAll() {
        // 查询所有分类（包含禁用的）
        List<Category> categoryList = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .orderByAsc(Category::getSortOrder, Category::getId)
        );

        // 构建分类树
        return buildCategoryTree(categoryList);
    }

    @Override
    public List<CategoryDTO> getCategoryListByParentId(Long parentId) {
        if (parentId == null) {
            throw new ValidationException("PARENT_ID_REQUIRED", "父分类ID不能为空");
        }

        List<Category> categoryList = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getParentId, parentId)
                        .orderByAsc(Category::getSortOrder, Category::getId)
        );

        return categoryList.stream()
                .map(category -> BeanUtil.copyProperties(category, CategoryDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * 验证分类信息
     */
    private void validateCategory(CategoryDTO categoryDTO) {
        if (categoryDTO.getParentId() == null) {
            throw new ValidationException("PARENT_ID_REQUIRED", "父分类ID不能为空");
        }

        if (!StringUtils.hasText(categoryDTO.getName())) {
            throw new ValidationException("CATEGORY_NAME_REQUIRED", "分类名称不能为空");
        }

        // 如果不是顶级分类，验证父分类是否存在
        if (!ProductConstants.Category.ROOT_PARENT_ID.equals(categoryDTO.getParentId())) {
            Category parentCategory = categoryMapper.selectById(categoryDTO.getParentId());
            if (parentCategory == null) {
                throw new BusinessException("PARENT_CATEGORY_NOT_FOUND", "父分类不存在");
            }

            // 计算层级（父分类层级 + 1）
            categoryDTO.setLevel(parentCategory.getLevel() + 1);

            // 验证层级不能超过3级
            if (categoryDTO.getLevel() > ProductConstants.Category.LEVEL_THREE) {
                throw new BusinessException("MAX_LEVEL_EXCEEDED", "分类层级不能超过3级");
            }
        } else {
            // 顶级分类层级为1
            categoryDTO.setLevel(ProductConstants.Category.LEVEL_ONE);
        }
    }

    /**
     * 构建分类树
     */
    private List<CategoryTreeNodeDTO> buildCategoryTree(List<Category> categoryList) {
        if (CollUtil.isEmpty(categoryList)) {
            return new ArrayList<>();
        }

        // 转换为DTO并按父ID分组
        Map<Long, List<CategoryTreeNodeDTO>> categoryMap = categoryList.stream()
                .map(category -> BeanUtil.copyProperties(category, CategoryTreeNodeDTO.class))
                .collect(Collectors.groupingBy(CategoryTreeNodeDTO::getParentId));

        // 递归构建树形结构
        return buildTree(ProductConstants.Category.ROOT_PARENT_ID, categoryMap);
    }

    /**
     * 递归构建树
     */
    private List<CategoryTreeNodeDTO> buildTree(Long parentId, 
                                                Map<Long, List<CategoryTreeNodeDTO>> categoryMap) {
        List<CategoryTreeNodeDTO> children = categoryMap.get(parentId);
        if (CollUtil.isEmpty(children)) {
            return new ArrayList<>();
        }

        // 递归设置子节点
        for (CategoryTreeNodeDTO node : children) {
            List<CategoryTreeNodeDTO> subChildren = buildTree(node.getId(), categoryMap);
            node.setChildren(subChildren);
        }

        return children;
    }

    /**
     * 清除分类树缓存
     */
    private void clearCategoryTreeCache() {
        cacheManager.delete(ProductConstants.Cache.CATEGORY_TREE_KEY);
        log.debug("清除分类树缓存");
    }

}
