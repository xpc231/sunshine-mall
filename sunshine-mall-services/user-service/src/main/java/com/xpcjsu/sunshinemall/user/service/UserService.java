package com.xpcjsu.sunshinemall.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xpcjsu.sunshinemall.user.dto.LoginRequest;
import com.xpcjsu.sunshinemall.user.dto.LoginResponse;
import com.xpcjsu.sunshinemall.user.dto.LoginRefreshResponse;
import com.xpcjsu.sunshinemall.user.dto.RefreshTokenRequest;
import com.xpcjsu.sunshinemall.user.dto.UserDTO;

import java.util.List;

/**
 * 用户服务接口
 *
 * @author sunshine-mall
 * @since 1.0.0
 */
public interface UserService {

    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest loginRequest);

    /**
     * 用户登出
     *
     * @param token JWT Token
     */
    void logout(String token);

    // 刷新令牌
    LoginRefreshResponse refreshToken(RefreshTokenRequest request);

    /**
     * 创建用户（注册）
     */
    Long createUser(UserDTO userDTO);

    /**
     * 根据ID查询用户
     */
    UserDTO getUserById(Long id);

    /**
     * 根据用户名查询用户
     */
    UserDTO getUserByUsername(String username);

    /**
     * 查询所有用户
     */
    List<UserDTO> getAllUsers();

    /**
     * 分页查询用户列表
     */
    Page<UserDTO> getUsersByPage(int pageNum, int pageSize);

    /**
     * 更新用户信息
     */
    boolean updateUser(UserDTO userDTO);

    /**
     * 删除用户（逻辑删除）
     */
    boolean deleteUser(Long id);
}
