package com.xpcjsu.sunshinemall.pay.config;

import com.alipay.easysdk.kernel.Config;
import com.alipay.easysdk.factory.Factory;
import org.apache.commons.lang3.StringUtils;
import lombok.Data;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class AlipayConfig {

    @Bean
    public Config config(AlipayProperties payProperties) {

        Config config = new Config();
        config.protocol = payProperties.getProtocol();
        config.gatewayHost = payProperties.getGatewayHost();
        // 兼容 signType 与 signing-algorithm 两种配置键名
        config.signType = StringUtils.defaultIfBlank(payProperties.getSignType(), payProperties.getSigningAlgorithm());
        config.appId = payProperties.getAppId();
        config.merchantPrivateKey = payProperties.getMerchantPrivateKey();
        config.alipayPublicKey = payProperties.getAlipayPublicKey();
        config.notifyUrl = payProperties.getNotifyUrl();
        // 设置全局配置到 EasySDK 工厂，后续可直接使用 Factory.Payment.FaceToFace()
        Factory.setOptions(config);
        return config;
    }
}
