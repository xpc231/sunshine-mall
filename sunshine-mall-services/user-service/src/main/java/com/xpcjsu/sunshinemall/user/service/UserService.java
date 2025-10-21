package com.xpcjsu.sunshinemall.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xpcjsu.sunshinemall.user.dto.LoginRequest;
import com.xpcjsu.sunshinemall.user.dto.LoginResponse;
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
     *
     * @param loginRequest 登录请求
     * @return 登录响应（包含token）
     */
    LoginResponse login(LoginRequest loginRequest);

    /**
     * 用户登出
     *
     * @param token JWT Token
     */
    void logout(String token);

    /**
     * 创建用户（注册）
     *
     * @param userDTO 用户信息
     * @return 用户ID
     */
    Long createUser(UserDTO userDTO);

    /**
     * 根据ID查询用户
     *
     * @param id 用户ID
     * @return 用户信息
     */
    UserDTO getUserById(Long id);

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    UserDTO getUserByUsername(String username);

    /**
     * 查询所有用户
     *
     * @return 用户列表
     */
    List<UserDTO> getAllUsers();

    /**
     * 分页查询用户列表
     *
     * @param pageNum  页码（从1开始）
     * @param pageSize 每页大小
     * @return 分页结果
     */
    Page<UserDTO> getUsersByPage(int pageNum, int pageSize);

    /**
     * 更新用户信息
     *
     * @param userDTO 用户信息
     * @return 是否成功
     */
    boolean updateUser(UserDTO userDTO);

    /**
     * 删除用户（逻辑删除）
     *
     * @param id 用户ID
     * @return 是否成功
     */
    boolean deleteUser(Long id);
}
