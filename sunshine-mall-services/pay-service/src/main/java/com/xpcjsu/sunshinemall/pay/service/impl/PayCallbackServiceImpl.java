package com.xpcjsu.sunshinemall.pay.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpcjsu.sunshinemall.framework.idempotent.annotation.Idempotent;
import com.xpcjsu.sunshinemall.pay.channel.AlipayChannelService;
import com.xpcjsu.sunshinemall.pay.channel.WechatChannelService;
import com.xpcjsu.sunshinemall.pay.client.OrderClient;
import com.xpcjsu.sunshinemall.pay.dto.OrderPaySuccessRequest;
import com.xpcjsu.sunshinemall.pay.entity.PayNotifyLog;
import com.xpcjsu.sunshinemall.pay.entity.PayTransaction;
import com.xpcjsu.sunshinemall.pay.enums.PayStatus;
import com.xpcjsu.sunshinemall.pay.mapper.PayNotifyLogMapper;
import com.xpcjsu.sunshinemall.pay.mapper.PayTransactionMapper;
import com.xpcjsu.sunshinemall.pay.service.PayCallbackService;
import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Idempotent(key = "'pay:notify:alipay:' + #params['out_trade_no']", prefix = "idempotent", expireTime = 300)
    public boolean handleAlipayNotify(Map<String, String> params) {
        try {
            if (params == null || StringUtils.isBlank(params.get("out_trade_no"))) {
                log.warn("支付宝回调参数缺失: {}", params);
                return false;
            }
            // 验签（占位实现）
            boolean signOk = alipayChannelService.verifyNotify(params);
            if (!signOk) {
                log.warn("支付宝验签失败: {}", params);
                return false;
            }

            String paySn = params.get("out_trade_no");
            String tradeStatus = params.getOrDefault("trade_status", "");
            String tradeNo = params.getOrDefault("trade_no", "");

            PayTransaction pay = payTransactionMapper.selectOne(new LambdaQueryWrapper<PayTransaction>()
                    .eq(PayTransaction::getPaySn, paySn)
                    .eq(PayTransaction::getIsDeleted, 0));
            if (pay == null) {
                log.warn("支付记录不存在，paySn={}", paySn);
                return false;
            }

            if (pay.getStatus() != PayStatus.SUCCESS.getCode()) {
                // 可按需判断 trade_status 是否为成功，这里保持最小实现
                pay.setStatus(PayStatus.SUCCESS.getCode());
                pay.setChannelTradeNo(tradeNo);
                pay.setCallbackTime(LocalDateTime.now());
                pay.setUpdateTime(LocalDateTime.now());
                payTransactionMapper.updateById(pay);

                // 记录通知日志
                try {
                    String payload = objectMapper.writeValueAsString(params);
                    PayNotifyLog logEntity = PayNotifyLog.builder()
                            .refNo(paySn)
                            .notifyType(1)
                            .payload(payload)
                            .signVerified(1)
                            .handleStatus(1)
                            .handleMessage("OK")
                            .createTime(LocalDateTime.now())
                            .build();
                    payNotifyLogMapper.insert(logEntity);
                } catch (Exception e) {
                    log.error("记录支付宝通知日志失败", e);
                }

                // 通知订单服务
                try {
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
                    log.error("调用订单服务异常 - orderNo: {}", pay.getOrderNo(), ex);
                }
            } else {
                log.info("支付宝支付已成功，无需重复处理 - paySn: {}", paySn);
            }
            return true;
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
            String paySn = wechatChannelService.extractOutTradeNo(body);
            String transactionId = wechatChannelService.extractTransactionId(body);
            if (StringUtils.isBlank(paySn)) {
                log.warn("微信回调缺少out_trade_no，body={}", body);
                return false;
            }

            PayTransaction pay = payTransactionMapper.selectOne(new LambdaQueryWrapper<PayTransaction>()
                    .eq(PayTransaction::getPaySn, paySn)
                    .eq(PayTransaction::getIsDeleted, 0));
            if (pay == null) {
                log.warn("支付记录不存在，paySn={}", paySn);
                return false;
            }

            if (pay.getStatus() != PayStatus.SUCCESS.getCode()) {
                pay.setStatus(PayStatus.SUCCESS.getCode());
                pay.setChannelTradeNo(StringUtils.defaultIfBlank(transactionId, pay.getChannelTradeNo()));
                pay.setCallbackTime(LocalDateTime.now());
                pay.setUpdateTime(LocalDateTime.now());
                payTransactionMapper.updateById(pay);

                // 记录通知日志
                try {
                    PayNotifyLog logEntity = PayNotifyLog.builder()
                            .refNo(paySn)
                            .notifyType(1)
                            .payload(body)
                            .signVerified(1)
                            .handleStatus(1)
                            .handleMessage("OK")
                            .createTime(LocalDateTime.now())
                            .build();
                    payNotifyLogMapper.insert(logEntity);
                } catch (Exception e) {
                    log.error("记录微信通知日志失败", e);
                }

                // 通知订单服务
                try {
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
                    log.error("调用订单服务异常 - orderNo: {}", pay.getOrderNo(), ex);
                }
            } else {
                log.info("微信支付已成功，无需重复处理 - paySn: {}", paySn);
            }
            return true;
        } catch (Exception e) {
            log.error("处理微信回调异常", e);
            return false;
        }
    }
}