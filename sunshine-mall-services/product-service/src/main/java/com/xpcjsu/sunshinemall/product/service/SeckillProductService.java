package com.xpcjsu.sunshinemall.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xpcjsu.sunshinemall.product.dto.SeckillProductDTO;

import java.util.List;

/**
 * 秒杀商品服务接口
 *
 * @author xpcjsu
 */
public interface SeckillProductService {

    /**
     * 创建秒杀商品
     *
     * @param seckillProductDTO 秒杀商品信息
     * @return 秒杀商品ID
     */
    Long createSeckillProduct(SeckillProductDTO seckillProductDTO);

    /**
     * 更新秒杀商品
     *
     * @param seckillProductDTO 秒杀商品信息
     * @return 是否成功
     */
    boolean updateSeckillProduct(SeckillProductDTO seckillProductDTO);

    /**
     * 删除秒杀商品
     *
     * @param id 秒杀商品ID
     * @return 是否成功
     */
    boolean deleteSeckillProduct(Long id);

    /**
     * 根据ID获取秒杀商品详情
     *
     * @param id 秒杀商品ID
     * @return 秒杀商品详情
     */
    SeckillProductDTO getSeckillProductById(Long id);

    /**
     * 根据SKU ID获取秒杀商品
     *
     * @param skuId SKU ID
     * @return 秒杀商品详情
     */
    SeckillProductDTO getSeckillProductBySkuId(Long skuId);

    /**
     * 分页查询秒杀商品列表
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param status   秒杀状态（可选）
     * @return 秒杀商品分页列表
     */
    Page<SeckillProductDTO> getSeckillProductsByPage(int pageNum, int pageSize, Integer status);

    /**
     * 查询进行中的秒杀商品列表
     *
     * @return 秒杀商品列表
     */
    List<SeckillProductDTO> getInProgressSeckillProducts();

    /**
     * 扣减秒杀库存
     * <p>
     * 双重防护机制：
     * 1. Redis预扣减（Lua脚本保证原子性）
     * 2. 数据库扣减（最终保障）
     *
     * @param id       秒杀商品ID
     * @param quantity 扣减数量
     * @return 是否成功
     */
    boolean deductSeckillStock(Long id, Integer quantity);

    /**
     * 预热秒杀库存到Redis
     * <p>
     * 在秒杀活动开始前调用，将数据库中的库存加载到Redis
     *
     * @param id 秒杀商品ID
     * @return 是否成功
     */
    boolean warmupSeckillStock(Long id);

    /**
     * 回滚秒杀库存（增加库存）
     * <p>
     * 用于订单创建失败时回滚库存
     *
     * @param id       秒杀商品ID
     * @param quantity 回滚数量
     * @return 是否成功
     */
    boolean rollbackSeckillStock(Long id, Integer quantity);

}

