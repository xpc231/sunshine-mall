package com.xpcjsu.sunshinemall.pay.enums;

import lombok.Getter;

@Getter
public enum PayStatus {
    INIT(0, "未支付"),
    SUCCESS(1, "支付成功"),
    FAIL(2, "支付失败"),
    CLOSED(3, "已关闭"),
    REFUND_PART(4, "部分退款"),
    REFUND_ALL(5, "全额退款");

    private final int code;
    private final String desc;

    PayStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}