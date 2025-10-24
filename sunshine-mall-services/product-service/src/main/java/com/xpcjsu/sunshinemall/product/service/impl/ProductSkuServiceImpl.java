package com.xpcjsu.sunshinemall.product.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xpcjsu.sunshinemall.framework.base.exception.BusinessException;
import com.xpcjsu.sunshinemall.framework.base.exception.ValidationException;
import com.xpcjsu.sunshinemall.framework.cache.core.CacheManager;
import com.xpcjsu.sunshinemall.framework.distributedid.core.SnowflakeIdGenerator;
import com.xpcjsu.sunshinemall.product.constant.ProductConstants;
import com.xpcjsu.sunshinemall.product.dto.ProductSkuDTO;
import com.xpcjsu.sunshinemall.product.entity.Product;
import com.xpcjsu.sunshinemall.product.entity.ProductSku;
import com.xpcjsu.sunshinemall.product.mapper.ProductMapper;
import com.xpcjsu.sunshinemall.product.mapper.ProductSkuMapper;
import com.xpcjsu.sunshinemall.product.service.ProductSkuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品SKU服务实现
 *
 * @author xpcjsu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSkuServiceImpl implements ProductSkuService {

    private final ProductSkuMapper productSkuMapper;
    private final ProductMapper productMapper;
    private final CacheManager cacheManager;
    private final SnowflakeIdGenerator idGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSku(ProductSkuDTO skuDTO) {
        // 参数验证
        validateSku(skuDTO);

        // 验证商品是否存在
        Product product = productMapper.selectById(skuDTO.getProductId());
        if (product == null) {
            throw new BusinessException("PRODUCT_NOT_FOUND", "商品不存在");
        }

        // 生成SKU ID和SKU编码
        Long skuId = idGenerator.nextId();
        String skuCode = generateSkuCode(skuDTO.getProductId());

        // 转换为实体
        ProductSku sku = convertToEntity(skuDTO);
        sku.setId(skuId);
        sku.setSkuCode(skuCode);

        // 设置默认值
        if (sku.getStatus() == null) {
            sku.setStatus(ProductConstants.SkuStatus.ENABLED);
        }

        // 保存到数据库
        productSkuMapper.insert(sku);

        // 清除SKU缓存
        clearSkuCache(skuId);

        log.info("创建SKU成功 - skuId: {}, skuCode: {}", skuId, skuCode);
        return skuId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> batchCreateSku(List<ProductSkuDTO> skuDTOList) {
        if (CollUtil.isEmpty(skuDTOList)) {
            throw new ValidationException("SKU_LIST_EMPTY", "SKU列表不能为空");
        }

        List<Long> skuIds = new ArrayList<>();
        for (ProductSkuDTO skuDTO : skuDTOList) {
            Long skuId = createSku(skuDTO);
            skuIds.add(skuId);
        }

        return skuIds;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSku(ProductSkuDTO skuDTO) {
        if (skuDTO.getId() == null) {
            throw new ValidationException("SKU_ID_REQUIRED", "SKU ID不能为空");
        }

        // 验证SKU是否存在
        ProductSku existingSku = productSkuMapper.selectById(skuDTO.getId());
        if (existingSku == null) {
            throw new BusinessException("SKU_NOT_FOUND", "SKU不存在");
        }

        // 更新SKU
        ProductSku sku = convertToEntity(skuDTO);
        int updated = productSkuMapper.updateById(sku);

        if (updated > 0) {
            // 清除SKU缓存
            clearSkuCache(skuDTO.getId());
            log.info("更新SKU成功 - skuId: {}", skuDTO.getId());
        }

        return updated > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSku(Long skuId) {
        if (skuId == null) {
            throw new ValidationException("SKU_ID_REQUIRED", "SKU ID不能为空");
        }

        // TODO: 检查是否有关联订单（需要在订单模块完成后实现）
        // TODO: 删除关联的库存记录

        // 删除SKU（逻辑删除）
        int deleted = productSkuMapper.deleteById(skuId);

        if (deleted > 0) {
            // 清除SKU缓存
            clearSkuCache(skuId);
            log.info("删除SKU成功 - skuId: {}", skuId);
        }

        return deleted > 0;
    }

    @Override
    public ProductSkuDTO getSkuById(Long skuId) {
        if (skuId == null) {
            throw new ValidationException("SKU_ID_REQUIRED", "SKU ID不能为空");
        }

        // 先从缓存获取
        String cacheKey = getSkuCacheKey(skuId);
        String cacheValue = cacheManager.get(cacheKey, String.class);
        if (StringUtils.hasText(cacheValue)) {
            return JSONUtil.toBean(cacheValue, ProductSkuDTO.class);
        }

        // 从数据库查询
        ProductSku sku = productSkuMapper.selectById(skuId);
        if (sku == null) {
            throw new BusinessException("SKU_NOT_FOUND", "SKU不存在");
        }

        ProductSkuDTO skuDTO = convertToDTO(sku);

        // 缓存SKU详情
        cacheManager.set(
                cacheKey,
                JSONUtil.toJsonStr(skuDTO),
                ProductConstants.Cache.PRODUCT_CACHE_EXPIRE
        );

        return skuDTO;
    }

    @Override
    public List<ProductSkuDTO> getSkuListByProductId(Long productId) {
        if (productId == null) {
            throw new ValidationException("PRODUCT_ID_REQUIRED", "商品ID不能为空");
        }

        List<ProductSku> skuList = productSkuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>()
                        .eq(ProductSku::getProductId, productId)
                        .orderByAsc(ProductSku::getId)
        );

        return skuList.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ProductSkuDTO getSkuByCode(String skuCode) {
        if (!StringUtils.hasText(skuCode)) {
            throw new ValidationException("SKU_CODE_REQUIRED", "SKU编码不能为空");
        }

        ProductSku sku = productSkuMapper.selectOne(
                new LambdaQueryWrapper<ProductSku>()
                        .eq(ProductSku::getSkuCode, skuCode)
        );

        if (sku == null) {
            throw new BusinessException("SKU_NOT_FOUND", "SKU不存在");
        }

        return convertToDTO(sku);
    }

    /**
     * 验证SKU信息
     */
    private void validateSku(ProductSkuDTO skuDTO) {
        if (skuDTO.getProductId() == null) {
            throw new ValidationException("PRODUCT_ID_REQUIRED", "商品ID不能为空");
        }

        if (!StringUtils.hasText(skuDTO.getSkuName())) {
            throw new ValidationException("SKU_NAME_REQUIRED", "SKU名称不能为空");
        }

        if (skuDTO.getPrice() == null || skuDTO.getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new ValidationException("PRICE_INVALID", "销售价格必须大于0");
        }
    }

    /**
     * 生成SKU编码
     */
    private String generateSkuCode(Long productId) {
        // 使用分布式ID生成器生成唯一编码
        return "SKU" + productId + "_" + idGenerator.nextId();
    }

    /**
     * 转换为实体
     */
    private ProductSku convertToEntity(ProductSkuDTO skuDTO) {
        ProductSku sku = BeanUtil.copyProperties(skuDTO, ProductSku.class);

        // 转换规格属性为JSON字符串
        if (CollUtil.isNotEmpty(skuDTO.getSpecMap())) {
            sku.setSpecJson(JSONUtil.toJsonStr(skuDTO.getSpecMap()));
        }

        return sku;
    }

    /**
     * 转换为DTO
     */
    private ProductSkuDTO convertToDTO(ProductSku sku) {
        ProductSkuDTO skuDTO = BeanUtil.copyProperties(sku, ProductSkuDTO.class);

        // 转换规格JSON字符串为Map
        if (StrUtil.isNotBlank(sku.getSpecJson())) {
            Map<String, String> specMap = JSONUtil.toBean(sku.getSpecJson(), Map.class);
            skuDTO.setSpecMap(specMap);
        }

        return skuDTO;
    }

    /**
     * 获取SKU缓存键
     */
    private String getSkuCacheKey(Long skuId) {
        return ProductConstants.Cache.SKU_DETAIL_PREFIX + skuId;
    }

    /**
     * 清除SKU缓存
     */
    private void clearSkuCache(Long skuId) {
        cacheManager.delete(getSkuCacheKey(skuId));
        log.debug("清除SKU缓存 - skuId: {}", skuId);
    }

}
