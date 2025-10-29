-- ==========================================
-- Product Service Database Schema
-- ==========================================

-- 1. 商品分类表（支持树形结构）
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
    `id` BIGINT NOT NULL COMMENT '分类ID',
    `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父分类ID（0表示顶级分类）',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `level` TINYINT NOT NULL DEFAULT 1 COMMENT '分类层级（1-一级分类，2-二级分类，3-三级分类）',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序顺序',
    `icon_url` VARCHAR(255) DEFAULT NULL COMMENT '分类图标URL',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态（0-禁用，1-启用）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识（0-未删除，1-已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- 2. 商品基础信息表
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
    `id` BIGINT NOT NULL COMMENT '商品ID',
    `category_id` BIGINT NOT NULL COMMENT '分类ID',
    `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称',
    `product_code` VARCHAR(64) NOT NULL COMMENT '商品编码',
    `brand_id` BIGINT DEFAULT NULL COMMENT '品牌ID',
    `main_image` VARCHAR(255) DEFAULT NULL COMMENT '主图URL',
    `sub_images` TEXT DEFAULT NULL COMMENT '副图URL列表（JSON数组）',
    `detail` TEXT DEFAULT NULL COMMENT '商品详情描述',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '商品状态（0-下架，1-上架，2-预售）',
    `sale_count` INT NOT NULL DEFAULT 0 COMMENT '销售数量',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识（0-未删除，1-已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_code` (`product_code`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品基础信息表';

-- 3. 商品SKU表
DROP TABLE IF EXISTS `product_sku`;
CREATE TABLE `product_sku` (
    `id` BIGINT NOT NULL COMMENT 'SKU ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `sku_code` VARCHAR(64) NOT NULL COMMENT 'SKU编码',
    `sku_name` VARCHAR(200) NOT NULL COMMENT 'SKU名称',
    `spec_json` VARCHAR(500) DEFAULT NULL COMMENT '规格属性（JSON格式，如：{"颜色":"红色","尺码":"XL"}）',
    `price` DECIMAL(10,2) NOT NULL COMMENT '销售价格',
    `original_price` DECIMAL(10,2) DEFAULT NULL COMMENT '原价',
    `cost_price` DECIMAL(10,2) DEFAULT NULL COMMENT '成本价',
    `sku_image` VARCHAR(255) DEFAULT NULL COMMENT 'SKU图片',
    `weight` DECIMAL(10,2) DEFAULT NULL COMMENT '重量（单位：kg）',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态（0-禁用，1-启用）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识（0-未删除，1-已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sku_code` (`sku_code`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SKU表';

-- 4. 商品库存表
DROP TABLE IF EXISTS `product_stock`;
CREATE TABLE `product_stock` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '库存ID',
    `sku_id` BIGINT NOT NULL COMMENT 'SKU ID',
    `total_stock` INT NOT NULL DEFAULT 0 COMMENT '总库存',
    `available_stock` INT NOT NULL DEFAULT 0 COMMENT '可用库存',
    `locked_stock` INT NOT NULL DEFAULT 0 COMMENT '锁定库存（预占）',
    `version` INT NOT NULL DEFAULT 0 COMMENT '版本号（乐观锁）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sku_id` (`sku_id`),
    KEY `idx_available_stock` (`available_stock`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品库存表';

-- 5. 库存操作日志表
DROP TABLE IF EXISTS `stock_log`;
CREATE TABLE `stock_log` (
    `id` BIGINT NOT NULL COMMENT '日志ID',
    `sku_id` BIGINT NOT NULL COMMENT 'SKU ID',
    `operation_type` TINYINT NOT NULL COMMENT '操作类型（1-入库，2-扣减，3-预占，4-释放，5-退货）',
    `quantity` INT NOT NULL COMMENT '变更数量',
    `before_stock` INT NOT NULL COMMENT '变更前库存',
    `after_stock` INT NOT NULL COMMENT '变更后库存',
    `order_id` BIGINT DEFAULT NULL COMMENT '关联订单ID',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '操作人',
    PRIMARY KEY (`id`),
    KEY `idx_sku_id` (`sku_id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存操作日志表';

-- ==========================================
-- 初始化测试数据
-- ==========================================
-- 插入分类数据（三级分类示例）
INSERT INTO `category` (`id`, `parent_id`, `name`, `level`, `sort_order`, `status`) VALUES
                                                                                        (1, 0, '电子产品', 1, 1, 1),
                                                                                        (2, 0, '服装鞋包', 1, 2, 1),
                                                                                        (3, 0, '图书音像', 1, 3, 1),
                                                                                        (11, 1, '手机通讯', 2, 1, 1),
                                                                                        (12, 1, '电脑办公', 2, 2, 1),
                                                                                        (21, 2, '男装', 2, 1, 1),
                                                                                        (22, 2, '女装', 2, 2, 1),
                                                                                        (111, 11, '手机', 3, 1, 1),
                                                                                        (112, 11, '手机配件', 3, 2, 1),
                                                                                        (121, 12, '笔记本电脑', 3, 1, 1),
                                                                                        (122, 12, '台式机', 3, 2, 1);


