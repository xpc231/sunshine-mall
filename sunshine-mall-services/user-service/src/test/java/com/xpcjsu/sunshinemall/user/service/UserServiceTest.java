package com.xpcjsu.sunshinemall.user.service;

import com.xpcjsu.sunshinemall.user.dto.LoginRequest;
import com.xpcjsu.sunshinemall.user.dto.LoginResponse;
import com.xpcjsu.sunshinemall.user.dto.UserDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户服务测试类
 *
 * @author sunshine-mall
 * @since 1.0.0
 */
@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Test
    void testLogin() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("123456");

        LoginResponse response = userService.login(request);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("admin", response.getUsername());
    }

    @Test
    void testCreateUser() {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("testuser_" + System.currentTimeMillis());
        userDTO.setPassword("123456");
        userDTO.setNickname("测试用户");
        userDTO.setPhone("13900000000");
        userDTO.setEmail("test@test.com");

        Long userId = userService.createUser(userDTO);

        assertNotNull(userId);
        assertTrue(userId > 0);
    }

    @Test
    void testGetUserById() {
        UserDTO userDTO = userService.getUserById(1L);

        assertNotNull(userDTO);
        assertEquals("admin", userDTO.getUsername());
        assertNull(userDTO.getPassword()); // 密码不应该返回
    }
}
