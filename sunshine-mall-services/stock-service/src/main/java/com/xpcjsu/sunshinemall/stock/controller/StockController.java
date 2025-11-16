package com.xpcjsu.sunshinemall.stock.controller;

import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import com.xpcjsu.sunshinemall.stock.dto.StockDTO;
import com.xpcjsu.sunshinemall.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@Validated
public class StockController {

    private final StockService stockService;

    @PostMapping("/init")
    public Result<Void> initStock(@RequestParam Long skuId, @RequestParam Integer quantity) {
        boolean success = stockService.initStock(skuId, quantity);
        return success ? Result.success(null, "初始化成功") : Result.failure("INIT_FAILED", "初始化失败");
    }

    @PostMapping("/add")
    public Result<Void> addStock(@RequestParam Long skuId, @RequestParam Integer quantity, @RequestParam(required = false) String remark) {
        boolean success = stockService.addStock(skuId, quantity, remark);
        return success ? Result.success(null, "增加成功") : Result.failure("ADD_FAILED", "增加失败");
    }

    @PostMapping("/deduct")
    public Result<Void> deductStock(@RequestParam Long skuId, @RequestParam Integer quantity,
                                    @RequestParam(required = false) Long orderId, @RequestParam(required = false) String remark) {
        boolean success = stockService.deductStock(skuId, quantity, orderId, remark);
        return success ? Result.success(null, "扣减成功") : Result.failure("DEDUCT_FAILED", "扣减失败");
    }

    @PostMapping("/lock")
    public Result<Void> lockStock(@RequestParam Long skuId, @RequestParam Integer quantity,
                                  @RequestParam(required = false) Long orderId, @RequestParam(required = false) String remark) {
        boolean success = stockService.lockStock(skuId, quantity, orderId, remark);
        return success ? Result.success(null, "预占成功") : Result.failure("LOCK_FAILED", "预占失败");
    }

    @PostMapping("/unlock")
    public Result<Void> unlockStock(@RequestParam Long skuId, @RequestParam Integer quantity,
                                    @RequestParam(required = false) Long orderId, @RequestParam(required = false) String remark) {
        boolean success = stockService.unlockStock(skuId, quantity, orderId, remark);
        return success ? Result.success(null, "释放成功") : Result.failure("UNLOCK_FAILED", "释放失败");
    }

    @PostMapping("/confirm-deduct")
    public Result<Void> confirmDeduct(@RequestParam Long skuId, @RequestParam Integer quantity,
                                      @RequestParam(required = false) Long orderId, @RequestParam(required = false) String remark) {
        boolean success = stockService.confirmDeduct(skuId, quantity, orderId, remark);
        return success ? Result.success(null, "确认成功") : Result.failure("CONFIRM_FAILED", "确认失败");
    }

    @PostMapping("/return")
    public Result<Void> returnStock(@RequestParam Long skuId, @RequestParam Integer quantity,
                                    @RequestParam(required = false) Long orderId, @RequestParam(required = false) String remark) {
        boolean success = stockService.returnStock(skuId, quantity, orderId, remark);
        return success ? Result.success(null, "退货成功") : Result.failure("RETURN_FAILED", "退货失败");
    }

    @GetMapping("/{skuId}")
    public Result<StockDTO> getStockBySkuId(@PathVariable Long skuId) {
        return Result.success(stockService.getStockBySkuId(skuId));
    }

    @GetMapping("/check")
    public Result<Boolean> checkStock(@RequestParam Long skuId, @RequestParam Integer quantity) {
        return Result.success(stockService.checkStock(skuId, quantity));
    }
}

