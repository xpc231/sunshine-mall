package com.xpcjsu.sunshinemall.product.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xpcjsu.sunshinemall.framework.base.exception.BusinessException;
import com.xpcjsu.sunshinemall.framework.base.exception.ValidationException;
import com.xpcjsu.sunshinemall.framework.distributedid.core.SnowflakeIdGenerator;
import com.xpcjsu.sunshinemall.product.constant.ProductConstants;
import com.xpcjsu.sunshinemall.product.dto.SeckillProductDTO;
import com.xpcjsu.sunshinemall.product.entity.ProductSku;
import com.xpcjsu.sunshinemall.product.entity.SeckillProduct;
import com.xpcjsu.sunshinemall.product.mapper.ProductSkuMapper;
import com.xpcjsu.sunshinemall.product.mapper.SeckillProductMapper;
import com.xpcjsu.sunshinemall.product.service.SeckillProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 秒杀商品服务实现
 *
 * @author xpcjsu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillProductServiceImpl implements SeckillProductService {

    private final SeckillProductMapper seckillProductMapper;
    private final ProductSkuMapper productSkuMapper;
    private final SnowflakeIdGenerator idGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSeckillProduct(SeckillProductDTO seckillProductDTO) {
        // 参数验证
        validateSeckillProduct(seckillProductDTO);

        // 验证SKU是否存在且启用
        ProductSku sku = productSkuMapper.selectById(seckillProductDTO.getSkuId());

        if (sku == null) {
            throw new BusinessException("SKU_NOT_FOUND", "SKU不存在");
        }
        if (!ProductConstants.SkuStatus.ENABLED.equals(sku.getStatus())) {
            throw new BusinessException("SKU_DISABLED", "SKU已禁用");
        }

        // 验证时间范围
        if (seckillProductDTO.getStartTime().isAfter(seckillProductDTO.getEndTime())) {
            throw new ValidationException("TIME_RANGE_INVALID", "开始时间不能晚于结束时间");
        }

        // 检查该SKU是否已有秒杀活动
        SeckillProduct existing = seckillProductMapper.selectOne(
                new LambdaQueryWrapper<SeckillProduct>()
                        .eq(SeckillProduct::getSkuId, seckillProductDTO.getSkuId())
                        .eq(SeckillProduct::getIsDeleted, 0)
        );

        if (existing != null) {
            throw new BusinessException("SECKILL_EXISTS", "该SKU已存在秒杀活动");
        }

        // 生成秒杀商品ID
        Long seckillId = idGenerator.nextId();

        // 转换为实体
        SeckillProduct seckillProduct = convertToEntity(seckillProductDTO);
        seckillProduct.setId(seckillId);

        // 设置默认值
        if (seckillProduct.getLimitQuantity() == null) {
            seckillProduct.setLimitQuantity(1);
        }
        if (seckillProduct.getStatus() == null) {
            // 根据时间自动设置状态
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(seckillProduct.getStartTime())) {
                seckillProduct.setStatus(ProductConstants.SeckillStatus.NOT_STARTED);
            } else if (now.isAfter(seckillProduct.getEndTime())) {
                seckillProduct.setStatus(ProductConstants.SeckillStatus.ENDED);
            } else {
                seckillProduct.setStatus(ProductConstants.SeckillStatus.IN_PROGRESS);
            }
        }
        if (seckillProduct.getSortOrder() == null) {
            seckillProduct.setSortOrder(0);
        }

        // 保存到数据库
        seckillProductMapper.insert(seckillProduct);

        log.info("创建秒杀商品成功 - seckillId: {}, skuId: {}", seckillId, seckillProductDTO.getSkuId());
        return seckillId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSeckillProduct(SeckillProductDTO seckillProductDTO) {
        if (seckillProductDTO.getId() == null) {
            throw new ValidationException("SECKILL_ID_REQUIRED", "秒杀商品ID不能为空");
        }

        // 验证秒杀商品是否存在
        SeckillProduct existing = seckillProductMapper.selectById(seckillProductDTO.getId());
        if (existing == null) {
            throw new BusinessException("SECKILL_NOT_FOUND", "秒杀商品不存在");
        }

        // 参数验证
        validateSeckillProduct(seckillProductDTO);

        // 验证时间范围
        if (seckillProductDTO.getStartTime().isAfter(seckillProductDTO.getEndTime())) {
            throw new ValidationException("TIME_RANGE_INVALID", "开始时间不能晚于结束时间");
        }

        // 转换为实体并更新
        SeckillProduct seckillProduct = convertToEntity(seckillProductDTO);
        seckillProduct.setId(seckillProductDTO.getId());

        // 根据时间自动更新状态
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(seckillProduct.getStartTime())) {
            seckillProduct.setStatus(ProductConstants.SeckillStatus.NOT_STARTED);
        } else if (now.isAfter(seckillProduct.getEndTime())) {
            seckillProduct.setStatus(ProductConstants.SeckillStatus.ENDED);
        } else {
            seckillProduct.setStatus(ProductConstants.SeckillStatus.IN_PROGRESS);
        }

        int updated = seckillProductMapper.updateById(seckillProduct);
        log.info("更新秒杀商品成功 - seckillId: {}", seckillProductDTO.getId());
        return updated > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSeckillProduct(Long id) {
        if (id == null) {
            throw new ValidationException("SECKILL_ID_REQUIRED", "秒杀商品ID不能为空");
        }

        SeckillProduct seckillProduct = seckillProductMapper.selectById(id);
        if (seckillProduct == null) {
            throw new BusinessException("SECKILL_NOT_FOUND", "秒杀商品不存在");
        }

        // 逻辑删除
        int deleted = seckillProductMapper.deleteById(id);
        log.info("删除秒杀商品成功 - seckillId: {}", id);
        return deleted > 0;
    }

    @Override
    public SeckillProductDTO getSeckillProductById(Long id) {
        if (id == null) {
            throw new ValidationException("SECKILL_ID_REQUIRED", "秒杀商品ID不能为空");
        }

        SeckillProduct seckillProduct = seckillProductMapper.selectById(id);
        if (seckillProduct == null) {
            return null;
        }

        return convertToDTO(seckillProduct);
    }

    @Override
    public SeckillProductDTO getSeckillProductBySkuId(Long skuId) {
        if (skuId == null) {
            throw new ValidationException("SKU_ID_REQUIRED", "SKU ID不能为空");
        }

        SeckillProduct seckillProduct = seckillProductMapper.selectOne(
                new LambdaQueryWrapper<SeckillProduct>()
                        .eq(SeckillProduct::getSkuId, skuId)
                        .eq(SeckillProduct::getIsDeleted, 0)
        );

        if (seckillProduct == null) {
            return null;
        }

        return convertToDTO(seckillProduct);
    }

    @Override
    public Page<SeckillProductDTO> getSeckillProductsByPage(int pageNum, int pageSize, Integer status) {
        Page<SeckillProduct> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SeckillProduct> wrapper = new LambdaQueryWrapper<SeckillProduct>()
                .eq(SeckillProduct::getIsDeleted, 0)
                .orderByDesc(SeckillProduct::getCreateTime);

        if (status != null) {
            wrapper.eq(SeckillProduct::getStatus, status);
        }

        Page<SeckillProduct> result = seckillProductMapper.selectPage(page, wrapper);

        // 转换为DTO
        Page<SeckillProductDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<SeckillProductDTO> dtoList = result.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);

        return dtoPage;
    }

    @Override
    public List<SeckillProductDTO> getInProgressSeckillProducts() {
        LocalDateTime now = LocalDateTime.now();
        List<SeckillProduct> seckillProducts = seckillProductMapper.selectInProgressSeckills(now);
        return seckillProducts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductSeckillStock(Long id, Integer quantity) {
        if (id == null) {
            throw new ValidationException("SECKILL_ID_REQUIRED", "秒杀商品ID不能为空");
        }
        if (quantity == null || quantity <= 0) {
            throw new ValidationException("QUANTITY_INVALID", "扣减数量必须大于0");
        }

        int updated = seckillProductMapper.deductSeckillStock(id, quantity);
        if (updated > 0) {
            log.info("扣减秒杀库存成功 - seckillId: {}, quantity: {}", id, quantity);
            return true;
        } else {
            log.warn("扣减秒杀库存失败，库存不足 - seckillId: {}, quantity: {}", id, quantity);
            throw new BusinessException("SECKILL_STOCK_INSUFFICIENT", "秒杀库存不足");
        }
    }

    /**
     * 验证秒杀商品参数
     */
    private void validateSeckillProduct(SeckillProductDTO dto) {
        if (dto.getProductId() == null) {
            throw new ValidationException("PRODUCT_ID_REQUIRED", "商品ID不能为空");
        }
        if (dto.getSkuId() == null) {
            throw new ValidationException("SKU_ID_REQUIRED", "SKU ID不能为空");
        }
        if (dto.getSeckillPrice() == null || dto.getSeckillPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new ValidationException("SECKILL_PRICE_INVALID", "秒杀价格必须大于0");
        }
        if (dto.getSeckillStock() == null || dto.getSeckillStock() <= 0) {
            throw new ValidationException("SECKILL_STOCK_INVALID", "秒杀库存必须大于0");
        }
        if (dto.getStartTime() == null) {
            throw new ValidationException("START_TIME_REQUIRED", "开始时间不能为空");
        }
        if (dto.getEndTime() == null) {
            throw new ValidationException("END_TIME_REQUIRED", "结束时间不能为空");
        }
    }

    /**
     * 转换为实体
     */
    private SeckillProduct convertToEntity(SeckillProductDTO dto) {
        SeckillProduct entity = new SeckillProduct();
        BeanUtil.copyProperties(dto, entity);
        return entity;
    }

    /**
     * 转换为DTO
     */
    private SeckillProductDTO convertToDTO(SeckillProduct entity) {
        SeckillProductDTO dto = new SeckillProductDTO();
        BeanUtil.copyProperties(entity, dto);
        return dto;
    }

}

