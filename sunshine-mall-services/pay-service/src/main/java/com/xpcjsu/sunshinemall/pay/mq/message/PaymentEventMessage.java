package com.xpcjsu.sunshinemall.pay.mq.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付事件消息
 *
 * @author xpcjsu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEventMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 事件类型（paid-支付成功, refund-退款成功）
     */
    private String eventType;

    /**
     * 支付流水号
     */
    private String paySn;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 支付方式（1-支付宝，2-微信）
     */
    private Integer payType;

    /**
     * 渠道订单号
     */
    private String channelTradeNo;

    /**
     * 事件发生时间
     */
    private LocalDateTime occurTime;
}

