package com.xpcjsu.sunshinemall.user.controller;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import com.xpcjsu.sunshinemall.framework.cache.core.CacheManager;
import com.xpcjsu.sunshinemall.user.config.JwtProperties;
import com.xpcjsu.sunshinemall.user.dto.LoginResponse;
import com.xpcjsu.sunshinemall.user.dto.SmsSendRequest;
import com.xpcjsu.sunshinemall.user.dto.SmsVerifyRequest;
import com.xpcjsu.sunshinemall.user.entity.User;
import com.xpcjsu.sunshinemall.user.mapper.UserMapper;
import com.xpcjsu.sunshinemall.user.utils.JwtTool;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/user/login/sms")
@RequiredArgsConstructor
public class SmsLoginController {
    private final CacheManager cacheManager;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTool jwtTool;
    private final JwtProperties jwtProperties;

    private static final String SMS_CODE_PREFIX = "login:sms:";
    private static final long CODE_TTL_SECONDS = 300L;

    @PostMapping("/send")
    public Result<Void> send(@Valid @RequestBody SmsSendRequest request) {
        String limitKey = SMS_CODE_PREFIX + "limit:" + request.getPhone();
        if (!cacheManager.setIfAbsent(limitKey, "1", 60)) {
            return Result.failure("RATE_LIMIT", "请求过于频繁");
        }
        String code = RandomUtil.randomNumbers(6);
        cacheManager.set(SMS_CODE_PREFIX + request.getPhone(), code, CODE_TTL_SECONDS);
        log.info("短信验证码:{} 收件人:{}", code, request.getPhone());
        return Result.success(null, "验证码已发送");
    }

    @PostMapping("/verify")
    public Result<LoginResponse> verify(@Valid @RequestBody SmsVerifyRequest request) {
        String key = SMS_CODE_PREFIX + request.getPhone();
        String cached = cacheManager.get(key, String.class);
        if (cached == null || !cached.equals(request.getCode())) {
            return Result.failure("SMS_CODE_INVALID", "验证码错误或过期");
        }
        cacheManager.delete(key);
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone()));
        if (user == null) {
            user = new User();
            user.setUsername("u_" + request.getPhone());
            user.setPassword(passwordEncoder.encode(RandomUtil.randomString(16)));
            user.setPhone(request.getPhone());
            user.setStatus(1);
            userMapper.insert(user);
        }
        String token = jwtTool.createToken(user.getId(), jwtProperties.getTokenTTL());
        String refreshToken = jwtTool.createToken(user.getId(), java.time.Duration.ofDays(7));
        cacheManager.set("refresh:" + refreshToken, String.valueOf(user.getId()), java.time.Duration.ofDays(7).toSeconds());
        return Result.success(new LoginResponse(token, refreshToken, user.getId(), user.getUsername(), user.getRealName()), "登录成功");
    }
}