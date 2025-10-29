package com.xpcjsu.sunshinemall.order.controller;

import com.xpcjsu.sunshinemall.framework.base.exception.BusinessException;
import com.xpcjsu.sunshinemall.framework.convention.errorcode.BusinessErrorCode;
import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import com.xpcjsu.sunshinemall.order.dto.AddCartItemRequest;
import com.xpcjsu.sunshinemall.order.dto.UpdateCartItemRequest;
import com.xpcjsu.sunshinemall.order.entity.CartItem;
import com.xpcjsu.sunshinemall.order.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 购物车控制器
 * 提供购物车增删改查的REST接口。
 */
@RestController
@RequestMapping("/api/cart")
@Tag(name = "购物车接口", description = "购物车增删改查")
@Validated
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * 新增购物车条目（若存在相同SKU则叠加数量）
     * @param req 请求体
     * @param request HTTP请求对象，用于解析用户ID
     * @return 标准响应
     */
    @PostMapping("/items")
    @Operation(summary = "新增购物车条目")
    public Result<Void> addItem(@RequestBody @Valid AddCartItemRequest req, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        cartService.addItem(userId, req);
        return Result.success();
    }

    /**
     * 更新购物车条目数量
     * @param req 请求体
     * @param request HTTP请求对象，用于解析用户ID
     * @return 标准响应
     */
    @PutMapping("/items")
    @Operation(summary = "更新购物车条目数量")
    public Result<Void> updateItem(@RequestBody @Valid UpdateCartItemRequest req, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        cartService.updateItem(userId, req);
        return Result.success();
    }

    /**
     * 删除购物车条目（逻辑删除）
     * @param skuId SKU ID
     * @param request HTTP请求对象，用于解析用户ID
     * @return 标准响应
     */
    @DeleteMapping("/items/{skuId}")
    @Operation(summary = "删除购物车条目")
    public Result<Void> removeItem(@PathVariable Long skuId, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        cartService.removeItem(userId, skuId);
        return Result.success();
    }

    /**
     * 查询购物车条目列表
     * @param request HTTP请求对象，用于解析用户ID
     * @return 购物车条目列表
     */
    @GetMapping("/items")
    @Operation(summary = "查询购物车条目")
    public Result<List<CartItem>> listItems(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return Result.success(cartService.listItems(userId));
    }

    /**
     * 从请求中提取用户ID，优先使用网关透传的Header: X-User-Id；
     * 兼容查询参数 userId；缺失则视为未登录。
     */
    private Long getUserIdFromRequest(HttpServletRequest request) {
        String userIdHeader = request.getHeader("X-User-Id");
        String userIdParam = request.getParameter("userId");
        String userIdStr = StringUtils.hasText(userIdHeader) ? userIdHeader : userIdParam;
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
