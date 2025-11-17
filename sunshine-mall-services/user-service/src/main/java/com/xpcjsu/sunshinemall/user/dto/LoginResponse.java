package com.xpcjsu.sunshinemall.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录响应 DTO
 *
 * @author sunshine-mall
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /**
     * 访问令牌
     */
    private String token;

    private String refreshToken;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;
}
