package com.xpcjsu.sunshinemall.product.service;

import com.xpcjsu.sunshinemall.product.entity.Product;
import com.xpcjsu.sunshinemall.product.util.Tokenizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 搜索索引维护服务（基于 Redis）：
 * - 关键词倒排：idx:kw:{token} -> Set<productId>
 * - 类目倒排：idx:cat:{categoryId} -> Set<productId>
 * - 品牌倒排：idx:brand:{brandId} -> Set<productId>
 * 最小实现，避免过度设计。
 */
@Service
@RequiredArgsConstructor
public class SearchIndexService {

    private final StringRedisTemplate redis;

    private static final String KW_PREFIX = "idx:kw:";
    private static final String CAT_PREFIX = "idx:cat:";
    private static final String BRAND_PREFIX = "idx:brand:";

    public void buildIndex(Product p) {
        if (p == null || p.getId() == null) return;
        String pid = String.valueOf(p.getId());
        // 关键词（使用商品名称）
        Set<String> toks = Tokenizer.tokens(p.getProductName());
        for (String t : toks) {
            redis.opsForSet().add(KW_PREFIX + t, pid);
        }
        // 类目/品牌
        if (p.getCategoryId() != null) {
            redis.opsForSet().add(CAT_PREFIX + p.getCategoryId(), pid);
        }
        if (p.getBrandId() != null) {
            redis.opsForSet().add(BRAND_PREFIX + p.getBrandId(), pid);
        }
    }

    public void removeIndex(Product p) {
        if (p == null || p.getId() == null) return;
        String pid = String.valueOf(p.getId());
        // 关键词移除
        Set<String> toks = Tokenizer.tokens(p.getProductName());
        for (String t : toks) {
            redis.opsForSet().remove(KW_PREFIX + t, pid);
        }
        // 类目/品牌移除
        if (p.getCategoryId() != null) {
            redis.opsForSet().remove(CAT_PREFIX + p.getCategoryId(), pid);
        }
        if (p.getBrandId() != null) {
            redis.opsForSet().remove(BRAND_PREFIX + p.getBrandId(), pid);
        }
    }

    public void updateIndex(Product oldP, Product newP) {
        // 先移除旧索引，再建立新索引
        removeIndex(oldP);
        buildIndex(newP);
    }
}