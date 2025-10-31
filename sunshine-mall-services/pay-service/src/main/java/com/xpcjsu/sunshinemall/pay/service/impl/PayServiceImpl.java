package com.xpcjsu.sunshinemall.pay.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xpcjsu.sunshinemall.framework.base.exception.BusinessException;
import com.xpcjsu.sunshinemall.framework.convention.errorcode.BusinessErrorCode;
import com.xpcjsu.sunshinemall.framework.distributedid.core.SnowflakeIdGenerator;
import com.xpcjsu.sunshinemall.framework.idempotent.annotation.Idempotent;
import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import com.xpcjsu.sunshinemall.pay.client.OrderClient;
import com.xpcjsu.sunshinemall.pay.dto.OrderPaySuccessRequest;
import com.xpcjsu.sunshinemall.pay.dto.pay.PayCreateRequest;
import com.xpcjsu.sunshinemall.pay.dto.pay.PayCreateResponse;
import com.xpcjsu.sunshinemall.pay.dto.pay.PayMockCallbackRequest;
import com.xpcjsu.sunshinemall.pay.dto.pay.PayQueryResponse;
import com.xpcjsu.sunshinemall.pay.dto.refund.RefundCreateRequest;
import com.xpcjsu.sunshinemall.pay.dto.refund.RefundCreateResponse;
import com.xpcjsu.sunshinemall.pay.entity.PayNotifyLog;
import com.xpcjsu.sunshinemall.pay.entity.PayRefund;
import com.xpcjsu.sunshinemall.pay.entity.PayTransaction;
import com.xpcjsu.sunshinemall.pay.enums.PayStatus;
import com.xpcjsu.sunshinemall.pay.enums.RefundStatus;
import com.xpcjsu.sunshinemall.pay.mapper.PayNotifyLogMapper;
import com.xpcjsu.sunshinemall.pay.mapper.PayRefundMapper;
import com.xpcjsu.sunshinemall.pay.mapper.PayTransactionMapper;
import com.xpcjsu.sunshinemall.pay.service.PayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class PayServiceImpl implements PayService {

    private final PayTransactionMapper payTransactionMapper;
    private final PayRefundMapper payRefundMapper;
    private final PayNotifyLogMapper payNotifyLogMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final OrderClient orderClient;

    @Override
    @Idempotent(key = "'pay:create:' + #userId + ':' + #request.clientToken", prefix = "idempotent", expireTime = 120)
    @Transactional(rollbackFor = Exception.class)
    public PayCreateResponse createPay(Long userId, PayCreateRequest request) {
        if (userId == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGIN,
                    BusinessErrorCode.getDefaultMessage(BusinessErrorCode.USER_NOT_LOGIN));
        }
        if (request == null || StringUtils.isBlank(request.getOrderNo())
                || request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(BusinessErrorCode.SYSTEM_PARAM_ERROR, "创建支付参数非法");
        }

        LocalDateTime now = LocalDateTime.now();
        String paySn = "P" + snowflakeIdGenerator.nextId();

        PayTransaction pay = PayTransaction.builder()
                .paySn(paySn)
                .orderId(null) // 与订单服务联动时可填充
                .orderNo(request.getOrderNo())
                .userId(userId)
                .amount(request.getAmount())
                .currency("CNY")
                .payType(request.getPayType())
                .status(PayStatus.INIT.getCode())
                .subject(request.getSubject())
                .body(request.getBody())
                .clientIp(null)
                .expireTime(now.plusMinutes(30))
                .createTime(now)
                .updateTime(now)
                .createBy("system")
                .updateBy("system")
                .isDeleted(0)
                .build();

        payTransactionMapper.insert(pay);

        return PayCreateResponse.builder()
                .payId(pay.getId())
                .payNo(paySn)
                .payUrl("http://mock.pay/qr/" + paySn)
                .build();
    }

    @Override
    public PayQueryResponse queryPay(String payNo) {
        if (StringUtils.isBlank(payNo)) {
            throw new BusinessException(BusinessErrorCode.SYSTEM_PARAM_ERROR, "payNo不能为空");
        }
        PayTransaction pay = payTransactionMapper.selectOne(new LambdaQueryWrapper<PayTransaction>()
                .eq(PayTransaction::getPaySn, payNo)
                .eq(PayTransaction::getIsDeleted, 0));
        if (pay == null) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_FAILED, "支付记录不存在");
        }
        return PayQueryResponse.builder()
                .payId(pay.getId())
                .payNo(pay.getPaySn())
                .status(pay.getStatus())
                .channelOrderNo(pay.getChannelTradeNo())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean mockPaySuccess(Long userId, PayMockCallbackRequest request) {
        if (userId == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGIN,
                    BusinessErrorCode.getDefaultMessage(BusinessErrorCode.USER_NOT_LOGIN));
        }
        if (request == null || StringUtils.isBlank(request.getPayNo())) {
            throw new BusinessException(BusinessErrorCode.SYSTEM_PARAM_ERROR, "回调参数非法");
        }

        PayTransaction pay = payTransactionMapper.selectOne(new LambdaQueryWrapper<PayTransaction>()
                .eq(PayTransaction::getPaySn, request.getPayNo())
                .eq(PayTransaction::getIsDeleted, 0));
        if (pay == null) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_FAILED, "支付记录不存在");
        }
        if (Objects.equals(pay.getStatus(), PayStatus.SUCCESS.getCode())) {
            log.info("支付已成功，无需重复处理 - paySn: {}", pay.getPaySn());
            return true;
        }

        // 1) 更新支付状态
        pay.setStatus(PayStatus.SUCCESS.getCode());
        pay.setChannelTradeNo(StringUtils.defaultIfBlank(request.getChannelTradeNo(), pay.getChannelTradeNo()));
        pay.setCallbackTime(LocalDateTime.now());
        pay.setUpdateTime(LocalDateTime.now());
        payTransactionMapper.updateById(pay);

        // 2) 记录通知日志（模拟）
        PayNotifyLog logEntity = PayNotifyLog.builder()
                .refNo(pay.getPaySn())
                .notifyType(1) // 1-支付成功
                .payload("{\"payNo\":\"" + pay.getPaySn() + "\"}")
                .signVerified(1)
                .handleStatus(1)
                .handleMessage("OK")
                .createTime(LocalDateTime.now())
                .build();
        payNotifyLogMapper.insert(logEntity);

        // 3) 通知订单服务（支付成功）
        try {
            OrderPaySuccessRequest orderReq = OrderPaySuccessRequest.builder()
                    .orderNo(pay.getOrderNo())
                    .paySn(pay.getPaySn())
                    .payAmount(pay.getAmount())
                    .build();
            Result<Void> res = orderClient.notifyOrderPaySuccess(userId, orderReq);
            if (res == null || res.isFailure()) {
                log.warn("订单服务支付成功通知失败 - orderNo: {}, code: {}, msg: {}",
                        pay.getOrderNo(), res == null ? null : res.getCode(), res == null ? null : res.getMessage());
            } else {
                log.info("已通知订单服务支付成功 - orderNo: {}", pay.getOrderNo());
            }
        } catch (Exception ex) {
            log.error("调用订单服务异常 - orderNo: {}", pay.getOrderNo(), ex);
        }

        return true;
    }

    @Override
    @Idempotent(key = "'refund:create:' + #userId + ':' + #request.orderNo", prefix = "idempotent", expireTime = 120)
    @Transactional(rollbackFor = Exception.class)
    public RefundCreateResponse createRefund(Long userId, RefundCreateRequest request) {
        if (userId == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGIN,
                    BusinessErrorCode.getDefaultMessage(BusinessErrorCode.USER_NOT_LOGIN));
        }
        if (request == null || StringUtils.isBlank(request.getOrderNo())
                || request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(BusinessErrorCode.SYSTEM_PARAM_ERROR, "退款参数非法");
        }

        // 查询最近的成功支付记录（简单实现，避免过度设计）
        PayTransaction pay = payTransactionMapper.selectOne(new LambdaQueryWrapper<PayTransaction>()
                .eq(PayTransaction::getOrderNo, request.getOrderNo())
                .eq(PayTransaction::getStatus, PayStatus.SUCCESS.getCode())
                .eq(PayTransaction::getIsDeleted, 0)
                .orderByDesc(PayTransaction::getCreateTime)
                .last("LIMIT 1"));
        if (pay == null) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_FAILED, "订单未支付或支付记录缺失");
        }

        LocalDateTime now = LocalDateTime.now();
        String refundSn = "R" + snowflakeIdGenerator.nextId();

        PayRefund refund = PayRefund.builder()
                .refundSn(refundSn)
                .payId(pay.getId())
                .paySn(pay.getPaySn())
                .orderId(pay.getOrderId())
                .orderNo(pay.getOrderNo())
                .userId(userId)
                .amount(request.getAmount())
                .status(RefundStatus.APPLYING.getCode())
                .reason(request.getReason())
                .channelRefundNo(null)
                .callbackTime(null)
                .createTime(now)
                .updateTime(now)
                .isDeleted(0)
                .build();

        payRefundMapper.insert(refund);

        return RefundCreateResponse.builder()
                .refundId(refund.getId())
                .refundNo(refundSn)
                .status(refund.getStatus())
                .build();
    }
}