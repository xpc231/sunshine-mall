package com.xpcjsu.sunshinemall.order.service;

import com.xpcjsu.sunshinemall.order.dto.order.OrderCreateRequest;
import com.xpcjsu.sunshinemall.order.dto.order.OrderCreateResponse;
import com.xpcjsu.sunshinemall.order.dto.order.OrderDetailResponse;
import com.xpcjsu.sunshinemall.order.dto.entity.OrderInfo;
import com.xpcjsu.sunshinemall.order.dto.entity.OrderItem;

import java.util.List;

/**
 * 订单服务接口
 */
public interface OrderService {

    /**
     * 创建订单
     * @param userId 用户ID
     * @param request 创建请求体
     * @return 创建响应
     */
    OrderCreateResponse createOrder(Long userId, OrderCreateRequest request);

    /**
     * 取消订单
     * @param userId 用户ID
     * @param orderNo 订单编号
     * @return 是否成功
     */
    boolean cancelOrder(Long userId, String orderNo);

    /**
     * 取消订单（带原因）
     * @param userId 用户ID
     * @param orderNo 订单编号
     * @param reason 取消原因
     * @return 是否成功
     */
    boolean cancelOrder(Long userId, String orderNo, String reason);

    /**
     * 支付成功回调
     * @param userId 用户ID
     * @param orderNo 订单编号
     * @param paySn 支付流水号
     * @return 是否成功
     */
    boolean paySuccess(Long userId, String orderNo, String paySn);

    /**
     * 获取订单详情
     * @param userId 用户ID
     * @param orderNo 订单编号
     * @return 订单详情响应
     */
    OrderDetailResponse getOrderDetail(Long userId, String orderNo);

    /**
     * 根据订单编号获取订单信息
     * @param userId 用户ID
     * @param orderNo 订单编号
     * @return 订单信息
     */
    OrderInfo getOrderByOrderNo(Long userId, String orderNo);

    /**
     * 获取订单项列表
     * @param orderId 订单ID
     * @return 订单项列表
     */
    List<OrderItem> listOrderItems(Long orderId);

    /**
     * 发货（创建运单并更新订单为已发货）
     * @param userId 用户ID
     * @param orderNo 订单编号
     * @param carrierCode 物流公司编码
     * @param carrierName 物流公司名称
     * @param senderAddress 发件地址
     * @param trackingCode 可选追踪码
     * @return 是否成功
     */
    boolean deliverOrder(Long userId, String orderNo, String carrierCode, String carrierName, String senderAddress, String trackingCode);
}