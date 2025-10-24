package com.xpcjsu.sunshinemall.product.controller;

import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import com.xpcjsu.sunshinemall.product.dto.ProductSkuDTO;
import com.xpcjsu.sunshinemall.product.service.ProductSkuService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 商品SKU控制器
 *
 * @author xpcjsu
 */
@RestController
@RequestMapping("/api/sku")
@RequiredArgsConstructor
@Validated
public class ProductSkuController {

    private final ProductSkuService productSkuService;

    /**
     * 创建SKU
     */
    @PostMapping
    public Result<Long> createSku(@Valid @RequestBody ProductSkuDTO skuDTO) {
        Long skuId = productSkuService.createSku(skuDTO);
        return Result.success(skuId, "创建成功");
    }

    /**
     * 批量创建SKU
     */
    @PostMapping("/batch")
    public Result<List<Long>> batchCreateSku(@Valid @RequestBody List<ProductSkuDTO> skuDTOList) {
        List<Long> skuIds = productSkuService.batchCreateSku(skuDTOList);
        return Result.success(skuIds, "批量创建成功");
    }

    /**
     * 更新SKU
     */
    @PutMapping("/{id}")
    public Result<Void> updateSku(@PathVariable Long id,
                                  @Valid @RequestBody ProductSkuDTO skuDTO) {
        skuDTO.setId(id);
        boolean success = productSkuService.updateSku(skuDTO);
        return success ? Result.success(null, "更新成功")
                       : Result.failure("UPDATE_FAILED", "更新失败");
    }

    /**
     * 删除SKU
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteSku(@PathVariable Long id) {
        boolean success = productSkuService.deleteSku(id);
        return success ? Result.success(null, "删除成功")
                       : Result.failure("DELETE_FAILED", "删除失败");
    }

    /**
     * 根据ID获取SKU
     */
    @GetMapping("/{id}")
    public Result<ProductSkuDTO> getSkuById(@PathVariable Long id) {
        ProductSkuDTO sku = productSkuService.getSkuById(id);
        return Result.success(sku);
    }

    /**
     * 根据商品ID获取SKU列表
     */
    @GetMapping("/product/{productId}")
    public Result<List<ProductSkuDTO>> getSkuListByProductId(@PathVariable Long productId) {
        List<ProductSkuDTO> skuList = productSkuService.getSkuListByProductId(productId);
        return Result.success(skuList);
    }

    /**
     * 根据SKU编码获取SKU
     */
    @GetMapping("/code/{skuCode}")
    public Result<ProductSkuDTO> getSkuByCode(@PathVariable String skuCode) {
        ProductSkuDTO sku = productSkuService.getSkuByCode(skuCode);
        return Result.success(sku);
    }

}
