package com.xpcjsu.sunshinemall.user.controller;

import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import com.xpcjsu.sunshinemall.framework.cache.core.CacheManager;
import com.xpcjsu.sunshinemall.user.config.JwtProperties;
import com.xpcjsu.sunshinemall.user.dto.LoginResponse;
import com.xpcjsu.sunshinemall.user.utils.JwtTool;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/user/token")
@RequiredArgsConstructor
public class TokenController {
    private final CacheManager cacheManager;
    private final JwtTool jwtTool;
    private final JwtProperties jwtProperties;

    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@RequestBody Map<String, String> req) {
        String refreshToken = req.get("refreshToken");
        if (refreshToken == null || refreshToken.isEmpty()) {
            return Result.failure("PARAM_ERROR", "缺少refreshToken");
        }
        Long userId;
        try {
            userId = jwtTool.parseToken(refreshToken);
        } catch (Exception e) {
            return Result.failure("REFRESH_INVALID", "refreshToken无效或过期");
        }
        String v = cacheManager.get("refresh:" + refreshToken, String.class);
        if (v == null || !v.equals(String.valueOf(userId))) {
            return Result.failure("REFRESH_INVALID", "refreshToken无效或过期");
        }
        cacheManager.delete("refresh:" + refreshToken);
        String token = jwtTool.createToken(userId, jwtProperties.getTokenTTL());
        String newRt = jwtTool.createToken(userId, Duration.ofDays(7));
        cacheManager.set("refresh:" + newRt, String.valueOf(userId), Duration.ofDays(7).toSeconds());
        return Result.success(new LoginResponse(token, newRt, userId, null, null));
    }
}