package com.xpcjsu.sunshinemall.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀商品实体
 *
 * @author xpcjsu
 */
@Data
@TableName("seckill_product")
public class SeckillProduct {

    /**
     * 秒杀商品ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * SKU ID
     */
    private Long skuId;

    /**
     * 秒杀价格
     */
    private BigDecimal seckillPrice;

    /**
     * 秒杀库存数量
     */
    private Integer seckillStock;

    /**
     * 原价（用于展示）
     */
    private BigDecimal originalPrice;

    /**
     * 秒杀开始时间
     */
    private LocalDateTime startTime;

    /**
     * 秒杀结束时间
     */
    private LocalDateTime endTime;

    /**
     * 限购数量（每个用户限购数量）
     */
    private Integer limitQuantity;

    /**
     * 秒杀状态（0-未开始，1-进行中，2-已结束，3-已取消）
     */
    private Integer status;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 版本号（乐观锁，用于防超卖）
     */
    private Integer version;

    /**
     * 删除标识（0-未删除，1-已删除）
     */
    @TableLogic
    private Integer isDeleted;

}

