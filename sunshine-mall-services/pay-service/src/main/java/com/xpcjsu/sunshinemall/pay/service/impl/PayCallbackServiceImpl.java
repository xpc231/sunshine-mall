package com.xpcjsu.sunshinemall.pay.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpcjsu.sunshinemall.framework.common.feign.clients.OrderClient;
import com.xpcjsu.sunshinemall.framework.idempotent.annotation.Idempotent;
import com.xpcjsu.sunshinemall.pay.channel.AlipayChannelService;
import com.xpcjsu.sunshinemall.pay.channel.WechatChannelService;
import com.xpcjsu.sunshinemall.framework.common.feign.dto.OrderPaySuccessRequest;
import com.xpcjsu.sunshinemall.pay.entity.PayNotifyLog;
import com.xpcjsu.sunshinemall.pay.entity.PayTransaction;
import com.xpcjsu.sunshinemall.pay.enums.PayStatus;
import com.xpcjsu.sunshinemall.pay.mapper.PayNotifyLogMapper;
import com.xpcjsu.sunshinemall.pay.mapper.PayTransactionMapper;
import com.xpcjsu.sunshinemall.pay.service.PayCallbackService;
import com.xpcjsu.sunshinemall.pay.callback.CallbackContext;
import com.xpcjsu.sunshinemall.pay.callback.CallbackHandlerChain;
import com.xpcjsu.sunshinemall.pay.mq.message.PaymentEventMessage;
import com.xpcjsu.sunshinemall.framework.common.mq.MqConstant;
import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
// RocketMQ已禁用，改用OpenFeign远程调用
// import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayCallbackServiceImpl implements PayCallbackService {

    private final PayTransactionMapper payTransactionMapper;
    private final PayNotifyLogMapper payNotifyLogMapper;
    private final OrderClient orderClient;
    private final AlipayChannelService alipayChannelService;
    private final WechatChannelService wechatChannelService;
    // RocketMQ已禁用，改用OpenFeign远程调用
    // private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Idempotent(key = "'pay:notify:alipay:' + #params['out_trade_no']", prefix = "idempotent", expireTime = 300)
    public boolean handleAlipayNotify(Map<String, String> params) {
        try {
            // 1. 构建上下文
            CallbackContext ctx = CallbackContext.builder()
                    .channel("alipay")
                    .params(params)
                    .paySn(params == null ? null : params.get("out_trade_no"))
                    .channelTradeNo(params == null ? null : params.getOrDefault("trade_no", ""))
                    .build();

            // 2. 构造责任链（最小节点集合）
            boolean ok = CallbackHandlerChain.execute(ctx, java.util.List.of(
                    // 参数校验与验签
                    c -> {
                        if (c.getParams() == null || StringUtils.isBlank(c.getPaySn())) {
                            log.warn("支付宝回调参数缺失: {}", c.getParams());
                            c.setErrorMessage("params missing");
                            return false;
                        }
                        boolean signOk = alipayChannelService.verifyNotify(c.getParams());
                        c.setSignVerified(signOk);
                        if (!signOk) {
                            log.warn("支付宝验签失败: {}", c.getParams());
                            c.setErrorMessage("sign verify failed");
                        }
                        return signOk;
                    },
                    // 加载支付记录
                    c -> {
                        PayTransaction pay = payTransactionMapper.selectOne(new LambdaQueryWrapper<PayTransaction>()
                                .eq(PayTransaction::getPaySn, c.getPaySn())
                                .eq(PayTransaction::getIsDeleted, 0));
                        if (pay == null) {
                            log.warn("支付记录不存在，paySn={}", c.getPaySn());
                            c.setErrorMessage("pay not found");
                            return false;
                        }
                        c.setPayTransaction(pay);
                        return true;
                    },
                    // 更新支付状态（若未成功）
                    c -> {
                        PayTransaction pay = c.getPayTransaction();
                        if (pay.getStatus() != PayStatus.SUCCESS.getCode()) {
                            pay.setStatus(PayStatus.SUCCESS.getCode());
                            pay.setChannelTradeNo(c.getChannelTradeNo());
                            pay.setCallbackTime(LocalDateTime.now());
                            pay.setUpdateTime(LocalDateTime.now());
                            payTransactionMapper.updateById(pay);
                        } else {
                            log.info("支付宝支付已成功，无需重复处理 - paySn: {}", c.getPaySn());
                        }
                        return true;
                    },
                    // 记录通知日志（不影响主流程）
                    c -> {
                        try {
                            String payload = objectMapper.writeValueAsString(c.getParams());
                            PayNotifyLog logEntity = PayNotifyLog.builder()
                                    .refNo(c.getPaySn())
                                    .notifyType(1)
                                    .payload(payload)
                                    .signVerified(c.isSignVerified() ? 1 : 0)
                                    .handleStatus(1)
                                    .handleMessage("OK")
                                    .createTime(LocalDateTime.now())
                                    .build();
                            payNotifyLogMapper.insert(logEntity);
                        } catch (Exception e) {
                            log.error("记录支付宝通知日志失败", e);
                        }
                        return true;
                    },
                    // 通知订单服务（失败仅记录告警，不中断）
                    c -> {
                        try {
                            PayTransaction pay = c.getPayTransaction();
                            OrderPaySuccessRequest orderReq = OrderPaySuccessRequest.builder()
                                    .orderNo(pay.getOrderNo())
                                    .paySn(pay.getPaySn())
                                    .payAmount(pay.getAmount())
                                    .build();
                            Result<Void> res = orderClient.notifyOrderPaySuccess(pay.getUserId(), orderReq);
                            if (res == null || res.isFailure()) {
                                log.warn("订单服务支付成功通知失败 - orderNo: {}, code: {}, msg: {}",
                                        pay.getOrderNo(), res == null ? null : res.getCode(), res == null ? null : res.getMessage());
                            }
                        } catch (Exception ex) {
                            PayTransaction pay = c.getPayTransaction();
                            log.error("调用订单服务异常 - orderNo: {}", pay == null ? null : pay.getOrderNo(), ex);
                        }
                        return true;
                    }
            ));

            return ok;
        } catch (Exception e) {
            log.error("处理支付宝回调异常", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Idempotent(key = "'pay:notify:wechat:' + #timestamp + ':' + #nonce", prefix = "idempotent", expireTime = 300)
    public boolean handleWechatNotify(String body, String serial, String signature, String timestamp, String nonce) {
        try {
            // 1. 构建上下文
            CallbackContext ctx = CallbackContext.builder()
                    .channel("wechat")
                    .body(body)
                    .paySn(wechatChannelService.extractOutTradeNo(body))
                    .channelTradeNo(wechatChannelService.extractTransactionId(body))
                    .build();

            // 2. 构造责任链（最小节点集合）
            boolean ok = CallbackHandlerChain.execute(ctx, java.util.List.of(
                    // 参数校验（占位验签）
                    c -> {
                        if (StringUtils.isBlank(c.getPaySn())) {
                            log.warn("微信回调缺少out_trade_no，body={}", c.getBody());
                            c.setErrorMessage("paySn missing");
                            return false;
                        }
                        // 若后续需要验签，可在此节点接入渠道验签逻辑
                        c.setSignVerified(true);
                        return true;
                    },
                    // 加载支付记录
                    c -> {
                        PayTransaction pay = payTransactionMapper.selectOne(new LambdaQueryWrapper<PayTransaction>()
                                .eq(PayTransaction::getPaySn, c.getPaySn())
                                .eq(PayTransaction::getIsDeleted, 0));
                        if (pay == null) {
                            log.warn("支付记录不存在，paySn={}", c.getPaySn());
                            c.setErrorMessage("pay not found");
                            return false;
                        }
                        c.setPayTransaction(pay);
                        return true;
                    },
                    // 更新支付状态（若未成功）
                    c -> {
                        PayTransaction pay = c.getPayTransaction();
                        if (pay.getStatus() != PayStatus.SUCCESS.getCode()) {
                            pay.setStatus(PayStatus.SUCCESS.getCode());
                            pay.setChannelTradeNo(StringUtils.defaultIfBlank(c.getChannelTradeNo(), pay.getChannelTradeNo()));
                            pay.setCallbackTime(LocalDateTime.now());
                            pay.setUpdateTime(LocalDateTime.now());
                            payTransactionMapper.updateById(pay);
                        } else {
                            log.info("微信支付已成功，无需重复处理 - paySn: {}", c.getPaySn());
                        }
                        return true;
                    },
                    // 记录通知日志（不影响主流程）
                    c -> {
                        try {
                            PayNotifyLog logEntity = PayNotifyLog.builder()
                                    .refNo(c.getPaySn())
                                    .notifyType(1)
                                    .payload(c.getBody())
                                    .signVerified(c.isSignVerified() ? 1 : 0)
                                    .handleStatus(1)
                                    .handleMessage("OK")
                                    .createTime(LocalDateTime.now())
                                    .build();
                            payNotifyLogMapper.insert(logEntity);
                        } catch (Exception e) {
                            log.error("记录微信通知日志失败", e);
                        }
                        return true;
                    },
                    // 通知订单服务（失败仅记录告警，不中断）
                    c -> {
                        try {
                            PayTransaction pay = c.getPayTransaction();
                            OrderPaySuccessRequest orderReq = OrderPaySuccessRequest.builder()
                                    .orderNo(pay.getOrderNo())
                                    .paySn(pay.getPaySn())
                                    .payAmount(pay.getAmount())
                                    .build();
                            Result<Void> res = orderClient.notifyOrderPaySuccess(pay.getUserId(), orderReq);
                            if (res == null || res.isFailure()) {
                                log.warn("订单服务支付成功通知失败 - orderNo: {}, code: {}, msg: {}",
                                        pay.getOrderNo(), res == null ? null : res.getCode(), res == null ? null : res.getMessage());
                            }
                        } catch (Exception ex) {
                            PayTransaction pay = c.getPayTransaction();
                            log.error("调用订单服务异常 - orderNo: {}", pay == null ? null : pay.getOrderNo(), ex);
                        }
                        return true;
                    }
            ));

            return ok;
        } catch (Exception e) {
            log.error("处理微信回调异常", e);
            return false;
        }
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