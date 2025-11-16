package com.xpcjsu.sunshinemall.stock.mq.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockChangeMessage {
    private Long skuId;
    private Integer operationType;
    private Integer quantity;
    private Integer beforeStock;
    private Integer afterStock;
    private Long orderId;
    private String remark;
    private Long timestamp;
}

