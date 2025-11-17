package com.xpcjsu.sunshinemall.user.controller;

import com.xpcjsu.sunshinemall.framework.common.util.UserContext;
import com.xpcjsu.sunshinemall.framework.convention.errorcode.BusinessErrorCode;
import com.xpcjsu.sunshinemall.framework.convention.result.Result;
import com.xpcjsu.sunshinemall.user.service.UserSignInService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

// 用户签到
@RestController
@RequestMapping("/api/user/sign-in")
public class UserSignInController {

    private final UserSignInService userSignInService;

    public UserSignInController(UserSignInService userSignInService) {
        this.userSignInService = userSignInService;
    }

    // 签到
    @PostMapping
    public Result<Void> sign() {
        Long userId = UserContext.getUser();
        if (userId == null) {
            return Result.failure(BusinessErrorCode.USER_NOT_LOGIN, "未登录");
        }
        boolean ok = userSignInService.signToday(userId);
        if (!ok) {
            return Result.failure(BusinessErrorCode.SYSTEM_PARAM_ERROR, "今日已签到");
        }
        return Result.success(null, "签到成功");
    }

    // 查询签到状态
    @GetMapping("/status")
    public Result<Boolean> status() {
        Long userId = UserContext.getUser();
        if (userId == null) {
            return Result.failure(BusinessErrorCode.USER_NOT_LOGIN, "未登录");
        }
        return Result.success(userSignInService.isSignedToday(userId));
    }

    // 查询签到次数
    @GetMapping("/count")
    public Result<Integer> count(@RequestParam(value = "month", required = false) String month) {
        Long userId = UserContext.getUser();
        if (userId == null) {
            return Result.failure(BusinessErrorCode.USER_NOT_LOGIN, "未登录");
        }
        // 解析年月字符串，如果为空则使用当前年月
        YearMonth ym = month == null || month.isEmpty()
                ? YearMonth.now()
                : YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyyMM"));
        return Result.success(userSignInService.countMonth(userId, ym));
    }
}