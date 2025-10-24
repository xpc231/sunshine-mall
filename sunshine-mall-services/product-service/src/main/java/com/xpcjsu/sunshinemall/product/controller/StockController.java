package com.xpcjsu.sunshinemall.product.controller;

import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import com.xpcjsu.sunshinemall.product.dto.StockDTO;
import com.xpcjsu.sunshinemall.product.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 库存控制器
 *
 * @author xpcjsu
 */
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@Validated
public class StockController {

    private final StockService stockService;

    /**
     * 初始化库存
     */
    @PostMapping("/init")
    public Result<Void> initStock(@RequestParam Long skuId, 
                                  @RequestParam Integer quantity) {
        boolean success = stockService.initStock(skuId, quantity);
        return success ? Result.success(null, "初始化成功")
                       : Result.failure("INIT_FAILED", "初始化失败");
    }

    /**
     * 增加库存
     */
    @PostMapping("/add")
    public Result<Void> addStock(@RequestParam Long skuId,
                                 @RequestParam Integer quantity,
                                 @RequestParam(required = false) String remark) {
        boolean success = stockService.addStock(skuId, quantity, remark);
        return success ? Result.success(null, "增加成功")
                       : Result.failure("ADD_FAILED", "增加失败");
    }

    /**
     * 扣减库存
     */
    @PostMapping("/deduct")
    public Result<Void> deductStock(@RequestParam Long skuId,
                                   @RequestParam Integer quantity,
                                   @RequestParam(required = false) Long orderId,
                                   @RequestParam(required = false) String remark) {
        boolean success = stockService.deductStock(skuId, quantity, orderId, remark);
        return success ? Result.success(null, "扣减成功")
                       : Result.failure("DEDUCT_FAILED", "扣减失败");
    }

    /**
     * 预占库存
     */
    @PostMapping("/lock")
    public Result<Void> lockStock(@RequestParam Long skuId,
                                  @RequestParam Integer quantity,
                                  @RequestParam(required = false) Long orderId,
                                  @RequestParam(required = false) String remark) {
        boolean success = stockService.lockStock(skuId, quantity, orderId, remark);
        return success ? Result.success(null, "预占成功")
                       : Result.failure("LOCK_FAILED", "预占失败");
    }

    /**
     * 释放库存
     */
    @PostMapping("/unlock")
    public Result<Void> unlockStock(@RequestParam Long skuId,
                                    @RequestParam Integer quantity,
                                    @RequestParam(required = false) Long orderId,
                                    @RequestParam(required = false) String remark) {
        boolean success = stockService.unlockStock(skuId, quantity, orderId, remark);
        return success ? Result.success(null, "释放成功")
                       : Result.failure("UNLOCK_FAILED", "释放失败");
    }

    /**
     * 确认扣减
     */
    @PostMapping("/confirm-deduct")
    public Result<Void> confirmDeduct(@RequestParam Long skuId,
                                      @RequestParam Integer quantity,
                                      @RequestParam(required = false) Long orderId,
                                      @RequestParam(required = false) String remark) {
        boolean success = stockService.confirmDeduct(skuId, quantity, orderId, remark);
        return success ? Result.success(null, "确认成功")
                       : Result.failure("CONFIRM_FAILED", "确认失败");
    }

    /**
     * 退货，补库存
     */
    @PostMapping("/return")
    public Result<Void> returnStock(@RequestParam Long skuId,
                                    @RequestParam Integer quantity,
                                    @RequestParam(required = false) Long orderId,
                                    @RequestParam(required = false) String remark) {
        boolean success = stockService.returnStock(skuId, quantity, orderId, remark);
        return success ? Result.success(null, "退货成功")
                       : Result.failure("RETURN_FAILED", "退货失败");
    }

    /**
     * 获取库存信息
     */
    @GetMapping("/{skuId}")
    public Result<StockDTO> getStockBySkuId(@PathVariable Long skuId) {
        StockDTO stock = stockService.getStockBySkuId(skuId);
        return Result.success(stock);
    }

    /**
     * 检查库存是否充足
     */
    @GetMapping("/check")
    public Result<Boolean> checkStock(@RequestParam Long skuId,
                                      @RequestParam Integer quantity) {
        boolean sufficient = stockService.checkStock(skuId, quantity);
        return Result.success(sufficient);
    }

}
