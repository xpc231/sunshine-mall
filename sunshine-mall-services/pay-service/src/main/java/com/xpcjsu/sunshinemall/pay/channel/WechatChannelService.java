package com.xpcjsu.sunshinemall.pay.channel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpcjsu.sunshinemall.pay.config.WechatProperties;
import com.xpcjsu.sunshinemall.pay.entity.PayTransaction;
import com.xpcjsu.sunshinemall.pay.entity.PayRefund;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 微信渠道最小实现（V3 Native）
 * 为避免过度设计，此处先返回模拟二维码链接；待密钥配置完成后再接入SDK调用。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WechatChannelService {

    private final WechatProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 预下单，返回二维码地址
     */
    public String preCreate(PayTransaction pay) {
        if (!Boolean.TRUE.equals(properties.getEnabled())
                || StringUtils.isAnyBlank(properties.getMchId(), properties.getAppId(), properties.getMerchantPrivateKey(), properties.getApiV3Key())) {
            log.warn("微信渠道未配置完整密钥，返回模拟二维码链接");
            return "https://mock.wechat.qr/" + pay.getPaySn();
        }
        // TODO: 使用微信支付SDK调用 /v3/pay/transactions/native，返回 code_url
        log.info("[Wechat] 预下单 - paySn: {}", pay.getPaySn());
        return "https://mock.wechat.qr/" + pay.getPaySn();
    }

    /**
     * 回调报文解析（占位实现，真实环境需验签+解密）
     * 尝试从明文 JSON 中提取 out_trade_no 与 transaction_id，用于开发联调。
     */
    public String extractOutTradeNo(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            // 尝试直接读取顶层字段
            if (root.has("out_trade_no")) {
                return root.path("out_trade_no").asText();
            }
            // 尝试从resource中读取（开发联调时可能已解密为明文）
            if (root.has("resource") && root.path("resource").has("out_trade_no")) {
                return root.path("resource").path("out_trade_no").asText();
            }
        } catch (Exception e) {
            log.error("解析微信回调报文失败", e);
        }
        return null;
    }

    public String extractTransactionId(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.has("transaction_id")) {
                return root.path("transaction_id").asText();
            }
            if (root.has("resource") && root.path("resource").has("transaction_id")) {
                return root.path("resource").path("transaction_id").asText();
            }
        } catch (Exception e) {
            log.error("解析微信回调报文失败", e);
        }
        return null;
    }

    /**
     * 退款（最小实现）：当前未接入微信支付V3退款真实接口，按配置降级返回模拟退款号。
     * @param pay 原支付记录
     * @param refund 退款记录
     * @return 渠道退款单号；配置缺失时返回模拟号；当前占位实现统一返回模拟号
     */
    public String refund(PayTransaction pay, PayRefund refund) {
        // 配置未完整时直接返回模拟退款号
        if (!Boolean.TRUE.equals(properties.getEnabled())
                || StringUtils.isAnyBlank(properties.getMchId(), properties.getAppId(), properties.getMerchantPrivateKey(), properties.getApiV3Key())) {
            String mockNo = "MOCK-WECHAT-REFUND-" + refund.getRefundSn();
            log.warn("微信退款渠道未配置完整密钥，返回模拟退款号: {}", mockNo);
            return mockNo;
        }

        // TODO: 接入微信支付V3接口 POST /v3/refund/domestic/refunds，并在成功后返回退款单号（微信返回的是退款单号与渠道单号）
        log.info("[Wechat] 退款占位实现 - refundSn: {}, orderNo: {}, amount: {}", refund.getRefundSn(), refund.getOrderNo(), refund.getAmount());
        return "MOCK-WECHAT-REFUND-" + refund.getRefundSn();
    }
}