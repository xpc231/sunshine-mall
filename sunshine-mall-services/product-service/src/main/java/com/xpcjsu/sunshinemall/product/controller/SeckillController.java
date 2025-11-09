package com.xpcjsu.sunshinemall.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import com.xpcjsu.sunshinemall.product.dto.SeckillProductDTO;
import com.xpcjsu.sunshinemall.product.service.SeckillProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 秒杀商品控制器
 *
 * @author xpcjsu
 */
@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
@Validated
public class SeckillController {

    private final SeckillProductService seckillProductService;

    /**
     * 创建秒杀商品
     */
    @PostMapping("/product")
    public Result<Long> createSeckillProduct(@Valid @RequestBody SeckillProductDTO seckillProductDTO) {
        Long seckillId = seckillProductService.createSeckillProduct(seckillProductDTO);
        return Result.success(seckillId, "创建成功");
    }

    /**
     * 更新秒杀商品
     */
    @PutMapping("/product/{id}")
    public Result<Void> updateSeckillProduct(@PathVariable Long id,
                                            @Valid @RequestBody SeckillProductDTO seckillProductDTO) {
        seckillProductDTO.setId(id);
        boolean success = seckillProductService.updateSeckillProduct(seckillProductDTO);
        return success ? Result.success(null, "更新成功")
                       : Result.failure("UPDATE_FAILED", "更新失败");
    }

    /**
     * 删除秒杀商品
     */
    @DeleteMapping("/product/{id}")
    public Result<Void> deleteSeckillProduct(@PathVariable Long id) {
        boolean success = seckillProductService.deleteSeckillProduct(id);
        return success ? Result.success(null, "删除成功")
                       : Result.failure("DELETE_FAILED", "删除失败");
    }

    /**
     * 根据ID获取秒杀商品详情
     */
    @GetMapping("/product/{id}")
    public Result<SeckillProductDTO> getSeckillProductById(@PathVariable Long id) {
        SeckillProductDTO seckillProduct = seckillProductService.getSeckillProductById(id);
        if (seckillProduct == null) {
            return Result.failure("SECKILL_NOT_FOUND", "秒杀商品不存在");
        }
        return Result.success(seckillProduct);
    }

    /**
     * 根据SKU ID获取秒杀商品
     */
    @GetMapping("/product/sku/{skuId}")
    public Result<SeckillProductDTO> getSeckillProductBySkuId(@PathVariable Long skuId) {
        SeckillProductDTO seckillProduct = seckillProductService.getSeckillProductBySkuId(skuId);
        if (seckillProduct == null) {
            return Result.failure("SECKILL_NOT_FOUND", "秒杀商品不存在");
        }
        return Result.success(seckillProduct);
    }

    /**
     * 分页查询秒杀商品列表
     */
    @GetMapping("/product/page")
    public Result<Page<SeckillProductDTO>> getSeckillProductsByPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status) {
        Page<SeckillProductDTO> page = seckillProductService.getSeckillProductsByPage(pageNum, pageSize, status);
        return Result.success(page);
    }

    /**
     * 查询进行中的秒杀商品列表
     */
    @GetMapping("/product/in-progress")
    public Result<List<SeckillProductDTO>> getInProgressSeckillProducts() {
        List<SeckillProductDTO> list = seckillProductService.getInProgressSeckillProducts();
        return Result.success(list);
    }

    /**
     * 扣减秒杀库存
     */
    @PostMapping("/product/{id}/deduct-stock")
    public Result<Void> deductSeckillStock(@PathVariable Long id,
                                          @RequestParam Integer quantity) {
        boolean success = seckillProductService.deductSeckillStock(id, quantity);
        return success ? Result.success(null, "扣减成功")
                       : Result.failure("DEDUCT_FAILED", "扣减失败");
    }

    /**
     * 预热秒杀库存到Redis
     */
    @PostMapping("/product/{id}/warmup-stock")
    public Result<Void> warmupSeckillStock(@PathVariable Long id) {
        boolean success = seckillProductService.warmupSeckillStock(id);
        return success ? Result.success(null, "预热成功")
                       : Result.failure("WARMUP_FAILED", "预热失败");
    }

    /**
     * 回滚秒杀库存
     * <p>
     * 说明：
     * 1. 订单创建失败时：自动调用（在OrderService中）
     * 2. 订单取消时：自动调用（在OrderService中）
     * 3. 此接口主要用于：管理员手动补偿、异常情况修复等场景
     */
    @PostMapping("/product/{id}/rollback-stock")
    public Result<Void> rollbackSeckillStock(@PathVariable Long id,
                                             @RequestParam Integer quantity) {
        boolean success = seckillProductService.rollbackSeckillStock(id, quantity);
        return success ? Result.success(null, "回滚成功")
                       : Result.failure("ROLLBACK_FAILED", "回滚失败");
    }

}

