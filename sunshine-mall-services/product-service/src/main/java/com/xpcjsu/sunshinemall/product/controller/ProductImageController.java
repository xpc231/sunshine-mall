package com.xpcjsu.sunshinemall.product.controller;

import com.aliyun.oss.OSS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import com.xpcjsu.sunshinemall.product.config.OssProperties;
import com.xpcjsu.sunshinemall.product.entity.Product;
import com.xpcjsu.sunshinemall.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/product/images")
@RequiredArgsConstructor
public class ProductImageController {
    private final OSS oss;
    private final OssProperties props;
    private final ProductMapper productMapper;

    @PostMapping("/upload-main/{productId}")
    public Result<String> uploadMain(@PathVariable Long productId, MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            return Result.failure("FILE_EMPTY", "文件为空");
        }
        Product p = productMapper.selectById(productId);
        if (p == null) {
            return Result.failure("NOT_FOUND", "商品不存在");
        }
        String key = buildKey(productId, file.getOriginalFilename());
        try (InputStream in = file.getInputStream()) {
            oss.putObject(props.getBucket(), key, in);
        }
        String url = buildUrl(key);
        productMapper.update(null, new LambdaUpdateWrapper<Product>().eq(Product::getId, productId).set(Product::getMainImage, url));
        return Result.success(url, "上传成功");
    }

    @PostMapping("/upload-sub/{productId}")
    public Result<String> uploadSub(@PathVariable Long productId, MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            return Result.failure("FILE_EMPTY", "文件为空");
        }
        Product p = productMapper.selectById(productId);
        if (p == null) {
            return Result.failure("NOT_FOUND", "商品不存在");
        }
        String key = buildKey(productId, file.getOriginalFilename());
        try (InputStream in = file.getInputStream()) {
            oss.putObject(props.getBucket(), key, in);
        }
        String url = buildUrl(key);
        String sub = p.getSubImages();
        String newSub;
        if (sub == null || sub.isEmpty()) {
            newSub = "[\"" + url + "\"]";
        } else {
            newSub = sub.substring(0, sub.length() - 1) + ",\"" + url + "\"]";
        }
        productMapper.update(null, new LambdaUpdateWrapper<Product>().eq(Product::getId, productId).set(Product::getSubImages, newSub));
        return Result.success(url, "上传成功");
    }

    private String buildKey(Long productId, String name) {
        String ext = getExt(name);
        return "product/images/" + productId + "/" + LocalDate.now().toString().replace("-", "") + "/" + UUID.randomUUID() + (ext == null ? "" : ("." + ext));
    }

    private String buildUrl(String key) {
        return "https://" + props.getBucket() + "." + props.getEndpoint().replace("http://", "").replace("https://", "") + "/" + key;
    }

    private String getExt(String name) {
        if (name == null) return null;
        int i = name.lastIndexOf('.');
        return i > 0 ? name.substring(i + 1) : null;
    }
}