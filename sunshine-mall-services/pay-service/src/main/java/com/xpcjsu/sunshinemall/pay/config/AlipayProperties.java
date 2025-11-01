package com.xpcjsu.sunshinemall.pay.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * 支付宝渠道配置属性
 */
@Configuration
@Data
@ConfigurationProperties(prefix = "alipay.easy")
public class AlipayProperties {

    /**
     * 支付宝支付协议类型
     * 用于指定与支付宝通信时使用的协议格式
     */
    private String protocol;

    /**
     * 支付宝网关地址
     * 用于指定支付宝API接口的访问地址
     */
    private String gatewayHost;

    /**
     * 签名算法类型
     * 用于指定数据签名时使用的算法类型，如RSA2等
     */
    private String signType;
    /** 为兼容历史配置，支持 signType/signingAlgorithm 两种键名 */
    private String signingAlgorithm;

    /**
     * 应用ID
     * 支付宝分配给开发者的应用唯一标识
     */
    private String appId;

    /**
     * 商户私钥
     * 商户生成的私钥，用于数据签名和身份认证
     */
    private String merchantPrivateKey;

    /**
     * 支付宝公钥
     * 支付宝提供的公钥，用于验证支付宝返回数据的真实性
     */
    private String alipayPublicKey;

    /**
     * 异步通知地址
     * 支付宝服务器异步通知商户服务器的回调地址
     */
    private String notifyUrl;

    /** 是否启用支付宝渠道 */
    private Boolean enabled = false;
    /** 网关完整地址（可选），为空时根据 protocol + gatewayHost 采用默认网关 */
    private String serverUrl;
    /** 字符集，默认 UTF-8 */
    private String charset = "UTF-8";

}