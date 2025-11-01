package com.xpcjsu.sunshinemall.pay.service;

import java.util.Map;

/**
 * 支付渠道回调业务接口
 */
public interface PayCallbackService {

    /**
     * 处理支付宝回调
     * @param params 表单参数
     * @return 是否处理成功
     */
    boolean handleAlipayNotify(Map<String, String> params);

    /**
     * 处理微信回调（V3）
     * @param body 回调报文（JSON）
     * @param serial 平台证书序列号
     * @param signature 平台签名
     * @param timestamp 回调时间戳
     * @param nonce 回调随机串
     * @return 是否处理成功
     */
    boolean handleWechatNotify(String body, String serial, String signature, String timestamp, String nonce);
}