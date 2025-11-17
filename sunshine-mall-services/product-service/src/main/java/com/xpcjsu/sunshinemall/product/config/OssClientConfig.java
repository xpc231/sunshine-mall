package com.xpcjsu.sunshinemall.product.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class OssClientConfig {
    private final OssProperties props;

    @Bean(destroyMethod = "shutdown")
    public OSS ossClient() {
        return new OSSClientBuilder().build(props.getEndpoint(), props.getAccessKeyId(), props.getAccessKeySecret());
    }
}