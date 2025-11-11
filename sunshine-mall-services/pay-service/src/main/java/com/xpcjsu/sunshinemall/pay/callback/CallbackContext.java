package com.xpcjsu.sunshinemall.pay.callback;

import com.xpcjsu.sunshinemall.pay.entity.PayTransaction;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 支付回调上下文（最小实现）
 * 用于在责任链各处理节点之间传递状态与数据
 */
@Data
@Builder
public class CallbackContext {
    /** 渠道类型：alipay / wechat */
    private String channel;

    /** 原始表单参数（支付宝） */
    private Map<String, String> params;

    /** 原始报文（微信） */
    private String body;

    /** 业务支付单号（out_trade_no） */
    private String paySn;

    /** 渠道交易号（支付宝 trade_no / 微信 transaction_id） */
    private String channelTradeNo;

    /** 验签是否通过（占位字段，渠道节点负责设置） */
    private boolean signVerified;

    /** 加载到的支付交易记录 */
    private PayTransaction payTransaction;

    /** 处理是否成功（链执行结果） */
    private boolean success;

    /** 错误信息（用于日志记录） */
    private String errorMessage;
}