package com.xpcjsu.sunshinemall.product.helper;

import com.xpcjsu.sunshinemall.framework.base.exception.ValidationException;
import com.xpcjsu.sunshinemall.product.dto.SeckillProductDTO;
import org.springframework.stereotype.Component;

/**
 * 秒杀商品验证辅助类
 * <p>
 * 封装秒杀商品相关的参数验证逻辑
 *
 * @author xpcjsu
 */
@Component
public class SeckillProductValidator {

    /**
     * 验证秒杀商品参数
     *
     * @param dto 秒杀商品DTO
     * @throws ValidationException 验证失败时抛出
     */
    public void validateSeckillProduct(SeckillProductDTO dto) {
        if (dto.getProductId() == null) {
            throw new ValidationException("PRODUCT_ID_REQUIRED", "商品ID不能为空");
        }
        if (dto.getSkuId() == null) {
            throw new ValidationException("SKU_ID_REQUIRED", "SKU ID不能为空");
        }
        if (dto.getSeckillPrice() == null || dto.getSeckillPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new ValidationException("SECKILL_PRICE_INVALID", "秒杀价格必须大于0");
        }
        if (dto.getSeckillStock() == null || dto.getSeckillStock() <= 0) {
            throw new ValidationException("SECKILL_STOCK_INVALID", "秒杀库存必须大于0");
        }
        if (dto.getStartTime() == null) {
            throw new ValidationException("START_TIME_REQUIRED", "开始时间不能为空");
        }
        if (dto.getEndTime() == null) {
            throw new ValidationException("END_TIME_REQUIRED", "结束时间不能为空");
        }
    }
}

