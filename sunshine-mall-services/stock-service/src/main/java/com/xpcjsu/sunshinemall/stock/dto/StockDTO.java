package com.xpcjsu.sunshinemall.stock.dto;

import lombok.Data;

@Data
public class StockDTO {
    private Long id;
    private Long skuId;
    private Integer totalStock;
    private Integer availableStock;
    private Integer lockedStock;
}

