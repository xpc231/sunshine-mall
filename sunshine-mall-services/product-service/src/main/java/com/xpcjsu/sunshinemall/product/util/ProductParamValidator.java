package com.xpcjsu.sunshinemall.product.util;

import com.xpcjsu.sunshinemall.framework.base.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;

/**
 * 商品服务参数校验工具
 * 
 * 功能:
 * 1. 在业务层前置拦截无效参数
 * 2. 防止恶意构造的参数穿透到数据库层
 * 3. 统一参数校验规则
 *
 * @author xpcjsu
 */
@Slf4j
public class ProductParamValidator {

    /**
     * 商品ID最大值 (假设商品ID不会超过10亿)
     */
    private static final long MAX_PRODUCT_ID = 1_000_000_000L;

    /**
     * SKU ID最大值
     */
    private static final long MAX_SKU_ID = 10_000_000_000L;

    /**
     * 分类ID最大值
     */
    private static final long MAX_CATEGORY_ID = 100_000L;

    /**
     * 商品名称最大长度
     */
    private static final int MAX_PRODUCT_NAME_LENGTH = 200;

    /**
     * SKU名称最大长度
     */
    private static final int MAX_SKU_NAME_LENGTH = 200;

    /**
     * 校验商品ID
     *
     * @param productId 商品ID
     * @throws ValidationException 参数无效时抛出
     */
    public static void validateProductId(Long productId) {
        if (productId == null) {
            throw new ValidationException("PRODUCT_ID_NULL", "商品ID不能为空");
        }
        
        if (productId <= 0) {
            log.warn("拦截无效商品ID - 负数或零: {}", productId);
            throw new ValidationException("PRODUCT_ID_INVALID", "商品ID必须为正整数");
        }
        
        if (productId > MAX_PRODUCT_ID) {
            log.warn("拦截无效商品ID - 超出范围: {}", productId);
            throw new ValidationException("PRODUCT_ID_OUT_OF_RANGE", 
                String.format("商品ID超出有效范围(1-%d)", MAX_PRODUCT_ID));
        }
    }

    /**
     * 校验SKU ID
     *
     * @param skuId SKU ID
     * @throws ValidationException 参数无效时抛出
     */
    public static void validateSkuId(Long skuId) {
        if (skuId == null) {
            throw new ValidationException("SKU_ID_NULL", "SKU ID不能为空");
        }
        
        if (skuId <= 0) {
            log.warn("拦截无效SKU ID - 负数或零: {}", skuId);
            throw new ValidationException("SKU_ID_INVALID", "SKU ID必须为正整数");
        }
        
        if (skuId > MAX_SKU_ID) {
            log.warn("拦截无效SKU ID - 超出范围: {}", skuId);
            throw new ValidationException("SKU_ID_OUT_OF_RANGE", 
                String.format("SKU ID超出有效范围(1-%d)", MAX_SKU_ID));
        }
    }

    /**
     * 校验分类ID
     *
     * @param categoryId 分类ID
     * @throws ValidationException 参数无效时抛出
     */
    public static void validateCategoryId(Long categoryId) {
        if (categoryId == null) {
            throw new ValidationException("CATEGORY_ID_NULL", "分类ID不能为空");
        }
        
        // 允许父分类ID为0(表示顶级分类)
        if (categoryId < 0) {
            log.warn("拦截无效分类ID - 负数: {}", categoryId);
            throw new ValidationException("CATEGORY_ID_INVALID", "分类ID不能为负数");
        }
        
        if (categoryId > MAX_CATEGORY_ID) {
            log.warn("拦截无效分类ID - 超出范围: {}", categoryId);
            throw new ValidationException("CATEGORY_ID_OUT_OF_RANGE", 
                String.format("分类ID超出有效范围(0-%d)", MAX_CATEGORY_ID));
        }
    }

    /**
     * 校验商品名称
     *
     * @param productName 商品名称
     * @throws ValidationException 参数无效时抛出
     */
    public static void validateProductName(String productName) {
        if (productName == null || productName.trim().isEmpty()) {
            throw new ValidationException("PRODUCT_NAME_EMPTY", "商品名称不能为空");
        }
        
        if (productName.length() > MAX_PRODUCT_NAME_LENGTH) {
            log.warn("拦截过长商品名称 - 长度: {}", productName.length());
            throw new ValidationException("PRODUCT_NAME_TOO_LONG", 
                String.format("商品名称长度不能超过%d个字符", MAX_PRODUCT_NAME_LENGTH));
        }
        
        // 检查是否包含特殊字符(防止SQL注入)
        if (containsDangerousChars(productName)) {
            log.warn("拦截包含危险字符的商品名称: {}", productName);
            throw new ValidationException("PRODUCT_NAME_INVALID_CHARS", 
                "商品名称不能包含特殊字符");
        }
    }

    /**
     * 校验SKU名称
     *
     * @param skuName SKU名称
     * @throws ValidationException 参数无效时抛出
     */
    public static void validateSkuName(String skuName) {
        if (skuName == null || skuName.trim().isEmpty()) {
            throw new ValidationException("SKU_NAME_EMPTY", "SKU名称不能为空");
        }
        
        if (skuName.length() > MAX_SKU_NAME_LENGTH) {
            log.warn("拦截过长SKU名称 - 长度: {}", skuName.length());
            throw new ValidationException("SKU_NAME_TOO_LONG", 
                String.format("SKU名称长度不能超过%d个字符", MAX_SKU_NAME_LENGTH));
        }
        
        if (containsDangerousChars(skuName)) {
            log.warn("拦截包含危险字符的SKU名称: {}", skuName);
            throw new ValidationException("SKU_NAME_INVALID_CHARS", 
                "SKU名称不能包含特殊字符");
        }
    }

    /**
     * 校验数量
     *
     * @param quantity 数量
     * @param maxQuantity 最大数量
     * @throws ValidationException 参数无效时抛出
     */
    public static void validateQuantity(Integer quantity, Integer maxQuantity) {
        if (quantity == null) {
            throw new ValidationException("QUANTITY_NULL", "数量不能为空");
        }
        
        if (quantity <= 0) {
            log.warn("拦截无效数量 - 非正数: {}", quantity);
            throw new ValidationException("QUANTITY_INVALID", "数量必须为正整数");
        }
        
        if (quantity > maxQuantity) {
            log.warn("拦截超限数量 - quantity: {}, max: {}", quantity, maxQuantity);
            throw new ValidationException("QUANTITY_EXCEEDS_LIMIT", 
                String.format("数量不能超过%d", maxQuantity));
        }
    }

    /**
     * 检查字符串是否包含危险字符
     *
     * @param str 待检查字符串
     * @return true-包含危险字符, false-安全
     */
    private static boolean containsDangerousChars(String str) {
        if (str == null) {
            return false;
        }
        
        // 危险字符列表(防止SQL注入和XSS攻击)
        String[] dangerousChars = {
            "'", "\"", ";", "--", "/*", "*/", 
            "<script", "</script>", "javascript:", 
            "onload=", "onerror="
        };
        
        String lowerStr = str.toLowerCase();
        for (String dangerousChar : dangerousChars) {
            if (lowerStr.contains(dangerousChar)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 校验分页参数
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @throws ValidationException 参数无效时抛出
     */
    public static void validatePageParams(Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) {
            throw new ValidationException("PAGE_NUM_INVALID", "页码必须大于等于1");
        }
        
        if (pageSize == null || pageSize < 1) {
            throw new ValidationException("PAGE_SIZE_INVALID", "每页大小必须大于等于1");
        }
        
        // 限制最大每页大小,防止恶意查询
        if (pageSize > 100) {
            log.warn("拦截过大分页大小 - pageSize: {}", pageSize);
            throw new ValidationException("PAGE_SIZE_TOO_LARGE", "每页大小不能超过100");
        }
    }
}
