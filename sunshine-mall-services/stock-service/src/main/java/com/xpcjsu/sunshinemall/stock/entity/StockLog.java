package com.xpcjsu.sunshinemall.stock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stock_log")
public class StockLog {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long skuId;
    private Integer operationType;
    private Integer quantity;
    private Integer beforeStock;
    private Integer afterStock;
    private Long orderId;
    private String remark;
    private LocalDateTime createTime;
    private String createBy;
}

