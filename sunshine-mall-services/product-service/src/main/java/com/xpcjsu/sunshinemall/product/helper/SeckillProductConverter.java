package com.xpcjsu.sunshinemall.product.helper;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xpcjsu.sunshinemall.product.dto.SeckillProductDTO;
import com.xpcjsu.sunshinemall.product.entity.SeckillProduct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 秒杀商品转换辅助类
 * <p>
 * 封装秒杀商品相关的对象转换操作，包括DTO/Entity转换和缓存反序列化转换
 *
 * @author xpcjsu
 */
@Slf4j
@Component
public class SeckillProductConverter {

    /**
     * 转换为实体
     *
     * @param dto DTO对象
     * @return 实体对象
     */
    public SeckillProduct convertToEntity(SeckillProductDTO dto) {
        SeckillProduct entity = new SeckillProduct();
        BeanUtil.copyProperties(dto, entity);
        return entity;
    }

    /**
     * 转换为DTO
     *
     * @param entity 实体对象
     * @return DTO对象
     */
    public SeckillProductDTO convertToDTO(SeckillProduct entity) {
        SeckillProductDTO dto = new SeckillProductDTO();
        BeanUtil.copyProperties(entity, dto);
        return dto;
    }

    /**
     * 转换缓存的DTO对象（处理反序列化问题）
     * <p>
     * 使用hutool的JSONUtil.toBean()自动处理LinkedHashMap、Map等类型转换
     *
     * @param cachedObj 从缓存获取的对象
     * @return 转换后的DTO对象，如果转换失败返回null
     */
    public SeckillProductDTO convertCachedDTO(Object cachedObj) {
        try {
            // 处理空值缓存
            if (cachedObj == null) {
                return null;
            }
            
            // 检查是否是空值缓存标记
            String nullValue = com.xpcjsu.sunshinemall.framework.cache.constant.CacheConstant.NULL_VALUE;
            if (nullValue.equals(cachedObj) || "NULL".equals(cachedObj)) {
                return null;
            }
            
            if (cachedObj instanceof SeckillProductDTO) {
                // 已经是目标类型，直接返回
                return (SeckillProductDTO) cachedObj;
            }
            
            // 使用hutool的JSONUtil自动转换（支持LinkedHashMap、Map、JSON字符串等）
            return JSONUtil.toBean(JSONUtil.toJsonStr(cachedObj), SeckillProductDTO.class);
        } catch (Exception e) {
            log.error("转换缓存DTO对象失败 - type: {}, error: {}", 
                    cachedObj != null ? cachedObj.getClass().getName() : "null", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 转换缓存的List对象（处理反序列化问题）
     * <p>
     * 使用hutool的JSONUtil自动处理List中的LinkedHashMap转换
     *
     * @param cachedObj 从缓存获取的对象
     * @return 转换后的List对象，如果转换失败返回null
     */
    @SuppressWarnings("unchecked")
    public List<SeckillProductDTO> convertCachedList(Object cachedObj) {
        try {
            if (cachedObj == null) {
                return new ArrayList<>();
            }
            
            if (!(cachedObj instanceof List)) {
                log.warn("缓存对象不是List类型 - type: {}", cachedObj.getClass().getName());
                return null;
            }

            List<?> cachedList = (List<?>) cachedObj;
            if (cachedList.isEmpty()) {
                return new ArrayList<>();
            }

            // 检查第一个元素的类型
            Object firstElement = cachedList.get(0);
            if (firstElement instanceof SeckillProductDTO) {
                // 已经是目标类型，直接转换
                return (List<SeckillProductDTO>) cachedList;
            }

            // 使用hutool的JSONUtil自动转换（支持List中的LinkedHashMap、Map等）
            return JSONUtil.toList(JSONUtil.toJsonStr(cachedObj), SeckillProductDTO.class);
        } catch (Exception e) {
            log.error("转换缓存List对象失败", e);
            return null;
        }
    }

    /**
     * 转换缓存的Page对象（处理反序列化问题）
     * <p>
     * 使用hutool的JSONUtil自动处理Page中records的LinkedHashMap转换
     *
     * @param cachedObj 从缓存获取的对象
     * @return 转换后的Page对象，如果转换失败返回null
     */
    @SuppressWarnings("unchecked")
    public Page<SeckillProductDTO> convertCachedPage(Object cachedObj) {
        try {
            if (cachedObj == null) {
                return null;
            }
            
            if (!(cachedObj instanceof Page)) {
                log.warn("缓存对象不是Page类型 - type: {}", cachedObj.getClass().getName());
                return null;
            }

            Page<?> cachedPage = (Page<?>) cachedObj;
            List<?> records = cachedPage.getRecords();
            
            // 重新构建Page对象
            Page<SeckillProductDTO> dtoPage = new Page<>(cachedPage.getCurrent(),
                    cachedPage.getSize(), cachedPage.getTotal());
            
            if (records == null || records.isEmpty()) {
                // 空列表，直接返回
                dtoPage.setRecords(new ArrayList<>());
                return dtoPage;
            }

            // 检查第一个元素的类型
            Object firstRecord = records.get(0);
            List<SeckillProductDTO> dtoList;

            if (firstRecord instanceof SeckillProductDTO) {
                // 已经是目标类型，直接转换
                dtoList = (List<SeckillProductDTO>) records;
            } else {
                // 使用hutool的JSONUtil自动转换（支持List中的LinkedHashMap、Map等）
                dtoList = JSONUtil.toList(JSONUtil.toJsonStr(records), SeckillProductDTO.class);
            }

            dtoPage.setRecords(dtoList);
            return dtoPage;
        } catch (Exception e) {
            log.error("转换缓存Page对象失败", e);
            return null;
        }
    }
}

