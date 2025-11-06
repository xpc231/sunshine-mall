-- 7. 秒杀订单记录表（防重复购买）
DROP TABLE IF EXISTS `seckill_order`;
CREATE TABLE `seckill_order` (
                                 `id` BIGINT NOT NULL COMMENT '秒杀订单记录ID',
                                 `seckill_product_id` BIGINT NOT NULL COMMENT '秒杀商品ID',
                                 `sku_id` BIGINT NOT NULL COMMENT 'SKU ID',
                                 `user_id` BIGINT NOT NULL COMMENT '用户ID',
                                 `order_id` BIGINT DEFAULT NULL COMMENT '订单ID（关联order_info.id）',
                                 `order_no` VARCHAR(64) DEFAULT NULL COMMENT '订单编号（关联order_info.order_no）',
                                 `quantity` INT NOT NULL DEFAULT 1 COMMENT '购买数量',
                                 `seckill_price` DECIMAL(10,2) NOT NULL COMMENT '秒杀价格',
                                 `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0-下单中，1-已下单，2-已取消）',
                                 `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_user_seckill` (`user_id`, `seckill_product_id`),
                                 KEY `idx_seckill_product_id` (`seckill_product_id`),
                                 KEY `idx_sku_id` (`sku_id`),
                                 KEY `idx_user_id` (`user_id`),
                                 KEY `idx_order_id` (`order_id`),
                                 KEY `idx_order_no` (`order_no`),
                                 KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀订单记录表';
