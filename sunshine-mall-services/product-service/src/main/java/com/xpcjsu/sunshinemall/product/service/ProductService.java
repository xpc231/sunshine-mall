package com.xpcjsu.sunshinemall.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xpcjsu.sunshinemall.product.dto.ProductDTO;

/**
 * 商品服务接口
 *
 * @author xpcjsu
 */
public interface ProductService {

    /**
     * 创建商品
     *
     * @param productDTO 商品信息
     * @return 商品ID
     */
    Long createProduct(ProductDTO productDTO);

    /**
     * 更新商品
     *
     * @param productDTO 商品信息
     * @return 是否成功
     */
    boolean updateProduct(ProductDTO productDTO);

    /**
     * 删除商品
     *
     * @param productId 商品ID
     * @return 是否成功
     */
    boolean deleteProduct(Long productId);

    /**
     * 根据ID获取商品详情
     *
     * @param productId 商品ID
     * @return 商品详情
     */
    ProductDTO getProductById(Long productId);

    /**
     * 分页查询商品列表
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param categoryId 分类ID（可选）
     * @param status 商品状态（可选）
     * @return 商品分页列表
     */
    Page<ProductDTO> getProductsByPage(int pageNum, int pageSize, Long categoryId, Integer status);

    /**
     * 上架商品
     *
     * @param productId 商品ID
     * @return 是否成功
     */
    boolean onShelf(Long productId);

    /**
     * 下架商品
     *
     * @param productId 商品ID
     * @return 是否成功
     */
    boolean offShelf(Long productId);

    /**
     * 增加浏览次数
     *
     * @param productId 商品ID
    /**
     * 增加浏览次数
     *
     * @param productId 商品ID
     */
    void increaseViewCount(Long productId);

}
