package com.xpcjsu.sunshinemall.product.constant;

/**
 * 商品服务常量
 *
 * @author xpcjsu
 */
public class ProductConstants {

    /**
     * 分类相关常量
     */
    public static class Category {
        /** 顶级分类父ID */
        public static final Long ROOT_PARENT_ID = 0L;
        
        /** 一级分类 */
        public static final Integer LEVEL_ONE = 1;
        
        /** 二级分类 */
        public static final Integer LEVEL_TWO = 2;
        
        /** 三级分类 */
        public static final Integer LEVEL_THREE = 3;
        
        /** 分类状态 - 禁用 */
        public static final Integer STATUS_DISABLED = 0;
        
        /** 分类状态 - 启用 */
        public static final Integer STATUS_ENABLED = 1;
    }

    /**
     * 商品状态常量
     */
    public static class ProductStatus {
        /** 下架 */
        public static final Integer OFF_SHELF = 0;
        
        /** 上架 */
        public static final Integer ON_SHELF = 1;
        
        /** 预售 */
        public static final Integer PRE_SALE = 2;
    }

    /**
     * SKU状态常量
     */
    public static class SkuStatus {
        /** 禁用 */
        public static final Integer DISABLED = 0;
        
        /** 启用 */
        public static final Integer ENABLED = 1;
    }

    /**
     * 库存操作类型
     */
    public static class StockOperationType {
        /** 入库 */
        public static final Integer IN_STOCK = 1;
        
        /** 扣减 */
        public static final Integer DEDUCT = 2;
        
        /** 预占 */
        public static final Integer LOCK = 3;
        
        /** 释放 */
        public static final Integer UNLOCK = 4;
        
        /** 退货 */
        public static final Integer RETURN = 5;
    }

    /**
     * 缓存相关常量
     */
    public static class Cache {
        /** 商品详情缓存前缀 */
        public static final String PRODUCT_DETAIL_PREFIX = "product:detail:";
        
        /** SKU详情缓存前缀 */
        public static final String SKU_DETAIL_PREFIX = "product:sku:";
        
        /** 分类树缓存键 */
        public static final String CATEGORY_TREE_KEY = "product:category:tree";
        
        /** 库存缓存前缀 */
        public static final String STOCK_PREFIX = "product:stock:";
        
        /** 商品详情缓存过期时间（秒） */
        public static final long PRODUCT_CACHE_EXPIRE = 3600L;
        
        /** 分类树缓存过期时间（秒） */
        public static final long CATEGORY_CACHE_EXPIRE = 7200L;
        
        /** 库存缓存过期时间（秒） */
        public static final long STOCK_CACHE_EXPIRE = 1800L;

        /** 秒杀库存缓存前缀 */
        public static final String SECKILL_STOCK_KEY_PREFIX = "seckill:stock:";
        
        /** 秒杀商品详情缓存前缀 */
        public static final String SECKILL_PRODUCT_DETAIL_PREFIX = "seckill:product:detail:";
        
        /** 秒杀商品SKU缓存前缀 */
        public static final String SECKILL_PRODUCT_SKU_PREFIX = "seckill:product:sku:";
        
        /** 进行中的秒杀商品列表缓存键 */
        public static final String SECKILL_PRODUCT_IN_PROGRESS_KEY = "seckill:product:in-progress";
        
        /** 秒杀商品分页查询缓存前缀 */
        public static final String SECKILL_PRODUCT_PAGE_PREFIX = "seckill:product:page:";
        
        /** 秒杀商品详情缓存过期时间（秒） */
        public static final long SECKILL_PRODUCT_CACHE_EXPIRE = 1800L; // 30分钟
        
        /** 进行中的秒杀商品列表缓存过期时间（秒） */
        public static final long SECKILL_PRODUCT_IN_PROGRESS_CACHE_EXPIRE = 300L; // 5分钟
        
        /** 秒杀商品分页查询缓存过期时间（秒） */
        public static final long SECKILL_PRODUCT_PAGE_CACHE_EXPIRE = 300L; // 5分钟
        
        /** 秒杀商品详情锁键前缀 */
        public static final String SECKILL_PRODUCT_LOCK_DETAIL_PREFIX = "seckill:product:lock:detail:";
        
        /** 秒杀商品SKU锁键前缀 */
        public static final String SECKILL_PRODUCT_LOCK_SKU_PREFIX = "seckill:product:lock:sku:";
        
        /** 秒杀库存扣减锁键前缀 */
        public static final String SECKILL_STOCK_LOCK_DEDUCT_PREFIX = "seckill:stock:deduct:";
        
        /** 秒杀库存回滚锁键前缀 */
        public static final String SECKILL_STOCK_LOCK_ROLLBACK_PREFIX = "seckill:stock:rollback:";
        
        /** 互斥锁键前缀（用于缓存击穿防护） */
        public static final String MUTEX_LOCK_PREFIX = "mutex:cache:";
        
        /** 互斥锁过期时间（秒） */
        public static final long MUTEX_LOCK_EXPIRE_TIME = 10L;
        
        /** 秒杀缓存刷新线程名称 */
        public static final String SECKILL_CACHE_REFRESH_THREAD_NAME = "seckill-cache-refresh";
        
        /** 分布式锁默认过期时间（秒） */
        public static final long DISTRIBUTED_LOCK_DEFAULT_EXPIRE_TIME = 30L;
        
        /** 互斥锁值（用于SETNX） */
        public static final String MUTEX_LOCK_VALUE = "1";
        
        /** 互斥锁等待重试时间（毫秒） */
        public static final long MUTEX_LOCK_RETRY_WAIT_TIME_MS = 50L;
        
        /** 空值缓存默认过期时间（秒） */
        public static final long NULL_VALUE_CACHE_EXPIRE_TIME = 300L; // 5分钟
    }

    /**
     * RocketMQ 主题常量
     */
    public static class MQ {
        /** 库存变更主题 */
        public static final String STOCK_CHANGE_TOPIC = "stock-change-topic";
        
        /** 商品上下架主题 */
        public static final String PRODUCT_STATUS_TOPIC = "product-status-topic";
    }

    /**
     * 秒杀商品状态常量
     */
    public static class SeckillStatus {
        /** 未开始 */
        public static final Integer NOT_STARTED = 0;
        
        /** 进行中 */
        public static final Integer IN_PROGRESS = 1;
        
        /** 已结束 */
        public static final Integer ENDED = 2;
        
        /** 已取消 */
        public static final Integer CANCELLED = 3;
    }

}
