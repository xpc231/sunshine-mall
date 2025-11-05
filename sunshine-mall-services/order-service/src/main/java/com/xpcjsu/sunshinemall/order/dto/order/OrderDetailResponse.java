package com.xpcjsu.sunshinemall.order.dto.order;

import com.xpcjsu.sunshinemall.order.dto.entity.OrderItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "OrderDetailResponse", description = "订单详情响应")
public class OrderDetailResponse {
    private Long orderId;
    private String orderNo;
    private Long userId;
    private Integer status;
    private String statusName;
    private BigDecimal totalAmount;
    private BigDecimal paymentAmount;
    private LocalDateTime paymentTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @Schema(description = "订单条目列表")
    private List<OrderItem> items;
}