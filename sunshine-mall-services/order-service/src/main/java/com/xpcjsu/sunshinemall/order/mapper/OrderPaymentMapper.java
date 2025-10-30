package com.xpcjsu.sunshinemall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xpcjsu.sunshinemall.order.entity.OrderPayment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单支付记录 Mapper
 */
@Mapper
public interface OrderPaymentMapper extends BaseMapper<OrderPayment> {
}