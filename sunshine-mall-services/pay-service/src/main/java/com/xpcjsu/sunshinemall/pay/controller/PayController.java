package com.xpcjsu.sunshinemall.pay.controller;

import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import com.xpcjsu.sunshinemall.pay.dto.pay.PayCreateRequest;
import com.xpcjsu.sunshinemall.pay.dto.pay.PayCreateResponse;
import com.xpcjsu.sunshinemall.pay.dto.pay.PayMockCallbackRequest;
import com.xpcjsu.sunshinemall.pay.dto.pay.PayQueryResponse;
import com.xpcjsu.sunshinemall.pay.dto.refund.RefundCreateRequest;
import com.xpcjsu.sunshinemall.pay.dto.refund.RefundCreateResponse;
import com.xpcjsu.sunshinemall.pay.service.PayService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 支付控制器
 */
@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
@Validated
public class PayController {

    private final PayService payService;

    /**
     * 创建支付
     */
    @PostMapping("/create")
    public Result<PayCreateResponse> createPay(@RequestHeader("userId") Long userId,
                                               @Valid @RequestBody PayCreateRequest request) {
        PayCreateResponse resp = payService.createPay(userId, request);
        return Result.success(resp);
    }

    /**
     * 查询支付
     */
    @GetMapping("/{payNo}")
    public Result<PayQueryResponse> queryPay(@PathVariable String payNo) {
        PayQueryResponse resp = payService.queryPay(payNo);
        return Result.success(resp);
    }

    /**
     * 模拟支付成功回调（本地联调用）
     */
    @PostMapping("/mock/callback")
    public Result<Boolean> mockPaySuccess(@RequestHeader("userId") Long userId,
                                          @Valid @RequestBody PayMockCallbackRequest request) {
        boolean success = payService.mockPaySuccess(userId, request);
        return Result.success(success);
    }

    /**
     * 创建退款
     */
    @PostMapping("/refund/create")
    public Result<RefundCreateResponse> createRefund(@RequestHeader("userId") Long userId,
                                                     @Valid @RequestBody RefundCreateRequest request) {
        RefundCreateResponse resp = payService.createRefund(userId, request);
        return Result.success(resp);
    }
}