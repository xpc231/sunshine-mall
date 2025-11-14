package com.xpcjsu.sunshinemall.product.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 极简分词器：
 * - 英文/数字按非字母数字分隔；
 * - 中文/混合做 2-gram 切分；
 * 避免过度设计，仅用于最小可用搜索索引。
 */
public class Tokenizer {

    public static Set<String> tokens(String text) {
        if (text == null) return Collections.emptySet();
        text = text.trim().toLowerCase();
        if (text.isEmpty()) return Collections.emptySet();

        Set<String> res = new HashSet<>();
        String[] parts = text.split("[^\\p{L}\\p{N}]+");
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (p.length() >= 2) {
                res.add(p);
                for (int i = 0; i + 2 <= p.length(); i++) {
                    res.add(p.substring(i, i + 2));
                }
            } else {
                res.add(p);
            }
        }
        return res;
    }
}