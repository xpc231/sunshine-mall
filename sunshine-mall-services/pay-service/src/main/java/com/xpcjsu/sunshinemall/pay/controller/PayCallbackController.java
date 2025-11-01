package com.xpcjsu.sunshinemall.pay.controller;

import com.xpcjsu.sunshinemall.pay.channel.WechatChannelService;
import com.xpcjsu.sunshinemall.pay.service.PayCallbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 支付渠道异步回调控制器（最小实现）
 */
@RestController
@RequestMapping("/api/pay/notify")
@RequiredArgsConstructor
@Slf4j
public class PayCallbackController {

    private final PayCallbackService payCallbackService;
    private final WechatChannelService wechatChannelService;

    /**
     * 支付宝回调
     * 返回字符串"success"表示处理成功（符合支付宝回调协议）
     */
    @PostMapping("/alipay")
    public String alipayNotify(@RequestParam Map<String, String> params) {
        try {
            if (params == null || StringUtils.isBlank(params.get("out_trade_no"))) {
                log.warn("支付宝回调参数缺失: {}", params);
                return "failure";
            }
            boolean ok = payCallbackService.handleAlipayNotify(params);
            return ok ? "success" : "failure";
        } catch (Exception e) {
            log.error("支付宝回调处理异常", e);
            return "failure";
        }
    }

    /**
     * 微信回调
     * 返回字符串"success"表示处理成功（符合微信回调协议）
     */
    @PostMapping("/wechat")
    public ResponseEntity<String> wechatNotify(@RequestBody String body,
                                               @RequestHeader(value = "Wechatpay-Serial", required = false) String serial,
                                               @RequestHeader(value = "Wechatpay-Signature", required = false) String signature,
                                               @RequestHeader(value = "Wechatpay-Timestamp", required = false) String timestamp,
                                               @RequestHeader(value = "Wechatpay-Nonce", required = false) String nonce) {
        try {
            if (StringUtils.isBlank(body)) {
                log.warn("微信回调报文为空");
                return ResponseEntity.ok("failure");
            }
            boolean ok = payCallbackService.handleWechatNotify(body, serial, signature, timestamp, nonce);
            return ResponseEntity.ok(ok ? "success" : "failure");
        } catch (Exception e) {
            log.error("微信回调处理异常", e);
            return ResponseEntity.ok("failure");
        }
    }
}