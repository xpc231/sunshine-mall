package com.xpcjsu.sunshinemall.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xpcjsu.sunshinemall.framework.base.exception.BusinessException;

import com.xpcjsu.sunshinemall.framework.cache.core.CacheManager;
import com.xpcjsu.sunshinemall.framework.convention.errorcode.BusinessErrorCode;
import com.xpcjsu.sunshinemall.user.config.JwtProperties;
import com.xpcjsu.sunshinemall.user.dto.LoginRequest;
import com.xpcjsu.sunshinemall.user.dto.LoginResponse;
import com.xpcjsu.sunshinemall.user.dto.LoginRefreshResponse;
import com.xpcjsu.sunshinemall.user.dto.RefreshTokenRequest;
import com.xpcjsu.sunshinemall.user.dto.UserDTO;
import com.xpcjsu.sunshinemall.user.entity.User;
import com.xpcjsu.sunshinemall.user.mapper.UserMapper;
import com.xpcjsu.sunshinemall.user.service.UserService;
import com.xpcjsu.sunshinemall.user.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 *
 * @author sunshine-mall
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final CacheManager cacheManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtTool jwtTool;
    private final JwtProperties jwtProperties;

    private static final String USER_CACHE_PREFIX = "user:";
    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";
    private static final String REFRESH_USER_PREFIX = "refresh:user:";
    private static final long USER_CACHE_EXPIRE = 3600L; // 1小时

    // 登录
    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        // 查询用户（优化：链式调用）
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, loginRequest.getUsername())
        );

        if (user == null || !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_FOUND, "用户名或密码错误");
        }

        // 检查用户状态
        if (user.getStatus() == 0) {
            throw new BusinessException(BusinessErrorCode.USER_ACCESS_DENIED, "账号已被禁用");
        }

        String accessToken = jwtTool.createAccessToken(user.getId(), jwtProperties.getTokenTTL());
        String refreshToken = jwtTool.createRefreshToken(user.getId(), jwtProperties.getRefreshTTL());

        // 缓存用户信息
        cacheManager.set(getUserCacheKey(user.getId()), user, USER_CACHE_EXPIRE);
        cacheManager.set(getRefreshUserKey(user.getId()), refreshToken, jwtProperties.getRefreshTTL().toSeconds());

        log.info("用户登录成功 - userId: {}, username: {}", user.getId(), user.getUsername());

        LoginResponse resp = new LoginResponse();
        resp.setToken(accessToken);
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setRealName(user.getRealName());
        resp.setRefreshToken(refreshToken);
        return resp;
    }

    // 登出
    @Override
    public void logout(String token) {
        cacheManager.set(getTokenBlacklistKey(token), "1", jwtProperties.getTokenTTL().toSeconds());
        try {
            Long userId = jwtTool.parseAccessToken(token);
            cacheManager.delete(getRefreshUserKey(userId));
        } catch (Exception ignored) {}
        log.info("登出：加入访问令牌黑名单并清除刷新令牌");
    }

    // 创建用户
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserDTO userDTO) {
        // 检查用户名是否已存在（优化：exists 替代 selectCount）
        if (userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getUsername, userDTO.getUsername()))) {
            throw new BusinessException("USER_ALREADY_EXISTS", "用户名已存在");
        }

        // 检查手机号是否已存在
        if (StringUtils.hasText(userDTO.getPhone()) &&
            userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getPhone, userDTO.getPhone()))) {
            throw new BusinessException("PHONE_ALREADY_EXISTS", "手机号已被注册");
        }

        // 创建用户
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        
        // 加密密码（使用 BCrypt）
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        
        // 设置默认值
        if (user.getStatus() == null) {
            user.setStatus(1); // 默认正常状态
        }
        if (user.getGender() == null) {
            user.setGender(0); // 默认女性
        }

        userMapper.insert(user);
        log.info("创建用户成功 - userId: {}, username: {}", user.getId(), user.getUsername());

        return user.getId();
    }

    // 根据ID查询用户
    @Override
    public UserDTO getUserById(Long id) {
        // 先从缓存获取
        String cacheKey = getUserCacheKey(id);
        User cachedUser = cacheManager.get(cacheKey, User.class);
        
        if (cachedUser != null) {
            return convertToDTO(cachedUser);
        }

        // 从数据库查询
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_FOUND, "用户不存在");
        }

        // 缓存用户信息
        cacheManager.set(cacheKey, user, USER_CACHE_EXPIRE);

        return convertToDTO(user);
    }

    // 根据用户名查询用户
    @Override
    public UserDTO getUserByUsername(String username) {
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );

        if (user == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_FOUND, "用户不存在");
        }

        return convertToDTO(user);
    }

    // 获取所有用户
    @Override
    public List<UserDTO> getAllUsers() {
        // 限制最多返回 1000 条，防止内存溢出
        Page<User> page = new Page<>(1, 1000);
        List<User> users = userMapper.selectPage(page, null).getRecords();
        return users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 分页查询用户
    @Override
    public Page<UserDTO> getUsersByPage(int pageNum, int pageSize) {

        Page<User> page = new Page<>(pageNum, pageSize);
        Page<User> userPage = userMapper.selectPage(page, null);

        // 转换为 DTO 分页结果
        Page<UserDTO> dtoPage = new Page<>(pageNum, pageSize, userPage.getTotal());
        List<UserDTO> dtoList = userPage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);

        return dtoPage;
    }

    // 更新用户
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(UserDTO userDTO) {
        if (userDTO.getId() == null) {
            throw new BusinessException(BusinessErrorCode.SYSTEM_PARAM_ERROR, "用户ID不能为空");
        }

        User existingUser = userMapper.selectById(userDTO.getId());
        if (existingUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_FOUND, "用户不存在");
        }

        User user = new User();
        BeanUtils.copyProperties(userDTO, user);

        // 如果修改了密码，需要加密（使用 BCrypt）
        if (StringUtils.hasText(userDTO.getPassword())) {
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        } else {
            user.setPassword(null); // 不更新密码
        }

        int rows = userMapper.updateById(user);
        if (rows == 0) {
            return false;
        }

        // 清除缓存
        cacheManager.delete(getUserCacheKey(user.getId()));
        log.info("更新用户成功 - userId: {}", user.getId());
        return true;
    }

    // 删除用户
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_FOUND, "用户不存在");
        }

        int rows = userMapper.deleteById(id);
        if (rows == 0) {
            return false;
        }

        // 清除缓存
        cacheManager.delete(getUserCacheKey(id));
        log.info("删除用户成功 - userId: {}", id);
        return true;
    }


    // 刷新令牌
    @Override
    public LoginRefreshResponse refreshToken(RefreshTokenRequest request) {
        Long userId = jwtTool.parseRefreshToken(request.getRefreshToken());
        Object rtInRedis = cacheManager.get(getRefreshUserKey(userId));
        if (rtInRedis == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGIN, "未登录或刷新令牌已失效");
        }
        if (!request.getRefreshToken().equals(rtInRedis.toString())) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGIN, "刷新令牌不匹配");
        }
        String newAccess = jwtTool.createAccessToken(userId, jwtProperties.getTokenTTL());
        return new LoginRefreshResponse(newAccess);
    }



    //---- 私有方法 --------------------------------------------------------------------------------------------------------


    /**
     * 生成用户缓存键
     */
    private String getUserCacheKey(Long userId) {
        return USER_CACHE_PREFIX + userId;
    }

    /**
     * 生成 Token 黑名单键
     */
    private String getTokenBlacklistKey(String token) {
        return TOKEN_BLACKLIST_PREFIX + token;
    }

    /**
     * 获取用户刷新令牌缓存键
     */
    private String getRefreshUserKey(Long userId) {
        return REFRESH_USER_PREFIX + userId;
    }


    /**
     * 实体转 DTO
     */
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        // 不返回密码
        dto.setPassword(null);
        return dto;
    }


}
