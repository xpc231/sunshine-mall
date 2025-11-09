package com.xpcjsu.sunshinemall.product.util;

import com.xpcjsu.sunshinemall.framework.base.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;

/**
 * 秒杀商品参数校验工具类
 * <p>
 * 功能：
 * 1. 在业务层前置拦截无效参数，防止恶意构造的参数穿透到数据库层
 * 2. 统一参数校验规则，提高代码可维护性
 * 3. 防止缓存穿透：通过参数范围校验，提前拦截无效请求
 *
 * @author xpcjsu
 */
@Slf4j
public class SeckillProductParamValidator {

    /**
     * 秒杀商品ID最大值（假设秒杀商品ID不会超过10亿）
     */
    private static final long MAX_SECKILL_PRODUCT_ID = 1_000_000_000L;

    /**
     * SKU ID最大值
     */
    private static final long MAX_SKU_ID = 10_000_000_000L;

    /**
     * 秒杀库存数量最大值
     */
    private static final int MAX_SECKILL_STOCK = 100_000;

    /**
     * 秒杀数量最大值（单次扣减数量）
     */
    private static final int MAX_QUANTITY = 100;

    /**
     * 校验秒杀商品ID
     * <p>
     * 第一道防线：参数校验，防止恶意构造的无效ID穿透到数据库
     *
     * @param id 秒杀商品ID
     * @throws ValidationException 参数无效时抛出
     */
    public static void validateSeckillProductId(Long id) {
        if (id == null) {
            throw new ValidationException("SECKILL_ID_REQUIRED", "秒杀商品ID不能为空");
        }

        if (id <= 0) {
            log.warn("拦截无效秒杀商品ID - 负数或零: {}", id);
            throw new ValidationException("SECKILL_ID_INVALID", "秒杀商品ID必须为正整数");
        }

        if (id > MAX_SECKILL_PRODUCT_ID) {
            log.warn("拦截无效秒杀商品ID - 超出范围: {}", id);
            throw new ValidationException("SECKILL_ID_OUT_OF_RANGE",
                    String.format("秒杀商品ID超出有效范围(1-%d)", MAX_SECKILL_PRODUCT_ID));
        }
    }

    /**
     * 校验SKU ID
     * <p>
     * 第一道防线：参数校验，防止恶意构造的无效SKU ID穿透到数据库
     *
     * @param skuId SKU ID
     * @throws ValidationException 参数无效时抛出
     */
    public static void validateSkuId(Long skuId) {
        if (skuId == null) {
            throw new ValidationException("SKU_ID_REQUIRED", "SKU ID不能为空");
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
     * 校验数量
     * <p>
     * 用于扣减库存、回滚库存等操作
     *
     * @param quantity 数量
     * @throws ValidationException 参数无效时抛出
     */
    public static void validateQuantity(Integer quantity) {
        if (quantity == null) {
            throw new ValidationException("QUANTITY_REQUIRED", "数量不能为空");
        }

        if (quantity <= 0) {
            log.warn("拦截无效数量 - 非正数: {}", quantity);
            throw new ValidationException("QUANTITY_INVALID", "数量必须为正整数");
        }

        if (quantity > MAX_QUANTITY) {
            log.warn("拦截超限数量 - quantity: {}, max: {}", quantity, MAX_QUANTITY);
            throw new ValidationException("QUANTITY_EXCEEDS_LIMIT",
                    String.format("数量不能超过%d", MAX_QUANTITY));
        }
    }

    /**
     * 校验秒杀库存数量
     *
     * @param stock 库存数量
     * @throws ValidationException 参数无效时抛出
     */
    public static void validateSeckillStock(Integer stock) {
        if (stock == null) {
            throw new ValidationException("SECKILL_STOCK_REQUIRED", "秒杀库存不能为空");
        }

        if (stock < 0) {
            log.warn("拦截无效秒杀库存 - 负数: {}", stock);
            throw new ValidationException("SECKILL_STOCK_INVALID", "秒杀库存不能为负数");
        }

        if (stock > MAX_SECKILL_STOCK) {
            log.warn("拦截超限秒杀库存 - stock: {}, max: {}", stock, MAX_SECKILL_STOCK);
            throw new ValidationException("SECKILL_STOCK_EXCEEDS_LIMIT",
                    String.format("秒杀库存不能超过%d", MAX_SECKILL_STOCK));
        }
    }

    /**
     * 校验分页参数
     *
     * @param pageNum  页码
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

        // 限制最大每页大小，防止恶意查询
        if (pageSize > 100) {
            log.warn("拦截过大分页大小 - pageSize: {}", pageSize);
            throw new ValidationException("PAGE_SIZE_TOO_LARGE", "每页大小不能超过100");
        }
    }
}

