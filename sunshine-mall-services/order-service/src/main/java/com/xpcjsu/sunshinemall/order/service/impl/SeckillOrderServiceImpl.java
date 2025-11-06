package com.xpcjsu.sunshinemall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xpcjsu.sunshinemall.framework.base.exception.BusinessException;
import com.xpcjsu.sunshinemall.framework.base.exception.ValidationException;
import com.xpcjsu.sunshinemall.framework.distributedid.core.SnowflakeIdGenerator;
import com.xpcjsu.sunshinemall.order.dto.entity.SeckillOrder;
import com.xpcjsu.sunshinemall.order.mapper.SeckillOrderMapper;
import com.xpcjsu.sunshinemall.order.service.SeckillOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 秒杀订单服务实现
 *
 * @author xpcjsu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillOrderServiceImpl implements SeckillOrderService {

    private final SeckillOrderMapper seckillOrderMapper;
    private final SnowflakeIdGenerator idGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSeckillOrder(SeckillOrder seckillOrder) {
        // 参数验证
        validateSeckillOrder(seckillOrder);

        // 检查用户是否已购买过该秒杀商品（防重复购买）
        SeckillOrder existing = seckillOrderMapper.selectByUserIdAndSeckillProductId(
                seckillOrder.getUserId(), seckillOrder.getSeckillProductId());
        if (existing != null) {
            throw new BusinessException("SECKILL_ALREADY_PURCHASED", "用户已购买过该秒杀商品");
        }

        // 生成ID
        Long seckillOrderId = idGenerator.nextId();
        seckillOrder.setId(seckillOrderId);

        // 设置默认值
        if (seckillOrder.getStatus() == null) {
            seckillOrder.setStatus(0); // 0-下单中
        }
        if (seckillOrder.getQuantity() == null) {
            seckillOrder.setQuantity(1);
        }

        // 保存到数据库
        seckillOrderMapper.insert(seckillOrder);

        log.info("创建秒杀订单记录成功 - seckillOrderId: {}, userId: {}, seckillProductId: {}",
                seckillOrderId, seckillOrder.getUserId(), seckillOrder.getSeckillProductId());
        return seckillOrderId;
    }

    @Override
    public SeckillOrder getSeckillOrderByUserAndProduct(Long userId, Long seckillProductId) {
        if (userId == null) {
            throw new ValidationException("USER_ID_REQUIRED", "用户ID不能为空");
        }
        if (seckillProductId == null) {
            throw new ValidationException("SECKILL_PRODUCT_ID_REQUIRED", "秒杀商品ID不能为空");
        }

        return seckillOrderMapper.selectByUserIdAndSeckillProductId(userId, seckillProductId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSeckillOrderWithOrderInfo(Long seckillOrderId, Long orderId, String orderNo) {
        if (seckillOrderId == null) {
            throw new ValidationException("SECKILL_ORDER_ID_REQUIRED", "秒杀订单记录ID不能为空");
        }
        if (orderId == null) {
            throw new ValidationException("ORDER_ID_REQUIRED", "订单ID不能为空");
        }
        if (orderNo == null) {
            throw new ValidationException("ORDER_NO_REQUIRED", "订单编号不能为空");
        }

        SeckillOrder seckillOrder = seckillOrderMapper.selectById(seckillOrderId);
        if (seckillOrder == null) {
            throw new BusinessException("SECKILL_ORDER_NOT_FOUND", "秒杀订单记录不存在");
        }

        // 更新订单信息
        int updated = seckillOrderMapper.update(null,
                new LambdaUpdateWrapper<SeckillOrder>()
                        .set(SeckillOrder::getOrderId, orderId)
                        .set(SeckillOrder::getOrderNo, orderNo)
                        .set(SeckillOrder::getStatus, 1) // 1-已下单
                        .eq(SeckillOrder::getId, seckillOrderId));

        log.info("更新秒杀订单记录成功 - seckillOrderId: {}, orderId: {}, orderNo: {}",
                seckillOrderId, orderId, orderNo);
        return updated > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSeckillOrderStatus(Long seckillOrderId, Integer status) {
        if (seckillOrderId == null) {
            throw new ValidationException("SECKILL_ORDER_ID_REQUIRED", "秒杀订单记录ID不能为空");
        }
        if (status == null) {
            throw new ValidationException("STATUS_REQUIRED", "状态不能为空");
        }

        SeckillOrder seckillOrder = seckillOrderMapper.selectById(seckillOrderId);
        if (seckillOrder == null) {
            throw new BusinessException("SECKILL_ORDER_NOT_FOUND", "秒杀订单记录不存在");
        }

        int updated = seckillOrderMapper.update(null,
                new LambdaUpdateWrapper<SeckillOrder>()
                        .set(SeckillOrder::getStatus, status)
                        .eq(SeckillOrder::getId, seckillOrderId));

        log.info("更新秒杀订单状态成功 - seckillOrderId: {}, status: {}", seckillOrderId, status);
        return updated > 0;
    }

    /**
     * 验证秒杀订单参数
     */
    private void validateSeckillOrder(SeckillOrder seckillOrder) {
        if (seckillOrder.getSeckillProductId() == null) {
            throw new ValidationException("SECKILL_PRODUCT_ID_REQUIRED", "秒杀商品ID不能为空");
        }
        if (seckillOrder.getSkuId() == null) {
            throw new ValidationException("SKU_ID_REQUIRED", "SKU ID不能为空");
        }
        if (seckillOrder.getUserId() == null) {
            throw new ValidationException("USER_ID_REQUIRED", "用户ID不能为空");
        }
        if (seckillOrder.getSeckillPrice() == null) {
            throw new ValidationException("SECKILL_PRICE_REQUIRED", "秒杀价格不能为空");
        }
    }

}

