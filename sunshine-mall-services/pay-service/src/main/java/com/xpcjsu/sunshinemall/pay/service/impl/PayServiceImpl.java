package com.xpcjsu.sunshinemall.pay.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xpcjsu.sunshinemall.framework.base.exception.BusinessException;
import com.xpcjsu.sunshinemall.framework.common.feign.clients.OrderClient;
import com.xpcjsu.sunshinemall.framework.convention.errorcode.BusinessErrorCode;
import com.xpcjsu.sunshinemall.framework.distributedid.core.SnowflakeIdGenerator;
import com.xpcjsu.sunshinemall.framework.idempotent.annotation.Idempotent;
import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import com.xpcjsu.sunshinemall.framework.common.feign.dto.OrderPaySuccessRequest;
import com.xpcjsu.sunshinemall.pay.channel.AlipayChannelService;
import com.xpcjsu.sunshinemall.pay.channel.WechatChannelService;

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
import com.xpcjsu.sunshinemall.pay.mq.message.PaymentEventMessage;
import com.xpcjsu.sunshinemall.framework.common.mq.MqConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
// RocketMQ已禁用，改用OpenFeign远程调用
// import org.apache.rocketmq.spring.core.RocketMQTemplate;
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

    private final AlipayChannelService alipayChannelService;
    private final WechatChannelService wechatChannelService;
    // RocketMQ已禁用，改用OpenFeign远程调用
    // private final RocketMQTemplate rocketMQTemplate;

    /**
     * 创建支付订单，并根据支付类型调用对应渠道的预下单接口生成支付链接。
     * 该方法具有幂等性，通过 userId 和 request.clientToken 组合生成幂等键。
     * 支持支付宝、微信等支付方式，若不支持则返回 mock 支付链接。
     *
     * @param userId  用户ID，不能为空，用于标识当前操作用户
     * @param request 支付创建请求参数对象，包含订单号、金额、支付类型等信息
     * @return PayCreateResponse 返回支付创建结果，包括支付ID、支付编号和支付链接
     * @throws BusinessException 当用户未登录或请求参数非法时抛出业务异常
     */
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
        //生成支付流水号
        String paySn = "P" + snowflakeIdGenerator.nextId();

        // 构建支付交易记录实体
        PayTransaction pay = PayTransaction.builder()
                .paySn(paySn)
                .orderId(request.getOrderId())
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

        // 根据支付类型调用不同渠道预下单接口获取支付链接
        String payUrl;
        Integer payType = request.getPayType();

        if (Objects.equals(payType, 1)) { // 1-支付宝
            payUrl = alipayChannelService.preCreate(pay);
        } else if (Objects.equals(payType, 2)) { // 2-微信
            payUrl = wechatChannelService.preCreate(pay);
        } else {
            payUrl = "http://mock.pay/qr/" + paySn;
        }

        return PayCreateResponse.builder()
                .payId(pay.getId())
                .payNo(paySn)
                .payUrl(payUrl)
                .build();
    }


    /**
     * 查询支付信息
     *
     * @param payNo 支付流水号，不能为空
     * @return PayQueryResponse 支付查询响应对象，包含支付ID、支付流水号、支付状态和渠道订单号
     * @throws BusinessException 当payNo为空时抛出参数错误异常，当支付记录不存在时抛出支付失败异常
     */
    @Override
    public PayQueryResponse queryPay(String payNo) {
        // 参数校验
        if (StringUtils.isBlank(payNo)) {
            throw new BusinessException(BusinessErrorCode.SYSTEM_PARAM_ERROR, "payNo不能为空");
        }

        // 根据支付流水号查询未删除的支付记录
        PayTransaction pay = payTransactionMapper.selectOne(new LambdaQueryWrapper<PayTransaction>()
                .eq(PayTransaction::getPaySn, payNo)
                .eq(PayTransaction::getIsDeleted, 0));

        // 支付记录不存在时抛出异常
        if (pay == null) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_FAILED, "支付记录不存在");
        }

        // 构建并返回支付查询响应对象
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

        // 3) 通知订单服务（支付成功）- 通过Feign同步调用
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

        // 4) 发送支付成功事件到MQ - RocketMQ已禁用，改用OpenFeign远程调用
        // 支付成功通知已通过Feign同步通知订单服务，如需通知其他服务请使用Feign客户端
        // sendPaymentSuccessEvent(pay);

        return true;
    }

    /**
     * 创建退款申请。
     * <p>
     * 该方法用于根据用户ID和退款请求参数创建一个新的退款记录。在创建前会校验用户登录状态、参数合法性，
     * 并查询最近一次成功的支付记录以确保可以发起退款操作。
     * </p>
     *
     * @param userId  用户ID，不能为空，代表当前发起退款的用户
     * @param request 退款请求参数对象，包含订单号、退款金额等信息，不能为null且关键字段需合法
     * @return 返回封装了退款ID、退款编号及初始状态的响应对象 {@link RefundCreateResponse}
     * @throws BusinessException 当用户未登录、参数非法或找不到有效支付记录时抛出业务异常
     */
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

        // 查询最近的成功支付记录
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

        // 调用渠道退款
        String channelRefundNo = null;

        try {
            if (Objects.equals(pay.getPayType(), 1)) { // 支付宝
                channelRefundNo = alipayChannelService.refund(pay, refund);

            } else if (Objects.equals(pay.getPayType(), 2)) { // 微信
                channelRefundNo = wechatChannelService.refund(pay, refund);

            } else {
                channelRefundNo = "MOCK-REFUND-" + refundSn;
            }

        } catch (Exception ex) {
            log.error("调用渠道退款异常 - refundSn: {}", refundSn, ex);
        }

        // 根据渠道结果更新退款记录状态
        if (StringUtils.isNotBlank(channelRefundNo)) {
            refund.setStatus(RefundStatus.SUCCESS.getCode());
            refund.setChannelRefundNo(channelRefundNo);
            refund.setCallbackTime(LocalDateTime.now());
            refund.setUpdateTime(LocalDateTime.now());
            payRefundMapper.updateById(refund);

        } else {
            refund.setStatus(RefundStatus.FAIL.getCode());
            refund.setUpdateTime(LocalDateTime.now());
            payRefundMapper.updateById(refund);
        }

        return RefundCreateResponse.builder()
                .refundId(refund.getId())
                .refundNo(refundSn)
                .status(refund.getStatus())
                .build();
    }

    // RocketMQ已禁用，改用OpenFeign远程调用
    // 支付成功通知已通过Feign同步通知订单服务，如需通知其他服务请使用Feign客户端
    /*
    *//**
     * 发送支付成功事件到MQ
     *
     * @param pay 支付交易记录
     *//*
    private void sendPaymentSuccessEvent(PayTransaction pay) {
        try {
            String destination = MqConstant.Order.TOPIC_EVENT + ":" + MqConstant.Order.Tag.PAID;
            PaymentEventMessage msg = PaymentEventMessage.builder()
                    .eventType(MqConstant.Order.Tag.PAID)
                    .paySn(pay.getPaySn())
                    .orderId(pay.getOrderId())
                    .orderNo(pay.getOrderNo())
                    .userId(pay.getUserId())
                    .amount(pay.getAmount())
                    .payType(pay.getPayType())
                    .channelTradeNo(pay.getChannelTradeNo())
                    .occurTime(LocalDateTime.now())
                    .build();
            rocketMQTemplate.syncSend(destination, msg);
            log.info("支付成功事件已发送到MQ - destination={}, orderNo={}, paySn={}", destination, pay.getOrderNo(), pay.getPaySn());
        } catch (Exception e) {
            log.warn("支付成功事件发送到MQ失败 - orderNo={}, paySn={}", pay.getOrderNo(), pay.getPaySn(), e);
        }
    }
    */
}