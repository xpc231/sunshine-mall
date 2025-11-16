package com.xpcjsu.sunshinemall.stock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("product_stock")
public class ProductStock {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long skuId;
    private Integer totalStock;
    private Integer availableStock;
    private Integer lockedStock;
    @Version
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

