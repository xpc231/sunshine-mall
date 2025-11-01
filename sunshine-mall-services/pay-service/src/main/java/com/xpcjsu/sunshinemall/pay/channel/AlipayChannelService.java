package com.xpcjsu.sunshinemall.pay.channel;
import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.payment.common.models.AlipayTradeRefundResponse;
import com.alipay.easysdk.payment.facetoface.models.AlipayTradePrecreateResponse;
import com.xpcjsu.sunshinemall.pay.config.AlipayProperties;
import com.xpcjsu.sunshinemall.pay.entity.PayTransaction;
import com.xpcjsu.sunshinemall.pay.entity.PayRefund;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 支付宝渠道最小实现（当面付预下单）
 * 为避免过度设计，此处先返回模拟二维码链接；待密钥配置完成后再接入SDK调用。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlipayChannelService {

    private final AlipayProperties properties;

    /**
     * 预下单，返回二维码地址
     *
     * @param pay 支付交易信息对象，包含订单号、金额等支付相关数据
     * @return 支付宝生成的二维码内容字符串，用于前端展示二维码；若失败或未启用则返回模拟链接
     */
    public String preCreate(PayTransaction pay) {

        // 如果支付宝支付未启用或关键配置缺失（如 appId、私钥、公钥），则返回模拟二维码链接
        if (!Boolean.TRUE.equals(properties.getEnabled()) || StringUtils.isAnyBlank(properties.getAppId(),
                properties.getMerchantPrivateKey(), properties.getAlipayPublicKey())) {

            log.warn("支付宝渠道未配置完整密钥，返回模拟二维码链接");
            return "https://mock.alipay.qr/" + pay.getPaySn();
        }

        try {

            // 使用 EasySDK 的当面付预创建接口发起支付请求
            String amount = pay.getAmount().setScale(2, java.math.RoundingMode.DOWN).toPlainString();
            // 设置支付主题，如果为空则使用默认值"订单支付"
            String subject = StringUtils.defaultIfBlank(pay.getSubject(), "订单支付");
            // 调用支付宝当面付预创建接口，生成支付二维码
            AlipayTradePrecreateResponse response = Factory.Payment.FaceToFace()
                    .preCreate(subject, pay.getPaySn(), amount);

            // 判断预下单是否成功：状态码为 "10000" 且返回了二维码内容
            if ("10000".equals(response.getCode()) && StringUtils.isNotBlank(response.getQrCode())) {
                log.info("[Alipay] 预下单成功 - paySn: {}, qr: {}", pay.getPaySn(), response.getQrCode());

                // 直接返回支付宝提供的二维码内容，由前端生成二维码（避免引入额外依赖）
                return response.getQrCode();
            }

            log.warn("[Alipay] 预下单失败 - paySn: {}, code: {}, msg: {}",
                    pay.getPaySn(), response.getCode(), response.getMsg());

        } catch (Exception e) {

            log.error("[Alipay] 预下单异常 - paySn: {}", pay.getPaySn(), e);
        }

        // 出现异常或失败时返回模拟二维码链接
        return "https://mock.alipay.qr/" + pay.getPaySn();
    }

    /**
     * 验签（基于 EasySDK 实现）
     *
     * @param params 来自支付宝异步通知的参数 Map
     * @return 验签通过返回 true，否则返回 false；若未启用或缺少配置则默认返回 true
     */
    public boolean verifyNotify(Map<String, String> params) {

        // 若支付宝支付未启用或未配置公钥，则跳过验签直接返回 true
        if (!Boolean.TRUE.equals(properties.getEnabled())
                || StringUtils.isBlank(properties.getAlipayPublicKey())) {
            log.warn("支付宝验签跳过（未配置公钥）");
            return true;
        }

        try {
            // 调用 EasySDK 提供的通用支付验签方法进行签名验证
            boolean ok = Factory.Payment.Common().verifyNotify(params);
            if (!ok) {
                log.warn("[Alipay] 验签失败: {}", params);
            }
            return ok;

        } catch (Exception e) {

            log.error("[Alipay] 验签异常", e);
            return false;
        }
    }

    /**
     * 退款（最小实现）：优先走 EasySDK 的通用退款接口；配置缺失时返回模拟退款号。
     * @param pay 原支付记录
     * @param refund 退款记录
     * @return 渠道退款单号；失败返回 null；配置缺失时返回模拟号
     */
    public String refund(PayTransaction pay, PayRefund refund) {
        if (!Boolean.TRUE.equals(properties.getEnabled())
                || StringUtils.isAnyBlank(properties.getAppId(), properties.getMerchantPrivateKey(), properties.getAlipayPublicKey())) {
            String mockNo = "MOCK-ALIPAY-REFUND-" + refund.getRefundSn();
            log.warn("支付宝退款渠道未配置完整密钥，返回模拟退款号: {}", mockNo);
            return mockNo;
        }
        try {
            String amount = refund.getAmount().setScale(2, java.math.RoundingMode.DOWN).toPlainString();
            String reason = StringUtils.defaultIfBlank(refund.getReason(), "申请退款");
            // 通过可选参数传入退款理由与幂等号
            AlipayTradeRefundResponse response = Factory.Payment.Common()
                    .optional("refund_reason", reason)
                    .optional("out_request_no", refund.getRefundSn())
                    .refund(pay.getPaySn(), amount);
            if ("10000".equals(response.getCode())) {
                String channelRefundNo = org.apache.commons.lang3.StringUtils.defaultIfBlank(response.getTradeNo(), response.getOutTradeNo());
                log.info("[Alipay] 退款成功 - refundSn: {}, channelRefundNo: {}, refundFee: {}",
                        refund.getRefundSn(), channelRefundNo, response.getRefundFee());
                return org.apache.commons.lang3.StringUtils.defaultIfBlank(channelRefundNo, "ALIPAY-REFUND-" + refund.getRefundSn());
            }
            log.warn("[Alipay] 退款失败 - refundSn: {}, code: {}, msg: {}",
                    refund.getRefundSn(), response.getCode(), response.getMsg());
        } catch (Exception e) {
            log.error("[Alipay] 退款异常 - refundSn: {}", refund.getRefundSn(), e);
        }
        return null;
    }
}
