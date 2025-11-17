package com.xpcjsu.sunshinemall.user.controller;

import com.xpcjsu.sunshinemall.framework.common.util.UserContext;
import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import com.xpcjsu.sunshinemall.user.service.UserSignInService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/sign-in")
@RequiredArgsConstructor
public class UserSignInController {
    private final UserSignInService service;

    @PostMapping
    public Result<Integer> signIn() {
        Long userId = UserContext.getUser();
        int streak = service.signIn(userId);
        return Result.success(streak);
    }

    @GetMapping("/stats")
    public Result<Integer> stats() {
        Long userId = UserContext.getUser();
        return Result.success(service.getStreak(userId));
    }
}