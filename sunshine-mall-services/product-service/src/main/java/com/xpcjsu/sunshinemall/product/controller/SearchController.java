package com.xpcjsu.sunshinemall.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import com.xpcjsu.sunshinemall.product.dto.ProductDTO;
import com.xpcjsu.sunshinemall.product.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 商品搜索控制器（基于 Redis 的最小实现）
 */
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
@Validated
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/search")
    public Result<Page<ProductDTO>> search(@RequestParam(required = false) String q,
                                           @RequestParam(required = false) Long categoryId,
                                           @RequestParam(required = false) Long brandId,
                                           @RequestParam(required = false) String sort,
                                           @RequestParam(required = false) String order,
                                           @RequestParam(defaultValue = "1") int pageNum,
                                           @RequestParam(defaultValue = "10") int pageSize) {
        Page<ProductDTO> page = searchService.search(q, categoryId, brandId, sort, order, pageNum, pageSize);
        return Result.success(page);
    }
}