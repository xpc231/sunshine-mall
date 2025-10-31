package com.xpcjsu.sunshinemall.pay.enums;

import lombok.Getter;

@Getter
public enum RefundStatus {
    APPLYING(0, "申请中"),
    SUCCESS(1, "成功"),
    FAIL(2, "失败");

    private final int code;
    private final String desc;

    RefundStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}