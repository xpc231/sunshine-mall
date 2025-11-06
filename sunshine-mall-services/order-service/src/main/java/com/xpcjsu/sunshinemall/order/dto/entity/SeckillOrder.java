package com.xpcjsu.sunshinemall.order.dto.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀订单记录实体
 *
 * @author xpcjsu
 */
@Data
@TableName("seckill_order")
public class SeckillOrder {

    /**
     * 秒杀订单记录ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 秒杀商品ID
     */
    private Long seckillProductId;

    /**
     * SKU ID
     */
    private Long skuId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 订单ID（关联order_info.id）
     */
    private Long orderId;

    /**
     * 订单编号（关联order_info.order_no）
     */
    private String orderNo;

    /**
     * 购买数量
     */
    private Integer quantity;

    /**
     * 秒杀价格
     */
    private BigDecimal seckillPrice;

    /**
     * 状态（0-下单中，1-已下单，2-已取消）
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}

