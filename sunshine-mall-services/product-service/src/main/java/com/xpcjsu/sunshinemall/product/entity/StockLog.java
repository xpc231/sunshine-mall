package com.xpcjsu.sunshinemall.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存操作日志实体
 *
 * @author xpcjsu
 */
@Data
@TableName("stock_log")
public class StockLog {

    /**
     * 日志ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

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
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 操作人
     */
    private String createBy;

}
