package com.xpcjsu.sunshinemall.pay.service;

import com.xpcjsu.sunshinemall.pay.dto.pay.PayCreateRequest;
import com.xpcjsu.sunshinemall.pay.dto.pay.PayCreateResponse;
import com.xpcjsu.sunshinemall.pay.dto.pay.PayMockCallbackRequest;
import com.xpcjsu.sunshinemall.pay.dto.pay.PayQueryResponse;
import com.xpcjsu.sunshinemall.pay.dto.refund.RefundCreateRequest;
import com.xpcjsu.sunshinemall.pay.dto.refund.RefundCreateResponse;

/**
 * 支付服务接口
 */
public interface PayService {

    /** 创建支付 */
    PayCreateResponse createPay(Long userId, PayCreateRequest request);

    /** 查询支付 */
    PayQueryResponse queryPay(String payNo);

    /** 模拟支付成功回调（更新支付状态并通知订单服务） */
    boolean mockPaySuccess(Long userId, PayMockCallbackRequest request);

    /** 创建退款 */
    RefundCreateResponse createRefund(Long userId, RefundCreateRequest request);
}