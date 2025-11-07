package com.xpcjsu.sunshinemall.product.helper;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xpcjsu.sunshinemall.product.dto.SeckillProductDTO;
import com.xpcjsu.sunshinemall.product.entity.SeckillProduct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
     * Redis反序列化时，DTO对象可能是LinkedHashMap而不是SeckillProductDTO
     * 需要手动转换为SeckillProductDTO
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
                // 已经是SeckillProductDTO类型，直接返回
                return (SeckillProductDTO) cachedObj;
            } else if (cachedObj instanceof java.util.LinkedHashMap) {
                // 是LinkedHashMap，需要转换为SeckillProductDTO
                SeckillProductDTO dto = new SeckillProductDTO();
                BeanUtil.copyProperties(cachedObj, dto);
                return dto;
            } else {
                log.warn("缓存对象类型不支持 - type: {}, value: {}", cachedObj.getClass().getName(), cachedObj);
                return null;
            }
        } catch (Exception e) {
            log.error("转换缓存DTO对象失败 - type: {}, error: {}", 
                    cachedObj != null ? cachedObj.getClass().getName() : "null", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 转换缓存的List对象（处理反序列化问题）
     * <p>
     * Redis反序列化时，List中的元素可能是LinkedHashMap而不是SeckillProductDTO
     * 需要手动转换为SeckillProductDTO
     *
     * @param cachedObj 从缓存获取的对象
     * @return 转换后的List对象，如果转换失败返回null
     */
    @SuppressWarnings("unchecked")
    public List<SeckillProductDTO> convertCachedList(Object cachedObj) {
        try {
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
            List<SeckillProductDTO> dtoList;

            if (firstElement instanceof SeckillProductDTO) {
                // 已经是SeckillProductDTO类型，直接转换
                dtoList = (List<SeckillProductDTO>) cachedList;
            } else if (firstElement instanceof java.util.LinkedHashMap) {
                // 是LinkedHashMap，需要转换为SeckillProductDTO
                dtoList = cachedList.stream()
                        .map(element -> {
                            if (element instanceof java.util.LinkedHashMap) {
                                SeckillProductDTO dto = new SeckillProductDTO();
                                BeanUtil.copyProperties(element, dto);
                                return dto;
                            } else {
                                log.warn("缓存元素类型异常 - type: {}", element.getClass().getName());
                                return null;
                            }
                        })
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());
            } else {
                log.warn("缓存元素类型不支持 - type: {}", firstElement.getClass().getName());
                return null;
            }

            return dtoList;
        } catch (Exception e) {
            log.error("转换缓存List对象失败", e);
            return null;
        }
    }

    /**
     * 转换缓存的Page对象（处理反序列化问题）
     * <p>
     * Redis反序列化时，Page中的records列表中的元素可能是LinkedHashMap而不是SeckillProductDTO
     * 需要手动转换为SeckillProductDTO
     *
     * @param cachedObj 从缓存获取的对象
     * @return 转换后的Page对象，如果转换失败返回null
     */
    @SuppressWarnings("unchecked")
    public Page<SeckillProductDTO> convertCachedPage(Object cachedObj) {
        try {
            if (!(cachedObj instanceof Page)) {
                log.warn("缓存对象不是Page类型 - type: {}", cachedObj.getClass().getName());
                return null;
            }

            Page<?> cachedPage = (Page<?>) cachedObj;
            List<?> records = cachedPage.getRecords();
            if (records == null || records.isEmpty()) {
                // 空列表，直接返回
                Page<SeckillProductDTO> dtoPage = new Page<>(cachedPage.getCurrent(), cachedPage.getSize(), cachedPage.getTotal());
                dtoPage.setRecords(new ArrayList<>());
                return dtoPage;
            }

            // 检查第一个元素的类型
            Object firstRecord = records.get(0);
            List<SeckillProductDTO> dtoList;

            if (firstRecord instanceof SeckillProductDTO) {
                // 已经是SeckillProductDTO类型，直接转换
                dtoList = (List<SeckillProductDTO>) records;
            } else if (firstRecord instanceof java.util.LinkedHashMap) {
                // 是LinkedHashMap，需要转换为SeckillProductDTO
                dtoList = records.stream()
                        .map(record -> {
                            if (record instanceof java.util.LinkedHashMap) {
                                SeckillProductDTO dto = new SeckillProductDTO();
                                BeanUtil.copyProperties(record, dto);
                                return dto;
                            } else {
                                log.warn("缓存记录类型异常 - type: {}", record.getClass().getName());
                                return null;
                            }
                        })
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());
            } else {
                log.warn("缓存记录类型不支持 - type: {}", firstRecord.getClass().getName());
                return null;
            }

            // 重新构建Page对象
            Page<SeckillProductDTO> dtoPage = new Page<>(cachedPage.getCurrent(), cachedPage.getSize(), cachedPage.getTotal());
            dtoPage.setRecords(dtoList);
            return dtoPage;
        } catch (Exception e) {
            log.error("转换缓存Page对象失败", e);
            return null;
        }
    }
}

