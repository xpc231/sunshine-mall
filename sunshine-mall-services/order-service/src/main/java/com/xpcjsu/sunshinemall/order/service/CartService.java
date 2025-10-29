package com.xpcjsu.sunshinemall.order.service;

import com.xpcjsu.sunshinemall.order.dto.AddCartItemRequest;
import com.xpcjsu.sunshinemall.order.dto.UpdateCartItemRequest;
import com.xpcjsu.sunshinemall.order.entity.CartItem;

import java.util.List;

/**
 * 购物车服务接口
 */
public interface CartService {

    /**
     * 新增购物车条目（若已存在相同SKU则叠加数量）
     * @param userId 用户ID
     * @param req    请求体
     */
    void addItem(Long userId, AddCartItemRequest req);

    /**
     * 更新购物车条目数量
     * @param userId 用户ID
     * @param req    请求体
     */
    void updateItem(Long userId, UpdateCartItemRequest req);

    /**
     * 删除购物车条目（逻辑删除）
     * @param userId 用户ID
     * @param skuId  SKU ID
     */
    void removeItem(Long userId, Long skuId);

    /**
     * 查询购物车条目列表
     * @param userId 用户ID
     * @return 购物车条目列表
     */
    List<CartItem> listItems(Long userId);
}