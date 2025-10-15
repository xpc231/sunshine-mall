package com.xpcjsu.sunshinemall.framework.database.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 测试实体类 - 用户表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("test_user")
public class TestUser extends BaseEntity {
    
    private String username;
    
    private String email;
    
    private Integer age;
}
