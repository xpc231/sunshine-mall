package com.xpcjsu.sunshinemall.product.service;

import com.xpcjsu.sunshinemall.product.dto.StockDTO;

/**
 * 库存服务接口
 *
 * @author xpcjsu
 */
public interface StockService {

    /**
     * 初始化库存
     *
     * @param skuId SKU ID
     * @param quantity 初始库存数量
     * @return 是否成功
     */
    boolean initStock(Long skuId, Integer quantity);

    /**
     * 增加库存
     *
     * @param skuId SKU ID
     * @param quantity 增加数量
     * @param remark 备注
     * @return 是否成功
     */
    boolean addStock(Long skuId, Integer quantity, String remark);

    /**
     * 扣减库存（直接扣减）
     *
     * @param skuId SKU ID
     * @param quantity 扣减数量
     * @param orderId 订单ID
     * @param remark 备注
     * @return 是否成功
     */
    boolean deductStock(Long skuId, Integer quantity, Long orderId, String remark);

    /**
     * 预占库存（锁定库存）
     *
     * @param skuId SKU ID
     * @param quantity 预占数量
     * @param orderId 订单ID
     * @param remark 备注
     * @return 是否成功
     */
    boolean lockStock(Long skuId, Integer quantity, Long orderId, String remark);

    /**
     * 释放库存（解锁库存）
     *
     * @param skuId SKU ID
     * @param quantity 释放数量
     * @param orderId 订单ID
     * @param remark 备注
     * @return 是否成功
     */
    boolean unlockStock(Long skuId, Integer quantity, Long orderId, String remark);

    /**
     * 确认扣减（将锁定库存转为扣减）
     *
     * @param skuId SKU ID
     * @param quantity 扣减数量
     * @param orderId 订单ID
     * @param remark 备注
     * @return 是否成功
     */
    boolean confirmDeduct(Long skuId, Integer quantity, Long orderId, String remark);

    /**
     * 退货补库存
     *
     * @param skuId SKU ID
     * @param quantity 退货数量
     * @param orderId 订单ID
     * @param remark 备注
     * @return 是否成功
     */
    boolean returnStock(Long skuId, Integer quantity, Long orderId, String remark);

    /**
     * 根据SKU ID获取库存信息
     *
     * @param skuId SKU ID
     * @return 库存信息
     */
    StockDTO getStockBySkuId(Long skuId);

    /**
     * 检查库存是否充足
     *
     * @param skuId SKU ID
     * @param quantity 需要的数量
     * @return 是否充足
     */
    boolean checkStock(Long skuId, Integer quantity);

}
