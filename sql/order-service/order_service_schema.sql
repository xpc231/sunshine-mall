-- ==========================================
-- Order Service Database Schema
-- ==========================================

-- 1. 订单主表
DROP TABLE IF EXISTS `order_info`;
CREATE TABLE `order_info` (
    `id` BIGINT NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `total_amount` DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
    `pay_amount` DECIMAL(10,2) NOT NULL COMMENT '实付金额',
    `freight_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '运费金额',
    `discount_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
    `pay_type` TINYINT DEFAULT NULL COMMENT '支付方式（1-支付宝，2-微信，3-银联）',
    `source_type` TINYINT NOT NULL DEFAULT 1 COMMENT '订单来源（1-PC，2-APP，3-小程序）',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态（0-待付款，1-待发货，2-已发货，3-已完成，4-已关闭，5-已取消）',
    `order_type` TINYINT NOT NULL DEFAULT 0 COMMENT '订单类型（0-普通订单，1-秒杀订单）',
    `delivery_company` VARCHAR(64) DEFAULT NULL COMMENT '物流公司',
    `delivery_sn` VARCHAR(64) DEFAULT NULL COMMENT '物流单号',
    `auto_confirm_day` INT DEFAULT 7 COMMENT '自动确认收货天数',
    `receiver_name` VARCHAR(100) NOT NULL COMMENT '收货人姓名',
    `receiver_phone` VARCHAR(32) NOT NULL COMMENT '收货人电话',
    `receiver_province` VARCHAR(32) DEFAULT NULL COMMENT '收货人省份',
    `receiver_city` VARCHAR(32) DEFAULT NULL COMMENT '收货人城市',
    `receiver_district` VARCHAR(32) DEFAULT NULL COMMENT '收货人区/县',
    `receiver_address` VARCHAR(200) NOT NULL COMMENT '收货人详细地址',
    `note` VARCHAR(500) DEFAULT NULL COMMENT '订单备注',
    `confirm_status` TINYINT DEFAULT 0 COMMENT '确认收货状态（0-未确认，1-已确认）',
    `delete_status` TINYINT DEFAULT 0 COMMENT '删除状态（0-未删除，1-已删除）',
    `payment_time` DATETIME DEFAULT NULL COMMENT '支付时间',
    `delivery_time` DATETIME DEFAULT NULL COMMENT '发货时间',
    `receive_time` DATETIME DEFAULT NULL COMMENT '确认收货时间',
    `comment_time` DATETIME DEFAULT NULL COMMENT '评价时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识（0-未删除，1-已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';

-- 2. 订单项表（订单商品明细）
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item` (
    `id` BIGINT NOT NULL COMMENT '订单项ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称',
    `product_image` VARCHAR(255) DEFAULT NULL COMMENT '商品图片',
    `sku_id` BIGINT NOT NULL COMMENT 'SKU ID',
    `sku_code` VARCHAR(64) NOT NULL COMMENT 'SKU编码',
    `sku_name` VARCHAR(200) NOT NULL COMMENT 'SKU名称',
    `sku_spec` VARCHAR(500) DEFAULT NULL COMMENT 'SKU规格属性',
    `price` DECIMAL(10,2) NOT NULL COMMENT '商品单价',
    `quantity` INT NOT NULL COMMENT '购买数量',
    `total_amount` DECIMAL(10,2) NOT NULL COMMENT '商品总金额',
    `real_amount` DECIMAL(10,2) NOT NULL COMMENT '实际金额（折扣后）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单项表';

-- 3. 订单操作历史记录表
DROP TABLE IF EXISTS `order_history`;
CREATE TABLE `order_history` (
    `id` BIGINT NOT NULL COMMENT '历史ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号',
    `operator_type` TINYINT NOT NULL COMMENT '操作人类型（0-系统，1-用户，2-后台管理员）',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(64) DEFAULT NULL COMMENT '操作人名称',
    `action` VARCHAR(32) NOT NULL COMMENT '操作类型（CREATE-创建订单，PAY-支付订单，SHIP-发货，RECEIVE-收货，CANCEL-取消订单等）',
    `status_before` TINYINT DEFAULT NULL COMMENT '操作前状态',
    `status_after` TINYINT DEFAULT NULL COMMENT '操作后状态',
    `note` VARCHAR(500) DEFAULT NULL COMMENT '操作备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单操作历史记录表';

-- 4. 购物车表
DROP TABLE IF EXISTS `cart_item`;
CREATE TABLE `cart_item` (
    `id` BIGINT NOT NULL COMMENT '购物车项ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称',
    `product_image` VARCHAR(255) DEFAULT NULL COMMENT '商品图片',
    `sku_id` BIGINT NOT NULL COMMENT 'SKU ID',
    `sku_code` VARCHAR(64) NOT NULL COMMENT 'SKU编码',
    `sku_name` VARCHAR(200) NOT NULL COMMENT 'SKU名称',
    `sku_spec` VARCHAR(500) DEFAULT NULL COMMENT 'SKU规格属性',
    `price` DECIMAL(10,2) NOT NULL COMMENT '商品单价',
    `quantity` INT NOT NULL DEFAULT 1 COMMENT '购买数量',
    `checked` TINYINT NOT NULL DEFAULT 1 COMMENT '是否选中（0-未选中，1-已选中）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_sku` (`user_id`, `sku_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- 5. 订单支付信息表
DROP TABLE IF EXISTS `order_payment`;
CREATE TABLE `order_payment` (
    `id` BIGINT NOT NULL COMMENT '支付ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `pay_amount` DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    `pay_type` TINYINT NOT NULL COMMENT '支付方式（1-支付宝，2-微信，3-银联）',
    `pay_status` TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态（0-未支付，1-已支付，2-支付失败，3-已退款）',
    `transaction_id` VARCHAR(64) DEFAULT NULL COMMENT '支付平台交易号',
    `callback_content` TEXT DEFAULT NULL COMMENT '回调内容',
    `callback_time` DATETIME DEFAULT NULL COMMENT '回调时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_id` (`order_id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_transaction_id` (`transaction_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单支付信息表';

-- 6. 订单退款表
DROP TABLE IF EXISTS `order_refund`;
CREATE TABLE `order_refund` (
    `id` BIGINT NOT NULL COMMENT '退款ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `refund_amount` DECIMAL(10,2) NOT NULL COMMENT '退款金额',
    `refund_reason` VARCHAR(200) DEFAULT NULL COMMENT '退款原因',
    `refund_status` TINYINT NOT NULL DEFAULT 0 COMMENT '退款状态（0-申请中，1-已退款，2-已拒绝）',
    `refund_type` TINYINT DEFAULT NULL COMMENT '退款类型（1-仅退款，2-退货退款）',
    `refund_transaction_id` VARCHAR(64) DEFAULT NULL COMMENT '退款交易号',
    `refuse_reason` VARCHAR(200) DEFAULT NULL COMMENT '拒绝原因',
    `apply_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `refund_time` DATETIME DEFAULT NULL COMMENT '退款时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单退款表';






