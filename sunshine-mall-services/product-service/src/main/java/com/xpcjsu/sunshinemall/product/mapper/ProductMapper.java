package com.xpcjsu.sunshinemall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xpcjsu.sunshinemall.product.entity.Product;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商品 Mapper
 *
 * @author xpcjsu
 */
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 查询所有商品ID
     * 用于初始化布隆过滤器
     *
     * @return 商品ID列表
     */
    @Select("SELECT id FROM product WHERE deleted = 0")
    List<Long> selectAllProductIds();

}
