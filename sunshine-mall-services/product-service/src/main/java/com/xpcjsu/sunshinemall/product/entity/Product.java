package com.xpcjsu.sunshinemall.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品基础信息实体
 *
 * @author xpcjsu
 */
@Data
@TableName("product")
public class Product {

    /**
     * 商品ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品编码
     */
    private String productCode;

    /**
     * 品牌ID
     */
    private Long brandId;

    /**
     * 主图URL
     */
    private String mainImage;

    /**
     * 副图URL列表（JSON数组）
     */
    private String subImages;

    /**
     * 商品详情描述
     */
    private String detail;

    /**
     * 商品状态（0-下架，1-上架，2-预售）
     */
    private Integer status;

    /**
     * 销售数量
     */
    private Integer saleCount;

    /**
     * 浏览次数
     */
    private Integer viewCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 删除标识（0-未删除，1-已删除）
     */
    @TableLogic
    private Integer isDeleted;

}
