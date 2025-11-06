package com.xpcjsu.sunshinemall.order.controller;

import com.xpcjsu.sunshinemall.framework.base.exception.BusinessException;
import com.xpcjsu.sunshinemall.framework.convention.errorcode.BusinessErrorCode;
import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import com.xpcjsu.sunshinemall.order.dto.entity.SeckillOrder;
import com.xpcjsu.sunshinemall.order.service.SeckillOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 秒杀订单控制器
 * <p>
 * 注意：秒杀订单记录主要用于防重复购买，大部分操作是内部服务调用
 * 这里只提供必要的查询接口
 *
 * @author xpcjsu
 */
@RestController
@RequestMapping("/api/seckill-order")
@RequiredArgsConstructor
@Validated
@Tag(name = "SeckillOrderController", description = "秒杀订单接口")
public class SeckillOrderController {

    private final SeckillOrderService seckillOrderService;

    @Operation(summary = "查询用户是否已购买秒杀商品")
    @GetMapping("/check/{seckillProductId}")
    public Result<Boolean> checkUserPurchased(@PathVariable Long seckillProductId,
                                             HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        SeckillOrder seckillOrder = seckillOrderService.getSeckillOrderByUserAndProduct(userId, seckillProductId);
        return Result.success(seckillOrder != null, seckillOrder != null ? "已购买" : "未购买");
    }

    /**
     * 从请求中提取用户ID
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

