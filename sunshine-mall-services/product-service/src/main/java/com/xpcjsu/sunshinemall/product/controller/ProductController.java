package com.xpcjsu.sunshinemall.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import com.xpcjsu.sunshinemall.product.dto.ProductDTO;
import com.xpcjsu.sunshinemall.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 商品控制器
 *
 * @author xpcjsu
 */
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final ProductService productService;

    /**
     * 创建商品
     */
    @PostMapping
    public Result<Long> createProduct(@Valid @RequestBody ProductDTO productDTO) {
        Long productId = productService.createProduct(productDTO);
        return Result.success(productId, "创建成功");
    }

    /**
     * 更新商品
     */
    @PutMapping("/{id}")
    public Result<Void> updateProduct(@PathVariable Long id,
                                      @Valid @RequestBody ProductDTO productDTO) {
        productDTO.setId(id);
        boolean success = productService.updateProduct(productDTO);
        return success ? Result.success(null, "更新成功")
                       : Result.failure("UPDATE_FAILED", "更新失败");
    }

    /**
     * 删除商品
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        boolean success = productService.deleteProduct(id);
        return success ? Result.success(null, "删除成功")
                       : Result.failure("DELETE_FAILED", "删除失败");
    }

    /**
     * 根据ID获取商品详情
     */
    @GetMapping("/{id}")
    public Result<ProductDTO> getProductById(@PathVariable Long id) {
        ProductDTO product = productService.getProductById(id);
        return Result.success(product);
    }

    /**
     * 分页查询商品列表
     */
    @GetMapping("/page")
    public Result<Page<ProductDTO>> getProductsByPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status) {
        Page<ProductDTO> page = productService.getProductsByPage(pageNum, pageSize, categoryId, status);
        return Result.success(page);
    }

    /**
     * 上架商品
     */
    @PutMapping("/{id}/on-shelf")
    public Result<Void> onShelf(@PathVariable Long id) {
        boolean success = productService.onShelf(id);
        return success ? Result.success(null, "上架成功")
                       : Result.failure("ON_SHELF_FAILED", "上架失败");
    }

    /**
     * 下架商品
     */
    @PutMapping("/{id}/off-shelf")
    public Result<Void> offShelf(@PathVariable Long id) {
        boolean success = productService.offShelf(id);
        return success ? Result.success(null, "下架成功")
                       : Result.failure("OFF_SHELF_FAILED", "下架失败");
    }

}
