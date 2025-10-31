package com.xpcjsu.sunshinemall.pay.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id"})
@TableName("pay_transaction")
@Schema(name = "PayTransaction", description = "支付主表（交易记录）")
public class PayTransaction implements Serializable {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("pay_sn")
    private String paySn;

    @TableField("order_id")
    private Long orderId;

    @TableField("order_no")
    private String orderNo;

    @TableField("user_id")
    private Long userId;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("currency")
    private String currency;

    @TableField("pay_type")
    private Integer payType;

    @TableField("status")
    private Integer status;

    @TableField("subject")
    private String subject;

    @TableField("body")
    private String body;

    @TableField("channel_trade_no")
    private String channelTradeNo;

    @TableField("client_ip")
    private String clientIp;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("callback_time")
    private LocalDateTime callbackTime;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("create_by")
    private String createBy;

    @TableField("update_by")
    private String updateBy;

    @TableField("is_deleted")
    private Integer isDeleted;
}