package com.xpcjsu.sunshinemall.cart.dto.request;

import com.xpcjsu.sunshinemall.cart.dto.common.SkuQuantityRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "UpdateCartItemRequest", description = "更新购物车条目请求")
public class UpdateCartItemRequest extends SkuQuantityRequest {
    // 继承SkuQuantityRequest，无需额外字段
}