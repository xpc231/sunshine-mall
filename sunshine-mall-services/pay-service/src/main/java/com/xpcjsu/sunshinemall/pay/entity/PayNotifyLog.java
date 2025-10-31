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
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id"})
@TableName("pay_notify_log")
@Schema(name = "PayNotifyLog", description = "回调通知日志表")
public class PayNotifyLog implements Serializable {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("ref_no")
    private String refNo;

    @TableField("notify_type")
    private Integer notifyType;

    @TableField("payload")
    private String payload;

    @TableField("sign_verified")
    private Integer signVerified;

    @TableField("handle_status")
    private Integer handleStatus;

    @TableField("handle_message")
    private String handleMessage;

    @TableField("create_time")
    private LocalDateTime createTime;
}