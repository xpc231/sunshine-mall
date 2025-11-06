-- 6. 秒杀商品表
DROP TABLE IF EXISTS `seckill_product`;
CREATE TABLE `seckill_product` (
                                   `id` BIGINT NOT NULL COMMENT '秒杀商品ID',
                                   `product_id` BIGINT NOT NULL COMMENT '商品ID',
                                   `sku_id` BIGINT NOT NULL COMMENT 'SKU ID',
                                   `seckill_price` DECIMAL(10,2) NOT NULL COMMENT '秒杀价格',
                                   `seckill_stock` INT NOT NULL DEFAULT 0 COMMENT '秒杀库存数量',
                                   `original_price` DECIMAL(10,2) DEFAULT NULL COMMENT '原价（用于展示）',
                                   `start_time` DATETIME NOT NULL COMMENT '秒杀开始时间',
                                   `end_time` DATETIME NOT NULL COMMENT '秒杀结束时间',
                                   `limit_quantity` INT NOT NULL DEFAULT 1 COMMENT '限购数量（每个用户限购数量）',
                                   `status` TINYINT NOT NULL DEFAULT 0 COMMENT '秒杀状态（0-未开始，1-进行中，2-已结束，3-已取消）',
                                   `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序顺序',
                                   `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
                                   `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
                                   `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识（0-未删除，1-已删除）',
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uk_sku_id` (`sku_id`),
                                   KEY `idx_product_id` (`product_id`),
                                   KEY `idx_status` (`status`),
                                   KEY `idx_time_range` (`start_time`, `end_time`),
                                   KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀商品表';