package com.xpcjsu.sunshinemall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xpcjsu.sunshinemall.order.dto.entity.SeckillOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 秒杀订单记录 Mapper
 *
 * @author xpcjsu
 */
public interface SeckillOrderMapper extends BaseMapper<SeckillOrder> {

    /**
     * 查询用户是否已购买过该秒杀商品
     *
     * @param userId 用户ID
     * @param seckillProductId 秒杀商品ID
     * @return 秒杀订单记录
     */
    @Select("SELECT * FROM seckill_order WHERE user_id = #{userId} " +
            "AND seckill_product_id = #{seckillProductId} " +
            "AND status IN (0, 1) LIMIT 1")
    SeckillOrder selectByUserIdAndSeckillProductId(@Param("userId") Long userId,
                                                   @Param("seckillProductId") Long seckillProductId);

}

