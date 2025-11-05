package com.xpcjsu.sunshinemall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xpcjsu.sunshinemall.order.dto.entity.OrderInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单主表 Mapper
 */
@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {
    // 基本CRUD由 BaseMapper 提供，如有自定义SQL可在此扩展
}