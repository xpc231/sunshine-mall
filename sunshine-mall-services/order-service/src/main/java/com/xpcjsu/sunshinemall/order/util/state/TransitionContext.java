package com.xpcjsu.sunshinemall.order.util.state;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 订单状态流转业务校验上下文
 */
@Data
@Builder
@Schema(name = "TransitionContext", description = "订单状态机流转时的业务校验上下文")
public class TransitionContext {

    @Schema(description = "是否已完成支付校验（支付成功）")
    private boolean paymentVerified;

    @Schema(description = "是否已准备发货（有物流信息、库存已扣减）")
    private boolean shippingPrepared;

    @Schema(description = "是否已确认收货")
    private boolean receiveConfirmed;

    @Schema(description = "是否需要管理员关闭订单（如超时关闭、风控关闭）")
    private boolean adminCloseRequired;

    @Schema(description = "取消订单申请是否通过（用户主动取消、库存失败等）")
    private boolean cancelApproved;
}