package com.xpcjsu.sunshinemall.order.dto.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 订单状态枚举
 */
@Schema(name = "OrderStatus", description = "订单状态枚举（0-待付款，1-待发货，2-已发货，3-已完成，4-已关闭，5-已取消）")
public enum OrderStatus {
    WAIT_PAY(0, "待付款"),
    WAIT_SHIP(1, "待发货"),
    SHIPPED(2, "已发货"),
    COMPLETED(3, "已完成"),
    CLOSED(4, "已关闭"),
    CANCELLED(5, "已取消");

    private final int code;
    private final String desc;

    OrderStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static OrderStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        //Java编译器会为每个枚举类型自动添加values()
        for (OrderStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }
}