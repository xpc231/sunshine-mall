package com.xpcjsu.sunshinemall.product.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xpcjsu.sunshinemall.framework.base.exception.BusinessException;
import com.xpcjsu.sunshinemall.framework.base.exception.ValidationException;
import com.xpcjsu.sunshinemall.framework.cache.core.CacheManager;
import com.xpcjsu.sunshinemall.framework.distributedid.core.SnowflakeIdGenerator;
import com.xpcjsu.sunshinemall.product.constant.ProductConstants;
import com.xpcjsu.sunshinemall.product.dto.ProductDTO;
import com.xpcjsu.sunshinemall.product.entity.Category;
import com.xpcjsu.sunshinemall.product.entity.Product;
import com.xpcjsu.sunshinemall.product.mapper.CategoryMapper;
import com.xpcjsu.sunshinemall.product.mapper.ProductMapper;
import com.xpcjsu.sunshinemall.product.service.ProductService;
import com.xpcjsu.sunshinemall.product.util.ProductParamValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品服务实现
 *
 * @author xpcjsu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final CacheManager cacheManager;
    private final SnowflakeIdGenerator idGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProduct(ProductDTO productDTO) {
        // 参数验证
        validateProduct(productDTO);

        // 验证分类是否存在且启用
        Category category = categoryMapper.selectById(productDTO.getCategoryId());
        if (category == null) {
            throw new BusinessException("CATEGORY_NOT_FOUND", "商品分类不存在");
        }
        if (!ProductConstants.Category.STATUS_ENABLED.equals(category.getStatus())) {
            throw new BusinessException("CATEGORY_DISABLED", "商品分类已禁用");
        }

        // 生成商品ID和商品编码
        Long productId = idGenerator.nextId();
        String productCode = generateProductCode();

        // 转换为实体
        Product product = convertToEntity(productDTO);
        product.setId(productId);
        product.setProductCode(productCode);

        // 设置默认值
        if (product.getStatus() == null) {
            product.setStatus(ProductConstants.ProductStatus.OFF_SHELF);
        }
        if (product.getSaleCount() == null) {
            product.setSaleCount(0);
        }
        if (product.getViewCount() == null) {
            product.setViewCount(0);
        }

        // 保存到数据库
        productMapper.insert(product);

        //TODO 添加到布隆过滤器

        log.info("创建商品成功 - productId: {}, productCode: {}", productId, productCode);
        return productId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateProduct(ProductDTO productDTO) {
        if (productDTO.getId() == null) {
            throw new ValidationException("PRODUCT_ID_REQUIRED", "商品ID不能为空");
        }

        // 验证商品是否存在
        Product existingProduct = productMapper.selectById(productDTO.getId());
        if (existingProduct == null) {
            throw new BusinessException("PRODUCT_NOT_FOUND", "商品不存在");
        }

        // 如果修改了分类，验证分类是否存在且启用
        if (productDTO.getCategoryId() != null && 
            !productDTO.getCategoryId().equals(existingProduct.getCategoryId())) {
            Category category = categoryMapper.selectById(productDTO.getCategoryId());
            if (category == null) {
                throw new BusinessException("CATEGORY_NOT_FOUND", "商品分类不存在");
            }
            if (!ProductConstants.Category.STATUS_ENABLED.equals(category.getStatus())) {
                throw new BusinessException("CATEGORY_DISABLED", "商品分类已禁用");
            }
        }

        // 更新商品
        Product product = convertToEntity(productDTO);
        int updated = productMapper.updateById(product);

        if (updated > 0) {
            log.info("更新商品成功 - productId: {}", productDTO.getId());
        }

        return updated > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteProduct(Long productId) {
        if (productId == null) {
            throw new ValidationException("PRODUCT_ID_REQUIRED", "商品ID不能为空");
        }

        // TODO: 检查是否有关联订单（需要在订单模块完成后实现）

        // 删除商品（逻辑删除）
        int deleted = productMapper.deleteById(productId);

        if (deleted > 0) {
            log.info("删除商品成功 - productId: {}", productId);
        }

        return deleted > 0;
    }

    @Override
    public ProductDTO getProductById(Long productId) {
        // 第一道防线: 参数校验
        ProductParamValidator.validateProductId(productId);

/*        // 第二道防线: 布隆过滤器拦截
        if (!productBloomFilter.productMightExist(productId)) {
            log.warn("布隆过滤器拦截 - 商品ID不存在: {}", productId);
            throw new BusinessException("PRODUCT_NOT_FOUND", "商品不存在");
        }*/

        // 第三道防线: 缓存查询(包含空值缓存)
        String cacheKey = getProductCacheKey(productId);
        String cacheValue = cacheManager.get(cacheKey, String.class);
        if (StringUtils.hasText(cacheValue)) {
            // 检查是否为空值缓存
            if ("NULL".equals(cacheValue)) {
                log.debug("命中空值缓存 - productId: {}", productId);
                throw new BusinessException("PRODUCT_NOT_FOUND", "商品不存在");
            }
            return JSONUtil.toBean(cacheValue, ProductDTO.class);
        }

        // 第四道防线: 数据库查询
        Product product = productMapper.selectById(productId);
        if (product == null) {
            // 设置空值缓存(60秒过期)
            cacheManager.set(cacheKey, "NULL", 60L);
            log.info("设置空值缓存 - productId: {}", productId);
            throw new BusinessException("PRODUCT_NOT_FOUND", "商品不存在");
        }

        ProductDTO productDTO = convertToDTO(product);

        // 缓存商品详情
        cacheManager.set(
                cacheKey,
                JSONUtil.toJsonStr(productDTO),
                ProductConstants.Cache.PRODUCT_CACHE_EXPIRE
        );

        // 异步增加浏览次数
        increaseViewCount(productId);

        return productDTO;
    }

    @Override
    public Page<ProductDTO> getProductsByPage(int pageNum, int pageSize, 
                                              Long categoryId, Integer status) {
        // 验证分页参数
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1 || pageSize > 100) {
            pageSize = 10;
        }

        // 构建查询条件
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }
        wrapper.orderByDesc(Product::getCreateTime);

        // 分页查询
        Page<Product> page = new Page<>(pageNum, pageSize);
        Page<Product> productPage = productMapper.selectPage(page, wrapper);

        // 转换为DTO分页结果
        Page<ProductDTO> dtoPage = new Page<>(pageNum, pageSize, productPage.getTotal());
        List<ProductDTO> dtoList = productPage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);

        return dtoPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean onShelf(Long productId) {
        if (productId == null) {
            throw new ValidationException("PRODUCT_ID_REQUIRED", "商品ID不能为空");
        }

        // 验证商品是否存在
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("PRODUCT_NOT_FOUND", "商品不存在");
        }

        // 验证商品是否已上架
        if (ProductConstants.ProductStatus.ON_SHELF.equals(product.getStatus())) {
            throw new BusinessException("PRODUCT_ALREADY_ON_SHELF", "商品已上架");
        }

        // TODO: 验证商品是否有可用库存（需要在库存模块完成后实现）

        // 上架商品
        int updated = productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .set(Product::getStatus, ProductConstants.ProductStatus.ON_SHELF)
                .eq(Product::getId, productId));

        if (updated > 0) {
            log.info("商品上架成功 - productId: {}", productId);
        }

        return updated > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean offShelf(Long productId) {
        if (productId == null) {
            throw new ValidationException("PRODUCT_ID_REQUIRED", "商品ID不能为空");
        }

        // 验证商品是否存在
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("PRODUCT_NOT_FOUND", "商品不存在");
        }

        // 验证商品是否已下架
        if (ProductConstants.ProductStatus.OFF_SHELF.equals(product.getStatus())) {
            throw new BusinessException("PRODUCT_ALREADY_OFF_SHELF", "商品已下架");
        }

        // 下架商品
        int updated = productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .set(Product::getStatus, ProductConstants.ProductStatus.OFF_SHELF)
                .eq(Product::getId, productId));

        if (updated > 0) {
            log.info("商品下架成功 - productId: {}", productId);
        }

        return updated > 0;
    }

    @Override
    public void increaseViewCount(Long productId) {
        if (productId == null) {
            return;
        }

        try {
            // 增加浏览次数（直接操作数据库）
            productMapper.update(null, new LambdaUpdateWrapper<Product>()
                    .setSql("view_count = view_count + 1")
                    .eq(Product::getId, productId));
        } catch (Exception e) {
            // 浏览次数更新失败不影响主流程
            log.error("增加浏览次数失败 - productId: {}", productId, e);
        }
    }

    /**
     * 验证商品信息
     */
    private void validateProduct(ProductDTO productDTO) {
        if (productDTO.getCategoryId() == null) {
            throw new ValidationException("CATEGORY_ID_REQUIRED", "分类ID不能为空");
        }

        if (!StringUtils.hasText(productDTO.getProductName())) {
            throw new ValidationException("PRODUCT_NAME_REQUIRED", "商品名称不能为空");
        }
    }

    /**
     * 生成商品编码
     */
    private String generateProductCode() {
        // 使用分布式ID生成器生成唯一编码
        return "P" + idGenerator.nextId();
    }

    /**
     * 转换为实体
     */
    private Product convertToEntity(ProductDTO productDTO) {
        Product product = BeanUtil.copyProperties(productDTO, Product.class);

        // 转换副图列表为JSON字符串
        if (CollUtil.isNotEmpty(productDTO.getSubImages())) {
            product.setSubImages(JSONUtil.toJsonStr(productDTO.getSubImages()));
        }

        return product;
    }

    /**
     * 转换为DTO
     */
    private ProductDTO convertToDTO(Product product) {
        ProductDTO productDTO = BeanUtil.copyProperties(product, ProductDTO.class);

        // 转换副图JSON字符串为列表
        if (StrUtil.isNotBlank(product.getSubImages())) {
            productDTO.setSubImages(JSONUtil.toList(product.getSubImages(), String.class));
        }

        return productDTO;
    }

    /**
     * 获取商品缓存键
     */
    private String getProductCacheKey(Long productId) {
        return ProductConstants.Cache.PRODUCT_DETAIL_PREFIX + productId;
    }

    /**
     * 清除商品缓存
     */
    private void clearProductCache(Long productId) {
        cacheManager.delete(getProductCacheKey(productId));
        log.debug("清除商品缓存 - productId: {}", productId);
    }

}
