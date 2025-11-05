package com.xpcjsu.sunshinemall.cart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.xpcjsu.sunshinemall.framework.cache.core.CacheManager;
import com.xpcjsu.sunshinemall.cart.dto.constant.CacheConstants;
import com.xpcjsu.sunshinemall.cart.dto.request.AddCartItemRequest;
import com.xpcjsu.sunshinemall.cart.dto.request.UpdateCartItemRequest;
import com.xpcjsu.sunshinemall.cart.dto.entity.CartItem;
import com.xpcjsu.sunshinemall.cart.mapper.CartItemMapper;
import com.xpcjsu.sunshinemall.cart.service.CartService;
import com.xpcjsu.sunshinemall.framework.base.exception.ValidationException;
import com.xpcjsu.sunshinemall.framework.convention.errorcode.BusinessErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 购物车服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartItemMapper cartItemMapper;
    private final CacheManager cacheManager;

    /**
     * 新增购物车条目（若已存在相同SKU则叠加数量）
     * @param userId 用户ID
     * @param req    请求体
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addItem(Long userId, AddCartItemRequest req) {
        if (userId == null || req == null || req.getSkuId() == null) {
            throw new ValidationException(BusinessErrorCode.SYSTEM_PARAM_ERROR, "参数为空：userId/req/skuId");
        }
        if (req.getQuantity() != null && req.getQuantity() <= 0) {
            throw new ValidationException(BusinessErrorCode.SYSTEM_PARAM_ERROR, "数量必须大于0");
        }

        // 查询判断是否已有相同SKU条目
        CartItem exist = cartItemMapper.selectOne(new QueryWrapper<CartItem>()
                .eq("user_id", userId)
                .eq("sku_id", req.getSkuId()));
        if (exist != null) {

            int newQty = exist.getQuantity() + (req.getQuantity() == null ? 1 : req.getQuantity());

            cartItemMapper.update(null, new UpdateWrapper<CartItem>()
                    .set("quantity", newQty)
                    .eq("id", exist.getId()));

            log.info("购物车叠加数量，userId={}, skuId={}, oldQty={}, newQty={}",
                    userId, req.getSkuId(), exist.getQuantity(), newQty);

        } else {
            CartItem item = CartItem.builder()
                    .userId(userId)
                    .productId(req.getProductId())
                    .productName(req.getProductName())
                    .productImage(req.getProductImage())
                    .skuId(req.getSkuId())
                    .skuCode(req.getSkuCode())
                    .skuName(req.getSkuName())
                    .skuSpec(req.getSkuSpec())
                    .price(req.getPrice())
                    .quantity(req.getQuantity() == null ? 1 : req.getQuantity())
                    .checked(1)
                    .build();
            cartItemMapper.insert(item);
            log.info("购物车新增条目，userId={}, skuId={}, qty={}", userId, req.getSkuId(), item.getQuantity());
        }
        // 缓存刷新留待后续引入 Redis 缓存装饰器
    }

    /**
     * 更新购物车条目数量
     * @param userId 用户ID
     * @param req    请求体
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateItem(Long userId, UpdateCartItemRequest req) {
        if (userId == null || req == null || req.getSkuId() == null || req.getQuantity() == null) {
            throw new ValidationException(BusinessErrorCode.SYSTEM_PARAM_ERROR, "参数为空：userId/req/skuId/quantity");
        }
        if (req.getQuantity() <= 0) {
            throw new ValidationException(BusinessErrorCode.SYSTEM_PARAM_ERROR, "数量必须大于0");
        }
        cartItemMapper.update(null, new UpdateWrapper<CartItem>()
                .set("quantity", req.getQuantity())
                .eq("user_id", userId)
                .eq("sku_id", req.getSkuId()));
        log.info("购物车更新数量，userId={}, skuId={}, qty={}", userId, req.getSkuId(), req.getQuantity());
    }

    /**
     * 删除购物车条目（逻辑删除）
     * @param userId 用户ID
     * @param skuId  SKU ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItem(Long userId, Long skuId) {
        if (userId == null || skuId == null) {
            throw new ValidationException(BusinessErrorCode.SYSTEM_PARAM_ERROR, "参数为空：userId/skuId");
        }
        QueryWrapper<CartItem> qw = new QueryWrapper<CartItem>()
                .eq("user_id", userId)
                .eq("sku_id", skuId);
        CartItem exist = cartItemMapper.selectOne(qw);
        if (exist != null) {
            cartItemMapper.deleteById(exist.getId());
            log.info("购物车删除条目，userId={}, skuId={}", userId, skuId);
        } else {
            log.warn("购物车删除失败，条目不存在，userId={}, skuId={}", userId, skuId);
        }
    }

    @Override
    public List<CartItem> listItems(Long userId) {
        if (userId == null) {
            throw new ValidationException(BusinessErrorCode.SYSTEM_PARAM_ERROR, "参数为空：userId");
        }

        // 1. 先从缓存中查询
        String cacheKey = CacheConstants.CART_ITEM_KEY_PREFIX + userId;
        List<CartItem> cachedItems = (List<CartItem>) cacheManager.get(cacheKey, CartItem.class);

        if (cachedItems != null && !cachedItems.isEmpty()) {
            // 缓存命中，直接返回
            return cachedItems;
        }

        // 2. 缓存未命中，查询数据库
        List<CartItem> cartItems = cartItemMapper.selectList(new QueryWrapper<CartItem>()
                .eq("user_id", userId)
                .orderByDesc("update_time"));

        // 3. 将查询结果添加到缓存中
        cacheManager.set(cacheKey, cartItems);

        return cartItems;
    }

}