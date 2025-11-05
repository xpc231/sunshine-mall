package com.xpcjsu.sunshinemall.cart.controller;

import com.xpcjsu.sunshinemall.framework.base.exception.BusinessException;
import com.xpcjsu.sunshinemall.framework.convention.errorcode.BusinessErrorCode;
import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import com.xpcjsu.sunshinemall.cart.dto.request.AddCartItemRequest;
import com.xpcjsu.sunshinemall.cart.dto.request.UpdateCartItemRequest;
import com.xpcjsu.sunshinemall.cart.dto.entity.CartItem;
import com.xpcjsu.sunshinemall.cart.service.CartService;
import com.xpcjsu.sunshinemall.framework.common.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
     * @return 标准响应
     */
    @PostMapping("/items")
    @Operation(summary = "新增购物车条目")
    public Result<Void> addItem(@RequestBody @Valid AddCartItemRequest req) {
        Long userId = getCurrentUserId();
        cartService.addItem(userId, req);
        return Result.success();
    }

    /**
     * 更新购物车条目数量
     * @param req 请求体
     * @return 标准响应
     */
    @PutMapping("/items")
    @Operation(summary = "更新购物车条目数量")
    public Result<Void> updateItem(@RequestBody @Valid UpdateCartItemRequest req) {
        Long userId = getCurrentUserId();
        cartService.updateItem(userId, req);
        return Result.success();
    }

    /**
     * 删除购物车条目（逻辑删除）
     * @param skuId SKU ID
     * @return 标准响应
     */
    @DeleteMapping("/items/{skuId}")
    @Operation(summary = "删除购物车条目")
    public Result<Void> removeItem(@PathVariable Long skuId) {
        Long userId = getCurrentUserId();
        cartService.removeItem(userId, skuId);
        return Result.success();
    }

    /**
     * 查询购物车条目列表
     * @return 购物车条目列表
     */
    @GetMapping("/items")
    @Operation(summary = "查询购物车条目")
    public Result<List<CartItem>> listItems() {
        Long userId = getCurrentUserId();
        return Result.success(cartService.listItems(userId));
    }

    /**
     * 获取当前登录用户ID
     * @return 用户ID
     */
    private Long getCurrentUserId() {
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGIN);
        }
        return userId;
    }
}