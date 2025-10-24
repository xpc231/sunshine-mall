package com.xpcjsu.sunshinemall.product.service;

import com.xpcjsu.sunshinemall.product.dto.ProductSkuDTO;

import java.util.List;

/**
 * 商品SKU服务接口
 *
 * @author xpcjsu
 */
public interface ProductSkuService {

    /**
     * 创建SKU
     *
     * @param skuDTO SKU信息
     * @return SKU ID
     */
    Long createSku(ProductSkuDTO skuDTO);

    /**
     * 批量创建SKU
     *
     * @param skuDTOList SKU信息列表
     * @return SKU ID列表
     */
    List<Long> batchCreateSku(List<ProductSkuDTO> skuDTOList);

    /**
     * 更新SKU
     *
     * @param skuDTO SKU信息
     * @return 是否成功
     */
    boolean updateSku(ProductSkuDTO skuDTO);

    /**
     * 删除SKU
     *
     * @param skuId SKU ID
     * @return 是否成功
     */
    boolean deleteSku(Long skuId);

    /**
     * 根据ID获取SKU
     *
     * @param skuId SKU ID
     * @return SKU信息
     */
    ProductSkuDTO getSkuById(Long skuId);

    /**
     * 根据商品ID获取SKU列表
     *
     * @param productId 商品ID
     * @return SKU列表
     */
    List<ProductSkuDTO> getSkuListByProductId(Long productId);

    /**
     * 根据SKU编码获取SKU
     *
     * @param skuCode SKU编码
     * @return SKU信息
     */
    ProductSkuDTO getSkuByCode(String skuCode);

}
