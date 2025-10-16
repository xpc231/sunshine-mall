package com.xpcjsu.sunshinemall.framework.cache.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 测试用户实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestUser implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    
    private String username;
    
    private String email;
    
    private Integer age;
}
