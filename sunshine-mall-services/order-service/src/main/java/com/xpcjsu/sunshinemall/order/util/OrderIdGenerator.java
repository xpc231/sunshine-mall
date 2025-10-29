package com.xpcjsu.sunshinemall.order.util;

import com.xpcjsu.sunshinemall.framework.distributedid.core.SnowflakeIdGenerator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 订单ID与订单号生成器
 */
@Component
@Schema(name = "OrderIdGenerator", description = "基于雪花算法的订单ID与订单号生成器")
@RequiredArgsConstructor
public class OrderIdGenerator {

    private final SnowflakeIdGenerator snowflakeIdGenerator;

    /**
     * 生成分布式唯一ID
     */
    public long nextId() {
        return snowflakeIdGenerator.nextId();
    }

    /**
     * 生成订单号（规则："O" + 雪花ID）
     */
    public String nextOrderNo() {
        return "O" + nextId();
    }
}