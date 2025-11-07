package com.xpcjsu.sunshinemall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xpcjsu.sunshinemall.product.entity.SeckillProduct;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 秒杀商品 Mapper
 *
 * @author xpcjsu
 */
public interface SeckillProductMapper extends BaseMapper<SeckillProduct> {

    /**
     * 查询进行中的秒杀商品列表
     *
     * @param now 当前时间
     * @return 秒杀商品列表
     */
    @Select("SELECT * FROM seckill_product WHERE status = 1 " +
            "AND start_time <= #{now} AND end_time >= #{now} " +
            "AND is_deleted = 0 ORDER BY sort_order ASC, create_time DESC")
    List<SeckillProduct> selectInProgressSeckills(@Param("now") LocalDateTime now);

    /**
     * 扣减秒杀库存（乐观锁）
     *
     * @param id 秒杀商品ID
     * @param quantity 扣减数量
     * @param version 版本号
     * @return 影响行数
     */
    @Update("UPDATE seckill_product SET seckill_stock = seckill_stock - #{quantity}, " +
            "version = version + 1 " +
            "WHERE id = #{id} AND seckill_stock >= #{quantity} AND version = #{version} AND is_deleted = 0")
    int deductSeckillStock(@Param("id") Long id, 
                          @Param("quantity") Integer quantity, 
                          @Param("version") Integer version);

    /**
     * 回滚秒杀库存（乐观锁）
     *
     * @param id 秒杀商品ID
     * @param quantity 回滚数量
     * @param version 版本号
     * @return 影响行数
     */
    @Update("UPDATE seckill_product SET seckill_stock = seckill_stock + #{quantity}, " +
            "version = version + 1 " +
            "WHERE id = #{id} AND version = #{version} AND is_deleted = 0")
    int rollbackSeckillStock(@Param("id") Long id, 
                            @Param("quantity") Integer quantity, 
                            @Param("version") Integer version);

}

