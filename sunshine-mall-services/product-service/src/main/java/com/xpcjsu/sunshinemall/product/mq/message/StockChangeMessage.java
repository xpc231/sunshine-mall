package com.xpcjsu.sunshinemall.product.mq.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 库存变更消息
 *
 * @author xpcjsu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockChangeMessage {

    /**
     * SKU ID
     */
    private Long skuId;

    /**
     * 操作类型（1-入库，2-扣减，3-预占，4-释放，5-退货）
     */
    private Integer operationType;

    /**
     * 变更数量
     */
    private Integer quantity;

    /**
     * 变更前库存
     */
    private Integer beforeStock;

    /**
     * 变更后库存
     */
    private Integer afterStock;

    /**
     * 关联订单ID
     */
    private Long orderId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 时间戳
     */
    private Long timestamp;

}
