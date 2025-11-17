package com.xpcjsu.sunshinemall.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import com.xpcjsu.sunshinemall.framework.idempotent.annotation.Idempotent;
import com.xpcjsu.sunshinemall.user.dto.LoginRequest;
import com.xpcjsu.sunshinemall.user.dto.LoginResponse;
import com.xpcjsu.sunshinemall.user.dto.LoginRefreshResponse;
import com.xpcjsu.sunshinemall.user.dto.RefreshTokenRequest;
import com.xpcjsu.sunshinemall.user.dto.UserDTO;
import com.xpcjsu.sunshinemall.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 *
 * @author sunshine-mall
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = userService.login(loginRequest);
        return Result.success(response, "登录成功");
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader) {
        // 提取 token（去除 "Bearer " 前缀）
        String token = authHeader.startsWith("Bearer ") 
            ? authHeader.substring(7) 
            : authHeader;
        
        userService.logout(token);
        return Result.success(null, "登出成功");
    }

    // 刷新令牌
    @PostMapping("/refresh")
    public Result<LoginRefreshResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        LoginRefreshResponse resp = userService.refreshToken(request);
        return Result.success(resp, "刷新成功");
    }

    /**
     * 创建用户（注册）
     */
    @PostMapping("/register")
    @Idempotent(key = "#userDTO.username", expireTime = 60, message = "请勿重复注册")
    public Result<Long> createUser(@Valid @RequestBody UserDTO userDTO) {
        Long userId = userService.createUser(userDTO);
        return Result.success(userId, "注册成功");
    }

    /**
     * 根据ID查询用户
     */
    @GetMapping("/{id}")
    public Result<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO userDTO = userService.getUserById(id);
        return Result.success(userDTO);
    }

    /**
     * 根据用户名查询用户
     */
    @GetMapping("/username/{username}")
    public Result<UserDTO> getUserByUsername(@PathVariable String username) {
        UserDTO userDTO = userService.getUserByUsername(username);
        return Result.success(userDTO);
    }

    /**
     * 查询所有用户
     */
    @GetMapping("/list")
    public Result<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return Result.success(users);
    }

    /**
     * 分页查询用户列表
     */
    @GetMapping("/page")
    public Result<Page<UserDTO>> getUsersByPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<UserDTO> page = userService.getUsersByPage(pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @Valid @RequestBody UserDTO userDTO) {
        userDTO.setId(id);
        boolean success = userService.updateUser(userDTO);
        return success ? Result.success(null, "更新成功") 
                      : Result.failure("UPDATE_FAILED", "更新失败");
    }

    /**
     * 删除用户（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        boolean success = userService.deleteUser(id);
        return success ? Result.success(null, "删除成功") 
                      : Result.failure("DELETE_FAILED", "删除失败");
    }
}
