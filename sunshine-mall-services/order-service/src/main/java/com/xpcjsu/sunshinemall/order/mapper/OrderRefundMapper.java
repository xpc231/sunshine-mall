package com.xpcjsu.sunshinemall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xpcjsu.sunshinemall.order.entity.OrderRefund;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单退款记录 Mapper
 */
@Mapper
public interface OrderRefundMapper extends BaseMapper<OrderRefund> {
}