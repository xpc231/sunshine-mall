-- ==========================================
-- Pay Service Database Schema (UTF-8)
-- 说明：与项目风格保持一致，MySQL 8.x / InnoDB / utf8mb4；字段含义见注释
-- ==========================================

-- 1. 支付主表（交易记录）
DROP TABLE IF EXISTS `pay_transaction`;
CREATE TABLE `pay_transaction` (
    `id` BIGINT NOT NULL COMMENT '支付ID（主键，Snowflake 等分布式ID）',
    `pay_sn` VARCHAR(64) NOT NULL COMMENT '支付单号（唯一）',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '支付金额（元）',
    `currency` VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT '币种（默认CNY）',
    `pay_type` TINYINT NOT NULL COMMENT '支付方式（1-支付宝，2-微信，3-银联）',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态（0-未支付，1-支付成功，2-支付失败，3-已关闭，4-部分退款，5-全额退款）',
    `subject` VARCHAR(128) DEFAULT NULL COMMENT '交易标题',
    `body` VARCHAR(512) DEFAULT NULL COMMENT '交易描述',
    `channel_trade_no` VARCHAR(64) DEFAULT NULL COMMENT '支付平台交易号',
    `client_ip` VARCHAR(64) DEFAULT NULL COMMENT '客户端IP',
    `expire_time` DATETIME DEFAULT NULL COMMENT '支付过期时间',
    `callback_time` DATETIME DEFAULT NULL COMMENT '支付回调完成时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识（0-未删除，1-已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pay_sn` (`pay_sn`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付主表（交易记录）';

-- 2. 退款表
DROP TABLE IF EXISTS `pay_refund`;
CREATE TABLE `pay_refund` (
    `id` BIGINT NOT NULL COMMENT '退款ID（主键）',
    `refund_sn` VARCHAR(64) NOT NULL COMMENT '退款单号（唯一）',
    `pay_id` BIGINT DEFAULT NULL COMMENT '支付ID（关联 pay_transaction.id）',
    `pay_sn` VARCHAR(64) NOT NULL COMMENT '支付单号（关联 pay_transaction.pay_sn）',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '退款金额（元）',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '退款状态（0-申请中，1-成功，2-失败）',
    `reason` VARCHAR(256) DEFAULT NULL COMMENT '退款原因',
    `channel_refund_no` VARCHAR(64) DEFAULT NULL COMMENT '支付平台退款单号',
    `callback_time` DATETIME DEFAULT NULL COMMENT '退款回调完成时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识（0-未删除，1-已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_refund_sn` (`refund_sn`),
    KEY `idx_pay_sn` (`pay_sn`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款表';

-- 3. 支付渠道配置表（敏感信息使用占位符或外部密钥管理）
DROP TABLE IF EXISTS `pay_channel_config`;
CREATE TABLE `pay_channel_config` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `channel` TINYINT NOT NULL COMMENT '渠道（1-支付宝，2-微信，3-银联）',
    `app_id` VARCHAR(128) DEFAULT NULL COMMENT '应用ID（占位符）',
    `mch_id` VARCHAR(128) DEFAULT NULL COMMENT '商户ID（占位符）',
    `private_key` VARCHAR(2048) DEFAULT NULL COMMENT '私钥（占位符，不建议明文）',
    `public_key` VARCHAR(2048) DEFAULT NULL COMMENT '公钥（占位符）',
    `notify_url` VARCHAR(256) DEFAULT NULL COMMENT '异步通知URL',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态（0-禁用，1-启用）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_channel` (`channel`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付渠道配置表';

-- 4. 回调通知日志表
DROP TABLE IF EXISTS `pay_notify_log`;
CREATE TABLE `pay_notify_log` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `ref_no` VARCHAR(64) NOT NULL COMMENT '关联单号（paySn/refundSn）',
    `notify_type` TINYINT NOT NULL COMMENT '通知类型（1-支付，2-退款）',
    `payload` TEXT DEFAULT NULL COMMENT '通知原始内容（脱敏存储）',
    `sign_verified` TINYINT NOT NULL DEFAULT 0 COMMENT '签名是否验证通过（0-否，1-是）',
    `handle_status` TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态（0-待处理，1-成功，2-失败）',
    `handle_message` VARCHAR(512) DEFAULT NULL COMMENT '处理结果信息',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_ref_no` (`ref_no`),
    KEY `idx_notify_type` (`notify_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回调通知日志表';

-- ==========================================
-- 结束
-- ==========================================