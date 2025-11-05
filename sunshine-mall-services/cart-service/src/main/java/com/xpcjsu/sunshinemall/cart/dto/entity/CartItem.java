package com.xpcjsu.sunshinemall.cart.dto.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车条目实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id"})
@TableName("cart_item")
@Schema(name = "CartItem", description = "购物车条目")
public class CartItem implements Serializable {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "用户ID")
    @TableField("user_id")
    private Long userId;

    @Schema(description = "商品ID")
    @TableField("product_id")
    private Long productId;

    @Schema(description = "商品名称")
    @TableField("product_name")
    private String productName;

    @Schema(description = "商品图片")
    @TableField("product_image")
    private String productImage;

    @Schema(description = "SKU ID")
    @TableField("sku_id")
    private Long skuId;

    @Schema(description = "SKU编码")
    @TableField("sku_code")
    private String skuCode;

    @Schema(description = "SKU名称")
    @TableField("sku_name")
    private String skuName;

    @Schema(description = "SKU规格JSON")
    @TableField("sku_spec")
    private String skuSpec;

    @Schema(description = "单价")
    @TableField("price")
    private BigDecimal price;

    @Schema(description = "数量")
    @TableField("quantity")
    private Integer quantity;

    @Schema(description = "是否勾选（0-未勾选，1-已勾选）")
    @TableField("checked")
    private Integer checked;

    @Schema(description = "创建时间")
    @TableField("create_time")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField("update_time")
    private LocalDateTime updateTime;

    @Schema(description = "逻辑删除标识（0-未删除，1-已删除）")
    @TableField("is_deleted")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}