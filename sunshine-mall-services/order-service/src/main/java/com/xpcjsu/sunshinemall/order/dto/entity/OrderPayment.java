package com.xpcjsu.sunshinemall.order.dto.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单支付记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id"})
@TableName("order_payment")
@Schema(name = "OrderPayment", description = "订单支付记录")
public class OrderPayment implements Serializable {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "订单ID")
    @TableField("order_id")
    private Long orderId;

    @Schema(description = "订单编号")
    @TableField("order_no")
    private String orderNo;

    @Schema(description = "用户ID")
    @TableField("user_id")
    private Long userId;

    @Schema(description = "支付金额")
    @TableField("pay_amount")
    private BigDecimal payAmount;

    @Schema(description = "支付方式（1-支付宝，2-微信，3-银联）")
    @TableField("pay_type")
    private Integer payType;

    @Schema(description = "支付状态（0-待支付，1-支付成功，2-支付失败）")
    @TableField("pay_status")
    private Integer payStatus;

    @Schema(description = "第三方交易号")
    @TableField("transaction_id")
    private String transactionId;

    @Schema(description = "回调内容")
    @TableField("callback_content")
    private String callbackContent;

    @Schema(description = "回调时间")
    @TableField("callback_time")
    private LocalDateTime callbackTime;

    @Schema(description = "创建时间")
    @TableField("create_time")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField("update_time")
    private LocalDateTime updateTime;

    @Schema(description = "逻辑删除标识（0-未删除，1-已删除）")
    @TableField("is_deleted")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}