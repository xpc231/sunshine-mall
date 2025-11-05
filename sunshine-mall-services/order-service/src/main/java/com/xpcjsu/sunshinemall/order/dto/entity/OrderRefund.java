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
 * 订单退款记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id"})
@TableName("order_refund")
@Schema(name = "OrderRefund", description = "订单退款记录")
public class OrderRefund implements Serializable {

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

    @Schema(description = "退款金额")
    @TableField("refund_amount")
    private BigDecimal refundAmount;

    @Schema(description = "退款原因")
    @TableField("refund_reason")
    private String refundReason;

    @Schema(description = "退款状态（0-待处理，1-退款成功，2-拒绝退款）")
    @TableField("refund_status")
    private Integer refundStatus;

    @Schema(description = "退款类型（例如：0-原路退回，1-线下转账）")
    @TableField("refund_type")
    private Integer refundType;

    @Schema(description = "退款交易号")
    @TableField("refund_transaction_id")
    private String refundTransactionId;

    @Schema(description = "拒绝退款原因")
    @TableField("refuse_reason")
    private String refuseReason;

    @Schema(description = "申请时间")
    @TableField("apply_time")
    private LocalDateTime applyTime;

    @Schema(description = "退款成功时间")
    @TableField("refund_time")
    private LocalDateTime refundTime;

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