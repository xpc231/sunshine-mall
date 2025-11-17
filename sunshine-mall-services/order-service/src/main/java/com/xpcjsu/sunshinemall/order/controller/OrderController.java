package com.xpcjsu.sunshinemall.order.controller;

import com.xpcjsu.sunshinemall.framework.base.exception.BusinessException;
import com.xpcjsu.sunshinemall.framework.convention.errorcode.BusinessErrorCode;
import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import com.xpcjsu.sunshinemall.order.dto.order.OrderCancelRequest;
import com.xpcjsu.sunshinemall.order.dto.order.OrderCreateRequest;
import com.xpcjsu.sunshinemall.order.dto.order.OrderCreateResponse;
import com.xpcjsu.sunshinemall.order.dto.order.OrderDetailResponse;
import com.xpcjsu.sunshinemall.order.dto.order.OrderPaySuccessRequest;
import com.xpcjsu.sunshinemall.order.dto.order.OrderDeliverRequest;
import com.xpcjsu.sunshinemall.framework.common.feign.dto.LogisticsShipmentDTO;
import com.xpcjsu.sunshinemall.framework.common.feign.clients.LogisticsClient;
import com.xpcjsu.sunshinemall.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Validated
@Tag(name = "OrderController", description = "订单接口")
public class OrderController {

    private final OrderService orderService;
    private final LogisticsClient logisticsClient;

    @Operation(summary = "创建订单")
    @PostMapping("/create")
    public Result<OrderCreateResponse> createOrder(@RequestBody @Valid OrderCreateRequest request,
                                                   HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        OrderCreateResponse response = orderService.createOrder(userId, request);
        return Result.success(response, "创建成功");
    }


    @Operation(summary = "订单详情")
    @GetMapping("/{orderNo}")
    public Result<OrderDetailResponse> getOrderDetail(@PathVariable String orderNo,
                                                      HttpServletRequest httpRequest) {

        Long userId = getUserIdFromRequest(httpRequest);
        OrderDetailResponse response = orderService.getOrderDetail(userId, orderNo);
        return Result.success(response);
    }

    @Operation(summary = "支付成功回调（确认扣减库存，更新状态）")
    @PostMapping("/pay/success")
    public Result<Void> paySuccess(@RequestBody @Valid OrderPaySuccessRequest request,
                                   HttpServletRequest httpRequest) {

        Long userId = getUserIdFromRequest(httpRequest);
        orderService.paySuccess(userId, request.getOrderNo(), request.getPaySn());
        return Result.success();
    }

    @Operation(summary = "取消订单")
    @PostMapping("/cancel")
    public Result<Void> cancelOrder(@RequestBody @Valid OrderCancelRequest request,
                                    HttpServletRequest httpRequest) {

        Long userId = getUserIdFromRequest(httpRequest);
        orderService.cancelOrder(userId, request.getOrderNo());
        return Result.success();
    }

    @Operation(summary = "订单发货")
    @PutMapping("/{orderNo}/deliver")
    public Result<Void> deliver(@PathVariable String orderNo,
                                @RequestBody @Valid OrderDeliverRequest request,
                                HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        orderService.deliverOrder(userId, orderNo, request.getCarrierCode(), request.getCarrierName(), request.getSenderAddress(), request.getTrackingCode());
        return Result.success();
    }

    @Operation(summary = "查询订单对应运单")
    @GetMapping("/{orderNo}/shipment")
    public Result<LogisticsShipmentDTO> getShipment(@PathVariable String orderNo,
                                                    HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        orderService.getOrderByOrderNo(userId, orderNo);
        var res = logisticsClient.getByOrderNo(orderNo);
        if (res == null || res.isFailure()) {
            return Result.failure(res == null ? BusinessErrorCode.SYSTEM_BUSY : res.getCode(),
                    res == null ? "物流服务繁忙" : res.getMessage());
        }
        return Result.success(res.getData());
    }

    //TODO: 分页查询用户所有订单

    //TODO: 售后服务

    /**
     * 从请求中提取用户ID，优先使用网关透传的Header: X-User-Id；
     * 兼容下游服务内部调用的Header: userId；缺失则视为未登录。
     */
    private Long getUserIdFromRequest(HttpServletRequest request) {
        String userIdHeaderPrimary = request.getHeader("X-User-Id");
        String userIdHeaderCompat = request.getHeader("userId");
        String userIdStr = StringUtils.hasText(userIdHeaderPrimary) ? userIdHeaderPrimary : userIdHeaderCompat;
        if (!StringUtils.hasText(userIdStr)) {
            throw new BusinessException(
                    BusinessErrorCode.USER_NOT_LOGIN,
                    BusinessErrorCode.getDefaultMessage(BusinessErrorCode.USER_NOT_LOGIN)
            );
        }
        try {
            return Long.parseLong(userIdStr);
        } catch (NumberFormatException ex) {
            throw new BusinessException(
                    BusinessErrorCode.SYSTEM_PARAM_ERROR,
                    "userId格式错误"
            );
        }
    }
}
