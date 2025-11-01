package com.xpcjsu.sunshinemall.pay.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信支付渠道配置属性（V3）
 */
@Data
@Component
@ConfigurationProperties(prefix = "pay.wechat")
public class WechatProperties {
    /** 是否启用微信渠道 */
    private Boolean enabled = false;
    /** 微信商户ID */
    private String mchId;
    /** 微信应用ID */
    private String appId;
    /** 商户私钥（用于请求签名） */
    private String merchantPrivateKey;
    /** 平台证书序列号（用于签名头） */
    private String serialNo;
    /** APIv3 密钥（用于回调解密） */
    private String apiV3Key;
    /** 异步回调通知地址 */
    private String notifyUrl;
}