package com.xpcjsu.sunshinemall.order.entity;

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
 * 订单主表实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id"})
@TableName("order_info")
@Schema(name = "OrderInfo", description = "订单主表")
public class OrderInfo implements Serializable {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "订单编号")
    @TableField("order_no")
    private String orderNo;

    @Schema(description = "用户ID")
    @TableField("user_id")
    private Long userId;

    @Schema(description = "订单总金额")
    @TableField("total_amount")
    private BigDecimal totalAmount;

    @Schema(description = "实付金额")
    @TableField("pay_amount")
    private BigDecimal payAmount;

    @Schema(description = "运费金额")
    @TableField("freight_amount")
    private BigDecimal freightAmount;

    @Schema(description = "优惠金额")
    @TableField("discount_amount")
    private BigDecimal discountAmount;

    @Schema(description = "支付方式（1-支付宝，2-微信，3-银联）")
    @TableField("pay_type")
    private Integer payType;

    @Schema(description = "订单来源（1-PC，2-APP，3-小程序）")
    @TableField("source_type")
    private Integer sourceType;

    @Schema(description = "订单状态（0-待付款，1-待发货，2-已发货，3-已完成，4-已关闭，5-已取消）")
    @TableField("status")
    private Integer status;

    @Schema(description = "订单类型（0-普通订单，1-秒杀订单）")
    @TableField("order_type")
    private Integer orderType;

    @Schema(description = "物流公司")
    @TableField("delivery_company")
    private String deliveryCompany;

    @Schema(description = "物流单号")
    @TableField("delivery_sn")
    private String deliverySn;

    @Schema(description = "自动确认收货天数")
    @TableField("auto_confirm_day")
    private Integer autoConfirmDay;

    @Schema(description = "收货人姓名")
    @TableField("receiver_name")
    private String receiverName;

    @Schema(description = "收货人电话")
    @TableField("receiver_phone")
    private String receiverPhone;

    @Schema(description = "收货人省份")
    @TableField("receiver_province")
    private String receiverProvince;

    @Schema(description = "收货人城市")
    @TableField("receiver_city")
    private String receiverCity;

    @Schema(description = "收货人区/县")
    @TableField("receiver_district")
    private String receiverDistrict;

    @Schema(description = "收货人详细地址")
    @TableField("receiver_address")
    private String receiverAddress;

    @Schema(description = "订单备注")
    @TableField("note")
    private String note;

    @Schema(description = "确认收货状态（0-未确认，1-已确认）")
    @TableField("confirm_status")
    private Integer confirmStatus;

    @Schema(description = "删除状态（0-未删除，1-已删除）")
    @TableField("delete_status")
    private Integer deleteStatus;

    @Schema(description = "支付时间")
    @TableField("payment_time")
    private LocalDateTime paymentTime;

    @Schema(description = "发货时间")
    @TableField("delivery_time")
    private LocalDateTime deliveryTime;

    @Schema(description = "确认收货时间")
    @TableField("receive_time")
    private LocalDateTime receiveTime;

    @Schema(description = "评价时间")
    @TableField("comment_time")
    private LocalDateTime commentTime;

    @Schema(description = "创建时间")
    @TableField("create_time")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField("update_time")
    private LocalDateTime updateTime;

    @Schema(description = "创建人")
    @TableField("create_by")
    private String createBy;

    @Schema(description = "更新人")
    @TableField("update_by")
    private String updateBy;

    @Schema(description = "逻辑删除标识（0-未删除，1-已删除）")
    @TableField("is_deleted")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}