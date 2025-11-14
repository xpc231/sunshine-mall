package com.xpcjsu.sunshinemall.product.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xpcjsu.sunshinemall.product.dto.ProductDTO;
import com.xpcjsu.sunshinemall.product.entity.Product;
import com.xpcjsu.sunshinemall.product.mapper.ProductMapper;
import com.xpcjsu.sunshinemall.product.util.Tokenizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于 Redis 的最小可用搜索服务：关键词/类目/品牌过滤 + 热度/时间排序。
 * - 交集在应用层完成，避免一次性从 Redis 拉大集合时的复杂性；
 * - 排序支持 hot（zset）与 time（DB）；
 * - 只返回上架商品（DB 过滤）。
 */
@Service
@RequiredArgsConstructor
public class SearchService {

    private final StringRedisTemplate redis;
    private final ProductMapper productMapper;

    private static final String KW_PREFIX = "idx:kw:";
    private static final String CAT_PREFIX = "idx:cat:";
    private static final String BRAND_PREFIX = "idx:brand:";
    private static final String HOT_ZSET = "product:z:hot";

    public Page<ProductDTO> search(String q, Long categoryId, Long brandId,
                                   String sort, String order,
                                   int pageNum, int pageSize) {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 10;

        // 1) 组装 keys
        List<String> keys = new ArrayList<>();
        Set<String> toks = Tokenizer.tokens(q);
        for (String t : toks) keys.add(KW_PREFIX + t);
        if (categoryId != null) keys.add(CAT_PREFIX + categoryId);
        if (brandId != null) keys.add(BRAND_PREFIX + brandId);

        if (keys.isEmpty()) {
            // 无条件时避免全量扫描：直接返回空页或做简单 DB 查询，这里返回空页
            return new Page<>(pageNum, pageSize, 0);
        }

        // 2) 按集合大小从小到大交集
        keys.sort(Comparator.comparingLong(k -> {
            Long c = redis.opsForSet().size(k);
            return c == null ? 0L : c;
        }));
        Set<String> candidate = null;
        for (String k : keys) {
            Set<String> s = redis.opsForSet().members(k);
            if (s == null) s = Collections.emptySet();
            if (candidate == null) candidate = new HashSet<>(s);
            else {
                candidate.retainAll(s);
            }
            if (candidate.isEmpty()) break;
        }
        if (candidate == null || candidate.isEmpty()) {
            return new Page<>(pageNum, pageSize, 0);
        }

        // 3) 仅保留上架商品（DB 过滤）
        List<Long> allIds = candidate.stream().map(Long::valueOf).collect(Collectors.toList());
        List<Product> all = allIds.isEmpty() ? Collections.emptyList() : productMapper.selectBatchIds(allIds);
        List<Product> onShelf = all.stream()
                .filter(p -> p.getStatus() != null && p.getStatus() == 1) // ON_SHELF
                .collect(Collectors.toList());
        if (onShelf.isEmpty()) {
            return new Page<>(pageNum, pageSize, 0);
        }

        // 4) 排序
        boolean asc = "asc".equalsIgnoreCase(order);
        String sortKey = sort == null ? "hot" : sort.toLowerCase();
        if ("time".equals(sortKey)) {
            onShelf.sort(Comparator.comparing(Product::getCreateTime,
                    asc ? Comparator.naturalOrder() : Comparator.reverseOrder()));
        } else { // 默认 hot
            onShelf.sort(Comparator.comparingDouble(p -> {
                Double score = redis.opsForZSet().score(HOT_ZSET, String.valueOf(p.getId()));
                return score == null ? 0.0 : score;
            }));
            if (!asc) Collections.reverse(onShelf);
        }

        // 5) 分页与映射
        long total = onShelf.size();
        int from = Math.max((pageNum - 1) * pageSize, 0);
        int to = Math.min(from + pageSize, onShelf.size());
        List<ProductDTO> items = Collections.emptyList();
        if (from < to) {
            items = onShelf.subList(from, to).stream()
                    .map(p -> BeanUtil.copyProperties(p, ProductDTO.class))
                    .collect(Collectors.toList());
        }

        Page<ProductDTO> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(items);
        return page;
    }
}