package com.xpcjsu.sunshinemall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpcjsu.sunshinemall.framework.base.exception.BusinessException;
import com.xpcjsu.sunshinemall.framework.base.exception.ValidationException;
import com.xpcjsu.sunshinemall.framework.cache.core.CacheManager;
import com.xpcjsu.sunshinemall.framework.convention.errorcode.BusinessErrorCode;
import com.xpcjsu.sunshinemall.framework.idempotent.annotation.Idempotent;
import com.xpcjsu.sunshinemall.order.client.ProductSkuClient;
import com.xpcjsu.sunshinemall.order.client.StockClient;
import com.xpcjsu.sunshinemall.order.constant.CacheConstants;
import com.xpcjsu.sunshinemall.order.constant.OrderConstants;
import com.xpcjsu.sunshinemall.order.dto.external.ProductSkuDTO;
import com.xpcjsu.sunshinemall.order.dto.order.OrderCreateRequest;
import com.xpcjsu.sunshinemall.order.dto.order.OrderCreateResponse;
import com.xpcjsu.sunshinemall.order.dto.order.OrderDetailResponse;
import com.xpcjsu.sunshinemall.order.dto.common.SkuQuantityRequest;
import com.xpcjsu.sunshinemall.order.entity.OrderInfo;
import com.xpcjsu.sunshinemall.order.entity.OrderItem;
import com.xpcjsu.sunshinemall.order.enums.OrderStatus;
import com.xpcjsu.sunshinemall.order.mapper.OrderInfoMapper;
import com.xpcjsu.sunshinemall.order.mapper.OrderItemMapper;
import com.xpcjsu.sunshinemall.order.mq.message.OrderEventMessage;
import com.xpcjsu.sunshinemall.order.service.OrderService;
// 移除未使用的状态机依赖，避免不必要的注入与代码膨胀
import com.xpcjsu.sunshinemall.order.util.OrderIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductSkuClient productSkuClient;
    private final StockClient stockClient;
    private final OrderIdGenerator orderIdGenerator;
    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;

    @Override
    @Idempotent(key = "'order:create:' + #userId + ':' + #request.clientToken", prefix = "idempotent", expireTime = 120)
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateResponse createOrder(Long userId, OrderCreateRequest request) {
        // 1) 参数校验
        validateCreateOrderRequest(userId, request);

        List<SkuQuantityRequest> itemsReq = request.getItems();
        // 2) 查询SKU并校验状态与库存
        List<ProductSkuDTO> skuList = fetchAndValidateSkus(itemsReq);

        // 3) 生成订单ID/订单号
        //外部交互使用 orderNo, 内部使用 orderId
        long orderId = orderIdGenerator.nextId();
        String orderNo = "O" + orderId;

        // 4) 预占库存
        List<SkuQuantityRequest> lockedItems = preLockStock(itemsReq, orderId, orderNo);

        // 5) 构建订单项
        LocalDateTime now = LocalDateTime.now();
        List<OrderItem> orderItems = buildOrderItems(orderId, orderNo, itemsReq, skuList, now);
        BigDecimal totalAmount = computeTotalAmount(orderItems);

        BigDecimal freightAmount = BigDecimal.ZERO; // TODO: 运费可根据规则计算
        BigDecimal discountAmount = BigDecimal.ZERO; // TODO: 优惠金额可根据促销规则计算
        BigDecimal payAmount = computePayAmount(totalAmount, freightAmount, discountAmount);

        // 6) 构建订单主信息
        OrderInfo order = buildOrderInfo(orderId, orderNo, userId, totalAmount, payAmount,
                freightAmount, discountAmount, request, now);

        // 7) 持久化（失败释放预占库存）
        persistOrder(order, orderItems, lockedItems, orderId, orderNo);

        // 8) 发送订单创建事件
        //sendOrderEvent(OrderConstants.MQ.Tags.CREATED, orderId, orderNo, userId);

        // 9) 返回创建结果
        return OrderCreateResponse.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .payAmount(order.getPayAmount())
                .build();
    }


    //查询订单详情
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDetailResponse getOrderDetail(Long userId, String orderNo) {
        // 查询订单基本信息
        OrderInfo order = getOrderByOrderNo(userId, orderNo);

        if (order == null) {
            throw new BusinessException(BusinessErrorCode.SYSTEM_PARAM_ERROR, "订单不存在");
        }

        // 查询订单项
        List<OrderItem> orderItems = listOrderItems(order.getId());

        // 构建响应对象
        OrderDetailResponse orderDetailResponse = OrderDetailResponse.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .status(order.getStatus())
                .statusName(OrderStatus.fromCode(order.getStatus()).getDesc())
                .totalAmount(order.getTotalAmount())
                .paymentAmount(order.getPayAmount())
                .paymentTime(order.getPaymentTime())
                .createTime(order.getCreateTime())
                .updateTime(order.getUpdateTime())
                .items(orderItems)
                .build();

        cacheManager.set(CacheConstants.CACHE_KEY_ORDER_DETAIL + order.getId(), orderDetailResponse);

        return orderDetailResponse;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean paySuccess(Long userId, String orderNo, String paySn) {
        // 1) 查询并校验订单
        OrderInfo order = loadOrderOrThrow(userId, orderNo);
        ensurePayable(order);

        // 2) 确认扣减库存
        List<OrderItem> items = listOrderItems(order.getId());
        confirmDeductForPayment(order, items, "PAY");

        // 3) 更新订单状态与支付时间
        order.setStatus(OrderStatus.WAIT_SHIP.getCode());
        order.setPaymentTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        orderInfoMapper.updateById(order);

        // 4) 发送支付成功事件
        //sendOrderEvent(OrderConstants.MQ.Tags.PAID, order.getId(), orderNo, userId);

        return true;
    }


    //取消订单不携带原因
    @Override
    public boolean cancelOrder(Long userId, String orderNo) {
        // 兼容无原因取消的接口，复用带原因的实现，避免重复逻辑
        return cancelOrder(userId, orderNo, null);
    }

    /** 取消订单（释放预占库存） */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(Long userId, String orderNo, String reason) {
        // 1) 查询并校验订单
        OrderInfo order = loadOrderOrThrow(userId, orderNo);
        ensureCancellable(order);

        // 2) 释放库存
        List<OrderItem> items = listOrderItems(order.getId());
        unlockOrderStockForCancel(order, items, "CANCEL");

        // 3) 更新订单状态
        order.setStatus(OrderStatus.CANCELLED.getCode());
        order.setNote(StringUtils.defaultIfBlank(reason, order.getNote()));
        order.setUpdateTime(LocalDateTime.now());

        orderInfoMapper.updateById(order);

        // 4) 发送取消事件
        //sendOrderEvent(OrderConstants.MQ.Tags.CANCELLED, order.getId(), orderNo, userId);

        return true;
    }

    //根据订单号查询订单
    @Override
    public OrderInfo getOrderByOrderNo(Long userId, String orderNo) {
        return loadOrderOrThrow(userId, orderNo);
    }

    //查询订单项
    @Override
    public List<OrderItem> listOrderItems(Long orderId) {

        if (orderId == null) {
            throw new ValidationException(BusinessErrorCode.SYSTEM_PARAM_ERROR, "orderId不能为空");
        }

        return orderItemMapper.selectList(new QueryWrapper<OrderItem>()
                .eq("order_id", orderId)
                .eq("is_deleted", 0));
    }





    // ===================== 私有方法：公共逻辑封装，减少重复与圈复杂度 =====================

    //检验请求参数
    private void validateCreateOrderRequest(Long userId, OrderCreateRequest request) {
        if (userId == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGIN, "用户未登录");
        }
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new ValidationException(BusinessErrorCode.SYSTEM_PARAM_ERROR, "订单条目不能为空");
        }
        for (SkuQuantityRequest itemReq : request.getItems()) {
            if (itemReq.getSkuId() == null || itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                throw new ValidationException(BusinessErrorCode.SYSTEM_PARAM_ERROR, "SKU或数量非法");
            }
        }
    }

    // 获取商品SKU信息
    private List<ProductSkuDTO> fetchAndValidateSkus(List<SkuQuantityRequest> itemsReq) {

        List<ProductSkuDTO> skuList = new ArrayList<>();

        for (SkuQuantityRequest itemReq : itemsReq) {
            Long skuId = itemReq.getSkuId();
            //var是Java10引入的局部变量类型推断关键字,
            //编译器根据右侧表达式自动推断变量类型Result<ProductSkuDTO>
            var skuRes = productSkuClient.getSkuById(skuId);
            if (skuRes == null || skuRes.isFailure() || skuRes.getData() == null) {
                throw new BusinessException(BusinessErrorCode.PRODUCT_NOT_FOUND, "SKU不存在或服务异常");
            }

            ProductSkuDTO skuDTO = skuRes.getData();
            if (skuDTO.getStatus() == null || skuDTO.getStatus() != 1) {
                throw new BusinessException(BusinessErrorCode.PRODUCT_OFFLINE, "SKU已下架或禁用");
            }

            var checkRes = stockClient.checkStock(skuId, itemReq.getQuantity());
            if (checkRes == null || checkRes.isFailure()) {
                throw new BusinessException(BusinessErrorCode.SYSTEM_BUSY, "库存服务繁忙，请稍后重试");
            }

            if (!Boolean.TRUE.equals(checkRes.getData())) {
                throw new BusinessException(BusinessErrorCode.PRODUCT_INSUFFICIENT_STOCK, "库存不足，skuId=" + skuId);
            }
            skuList.add(skuDTO);
        }
        return skuList;
    }

    // 预占库存
    private List<SkuQuantityRequest> preLockStock(List<SkuQuantityRequest> itemsReq,
                                                  long orderId, String orderNo) {

        List<SkuQuantityRequest> lockedItems = new ArrayList<>();

        try {
            for (SkuQuantityRequest itemReq : itemsReq) {

                var lockRes = stockClient.lockStock(itemReq.getSkuId(),
                        itemReq.getQuantity(), orderId, "CREATE:" + orderNo);

                if (lockRes == null || lockRes.isFailure()) {

                    for (SkuQuantityRequest locked : lockedItems) {

                        try {
                            stockClient.unlockStock(locked.getSkuId(),
                                    locked.getQuantity(), orderId, "ROLLBACK:" + orderNo);

                        } catch (Exception e) {
                            log.warn("释放预占库存失败，skuId={}, qty={}, orderId={}",
                                    locked.getSkuId(), locked.getQuantity(), orderId);
                        }
                    }

                    throw new BusinessException(BusinessErrorCode.PRODUCT_INSUFFICIENT_STOCK, "库存预占失败");
                }

                lockedItems.add(itemReq);
            }

        } catch (RuntimeException e) {
            throw e;

        } catch (Exception e) {
            throw new BusinessException(BusinessErrorCode.SYSTEM_BUSY, "库存服务异常", e);
        }

        return lockedItems;
    }

    //统计各个sku的金额
    private List<OrderItem> buildOrderItems(long orderId, String orderNo,
                                            List<SkuQuantityRequest> itemsReq, List<ProductSkuDTO> skuList,
                                            LocalDateTime now) {

        List<OrderItem> orderItems = new ArrayList<>();

        for (int i = 0; i < itemsReq.size(); i++) {

            SkuQuantityRequest itemReq = itemsReq.get(i);
            ProductSkuDTO skuDTO = skuList.get(i);

            BigDecimal price = skuDTO.getPrice() == null ? BigDecimal.ZERO : skuDTO.getPrice();
            BigDecimal itemTotal = price.multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            String specJson = safeJson(skuDTO.getSpecMap());

            OrderItem item = OrderItem.builder()
                    .orderId(orderId)
                    .orderNo(orderNo)
                    .productId(skuDTO.getProductId())
                    .productName(skuDTO.getSkuName())
                    .productImage(skuDTO.getSkuImage())
                    .skuId(skuDTO.getId())
                    .skuCode(skuDTO.getSkuCode())
                    .skuName(skuDTO.getSkuName())
                    .skuSpec(specJson)
                    .price(price)
                    .quantity(itemReq.getQuantity())
                    .totalAmount(itemTotal)
                    .realAmount(itemTotal)
                    .createTime(now)
                    .updateTime(now)
                    .isDeleted(0)
                    .build();

            orderItems.add(item);
        }
        return orderItems;
    }

    //计算订单总金额
    private BigDecimal computeTotalAmount(List<OrderItem> orderItems) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : orderItems) {
            total = total.add(item.getTotalAmount() == null ? BigDecimal.ZERO : item.getTotalAmount());
        }
        return total;
    }

    //计算加上运费和折扣的支付金额
    private BigDecimal computePayAmount(BigDecimal totalAmount, BigDecimal freightAmount, BigDecimal discountAmount) {
        return (totalAmount == null ? BigDecimal.ZERO : totalAmount)
                .add(freightAmount == null ? BigDecimal.ZERO : freightAmount)
                .subtract(discountAmount == null ? BigDecimal.ZERO : discountAmount);
    }

    // 构建订单信息
    private OrderInfo buildOrderInfo(long orderId, String orderNo, Long userId,
                                     BigDecimal totalAmount, BigDecimal payAmount,
                                     BigDecimal freightAmount, BigDecimal discountAmount,
                                     OrderCreateRequest request, LocalDateTime now) {
        return OrderInfo.builder()
                .id(orderId)
                .orderNo(orderNo)
                .userId(userId)
                .totalAmount(totalAmount)
                .payAmount(payAmount)
                .freightAmount(freightAmount)
                .discountAmount(discountAmount)
                .payType(request.getPayType())
                .sourceType(request.getSourceType())
                .status(OrderStatus.WAIT_PAY.getCode())
                .orderType(request.getOrderType())
                .deliveryCompany(null)
                .deliverySn(null)
                .autoConfirmDay(OrderConstants.Biz.DEFAULT_AUTO_CONFIRM_DAY)
                .receiverName(request.getReceiverName())
                .receiverPhone(request.getReceiverPhone())
                .receiverProvince(request.getReceiverProvince())
                .receiverCity(request.getReceiverCity())
                .receiverDistrict(request.getReceiverDistrict())
                .receiverAddress(request.getReceiverAddress())
                .note(StringUtils.defaultIfBlank(request.getNote(), OrderConstants.Biz.DEFAULT_NOTE))
                .confirmStatus(0)
                .deleteStatus(0)
                .paymentTime(null)
                .deliveryTime(null)
                .receiveTime(null)
                .commentTime(null)
                .createTime(now)
                .updateTime(now)
                .createBy(String.valueOf(userId))
                .updateBy(String.valueOf(userId))
                .isDeleted(0)
                .build();
    }

    //存储订单
    private void persistOrder(OrderInfo order, List<OrderItem> orderItems,
                              List<SkuQuantityRequest> lockedItems, long orderId, String orderNo) {
        try {
            orderInfoMapper.insert(order);

            for (OrderItem item : orderItems) {
                orderItemMapper.insert(item);
            }

        } catch (Exception e) {

            for (SkuQuantityRequest locked : lockedItems) {
                try {
                    stockClient.unlockStock(locked.getSkuId(), locked.getQuantity(),
                            orderId, "DB_FAIL:" + orderNo);
                } catch (Exception ex) {
                    log.warn("DB失败释放库存异常，skuId={}, qty={}, orderId={}", locked.getSkuId(),
                            locked.getQuantity(), orderId);
                }
            }

            throw e;
        }
    }

    // 安全转换成JSON
    private String safeJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception ex) {
            return obj.toString();
        }
    }

    //向MQ发送订单事件
    private void sendOrderEvent(String tag, long orderId, String orderNo, Long userId) {
        String destination = OrderConstants.MQ.ORDER_EVENT_TOPIC + ":" + tag;
        OrderEventMessage msg = OrderEventMessage.builder()
                .eventType(tag)
                .orderId(orderId)
                .orderNo(orderNo)
                .userId(userId)
                .occurTime(LocalDateTime.now())
                .build();
        try {
            rocketMQTemplate.syncSend(destination, msg);
            log.info("订单事件已发送: destination={}, orderNo={}", destination, orderNo);
        } catch (Exception e) {
            log.warn("订单事件发送失败，orderNo={}, tag={}", orderNo, tag, e);
        }
    }

    //查询订单
    private OrderInfo loadOrderOrThrow(Long userId, String orderNo) {

        if (userId == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGIN, "用户未登录");
        }

        if (StringUtils.isBlank(orderNo)) {
            throw new ValidationException(BusinessErrorCode.SYSTEM_PARAM_ERROR, "订单号不能为空");
        }

        OrderInfo order = orderInfoMapper.selectOne(new QueryWrapper<OrderInfo>()
                .eq("order_no", orderNo)
                .eq("user_id", userId)
                .eq("is_deleted", 0));

        if (order == null) {
            throw new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND, "订单不存在");
        }

        return order;
    }

    //确认订单可取消
    private void ensureCancellable(OrderInfo order) {

        if (Objects.equals(order.getStatus(), OrderStatus.CANCELLED.getCode())) {
            throw new BusinessException(BusinessErrorCode.ORDER_CANCELLED, "订单已取消");
        }

        if (!Objects.equals(order.getStatus(), OrderStatus.WAIT_PAY.getCode())) {
            throw new BusinessException(BusinessErrorCode.ORDER_STATUS_ERROR, "仅待支付订单可取消");
        }
    }

    //确认订单可支付
    private void ensurePayable(OrderInfo order) {

        if (!Objects.equals(order.getStatus(), OrderStatus.WAIT_PAY.getCode())) {
            throw new BusinessException(BusinessErrorCode.ORDER_STATUS_ERROR, "非待付款订单不可支付确认");
        }
    }

    //取消订单释放库存
    private void unlockOrderStockForCancel(OrderInfo order, List<OrderItem> items, String tagPrefix) {
        for (OrderItem item : items) {

            try {
                stockClient.unlockStock(item.getSkuId(), item.getQuantity(), order.getId(),
                        tagPrefix + ":" + order.getOrderNo());

            } catch (Exception e) {
                log.warn("取消订单释放库存失败，skuId={}, qty={}, orderId={}",
                        item.getSkuId(), item.getQuantity(), order.getId());
            }

        }
    }

    //确认扣减库存
    private void confirmDeductForPayment(OrderInfo order, List<OrderItem> items, String tagPrefix) {

        for (OrderItem item : items) {

            var res = stockClient.confirmDeduct(item.getSkuId(), item.getQuantity(),
                    order.getId(), tagPrefix + ":" + order.getOrderNo());

            if (res == null || res.isFailure()) {
                throw new BusinessException(BusinessErrorCode.SYSTEM_BUSY, "库存确认扣减失败，请稍后重试");
            }

        }
    }
}
