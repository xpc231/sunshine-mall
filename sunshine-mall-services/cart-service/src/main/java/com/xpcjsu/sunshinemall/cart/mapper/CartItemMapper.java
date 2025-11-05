package com.xpcjsu.sunshinemall.cart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xpcjsu.sunshinemall.cart.dto.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 购物车条目 Mapper
 */
@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {
}