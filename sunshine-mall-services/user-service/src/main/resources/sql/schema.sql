CREATE TABLE IF NOT EXISTS t_user (
-- 基础字段（继承自 BaseEntity）
                                      id BIGINT(20) NOT NULL COMMENT '用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag TINYINT(1) DEFAULT 0 COMMENT '逻辑删除（0-未删除，1-已删除）',
-- 用户基本信息
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
    real_name VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    phone VARCHAR(11) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    gender TINYINT(1) DEFAULT 0 COMMENT '性别（0-未知，1-男，2-女）',
    avatar VARCHAR(200) DEFAULT NULL COMMENT '头像URL',
-- 账号状态与安全
    status TINYINT(1) DEFAULT 1 COMMENT '状态（0-禁用，1-正常）',
    last_login_time DATETIME DEFAULT NULL COMMENT '最后登录时间',
    login_fail_count INT(11) DEFAULT 0 COMMENT '登录失败次数',
    lock_time DATETIME DEFAULT NULL COMMENT '账号锁定时间',
-- 主键与索引
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_phone (phone),
    UNIQUE KEY uk_email (email),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
