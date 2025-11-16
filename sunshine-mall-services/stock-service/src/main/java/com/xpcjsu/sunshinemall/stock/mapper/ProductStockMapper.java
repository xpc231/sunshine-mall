package com.xpcjsu.sunshinemall.stock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xpcjsu.sunshinemall.stock.entity.ProductStock;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface ProductStockMapper extends BaseMapper<ProductStock> {

    @Update("UPDATE product_stock SET available_stock = available_stock - #{quantity}, version = version + 1 WHERE sku_id = #{skuId} AND available_stock >= #{quantity} AND version = #{version}")
    int deductStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity, @Param("version") Integer version);

    @Update("UPDATE product_stock SET available_stock = available_stock - #{quantity}, locked_stock = locked_stock + #{quantity}, version = version + 1 WHERE sku_id = #{skuId} AND available_stock >= #{quantity} AND version = #{version}")
    int lockStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity, @Param("version") Integer version);

    @Update("UPDATE product_stock SET available_stock = available_stock + #{quantity}, locked_stock = locked_stock - #{quantity}, version = version + 1 WHERE sku_id = #{skuId} AND locked_stock >= #{quantity} AND version = #{version}")
    int unlockStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity, @Param("version") Integer version);

    @Update("UPDATE product_stock SET total_stock = total_stock + #{quantity}, available_stock = available_stock + #{quantity}, version = version + 1 WHERE sku_id = #{skuId}")
    int addStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);
}

