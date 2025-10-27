package com.xpcjsu.sunshinemall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xpcjsu.sunshinemall.product.entity.ProductSku;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商品SKU Mapper
 *
 * @author xpcjsu
 */
public interface ProductSkuMapper extends BaseMapper<ProductSku> {

    /**
     * 查询所有SKU ID
     * 用于初始化布隆过滤器
     *
     * @return SKU ID列表
     */
    @Select("SELECT id FROM product_sku WHERE deleted = 0")
    List<Long> selectAllSkuIds();

}
