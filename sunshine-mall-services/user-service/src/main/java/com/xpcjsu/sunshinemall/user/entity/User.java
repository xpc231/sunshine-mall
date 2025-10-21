package com.xpcjsu.sunshinemall.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xpcjsu.sunshinemall.framework.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体类
 *
 * @author sunshine-mall
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user")
public class User extends BaseEntity {

    /**
     * 用户名（登录账号）
     */
    private String username;

    /**
     * 密码（加密存储）
     */
    private String password;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 性别：0-女，1-男
     */
    private Integer gender;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 用户状态：0-禁用，1-正常
     */
    private Integer status;

    /**
     * 最后登录时间
     */
    private java.time.LocalDateTime lastLoginTime;

    /**
     * 登录失败次数
     */
    private Integer loginFailCount;

    /**
     * 锁定时间
     */
    private java.time.LocalDateTime lockTime;
}
