package com.xpcjsu.sunshinemall.product.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀商品 DTO
 *
 * @author xpcjsu
 */
@Data
public class SeckillProductDTO {

    /**
     * 秒杀商品ID
     */
    private Long id;

    /**
     * 商品ID
     */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /**
     * SKU ID
     */
    @NotNull(message = "SKU ID不能为空")
    private Long skuId;

    /**
     * 秒杀价格
     */
    @NotNull(message = "秒杀价格不能为空")
    @Positive(message = "秒杀价格必须大于0")
    private BigDecimal seckillPrice;

    /**
     * 秒杀库存数量
     */
    @NotNull(message = "秒杀库存不能为空")
    @Positive(message = "秒杀库存必须大于0")
    private Integer seckillStock;

    /**
     * 原价（用于展示）
     */
    private BigDecimal originalPrice;

    /**
     * 秒杀开始时间
     */
    @NotNull(message = "秒杀开始时间不能为空")
    private LocalDateTime startTime;

    /**
     * 秒杀结束时间
     */
    @NotNull(message = "秒杀结束时间不能为空")
    private LocalDateTime endTime;

    /**
     * 限购数量（每个用户限购数量）
     */
    @Positive(message = "限购数量必须大于0")
    private Integer limitQuantity;

    /**
     * 秒杀状态（0-未开始，1-进行中，2-已结束，3-已取消）
     */
    private Integer status;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

}

