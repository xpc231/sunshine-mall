package com.xpcjsu.sunshinemall.product.dto;

import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 库存DTO
 *
 * @author xpcjsu
 */
@Data
public class StockDTO {

    /**
     * 库存ID
     */
    private Long id;

    /**
     * SKU ID
     */
    private Long skuId;

    /**
     * 总库存
     */
    private Integer totalStock;

    /**
     * 可用库存
     */
    private Integer availableStock;

    /**
     * 锁定库存
     */
    private Integer lockedStock;

}
