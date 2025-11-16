package com.xpcjsu.sunshinemall.stock.service;

import com.xpcjsu.sunshinemall.stock.dto.StockDTO;

public interface StockService {
    boolean initStock(Long skuId, Integer quantity);
    boolean addStock(Long skuId, Integer quantity, String remark);
    boolean deductStock(Long skuId, Integer quantity, Long orderId, String remark);
    boolean lockStock(Long skuId, Integer quantity, Long orderId, String remark);
    boolean unlockStock(Long skuId, Integer quantity, Long orderId, String remark);
    boolean confirmDeduct(Long skuId, Integer quantity, Long orderId, String remark);
    boolean returnStock(Long skuId, Integer quantity, Long orderId, String remark);
    StockDTO getStockBySkuId(Long skuId);
    boolean checkStock(Long skuId, Integer quantity);
}

