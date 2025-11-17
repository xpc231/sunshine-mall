package com.xpcjsu.sunshinemall.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xpcjsu.sunshinemall.framework.cache.core.CacheManager;
import com.xpcjsu.sunshinemall.user.entity.UserSignIn;
import com.xpcjsu.sunshinemall.user.mapper.UserSignInMapper;
import com.xpcjsu.sunshinemall.user.service.UserSignInService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class UserSignInServiceImpl implements UserSignInService {
    private final UserSignInMapper mapper;
    private final CacheManager cacheManager;

    private static final String STREAK_KEY_PREFIX = "sign:streak:";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int signIn(Long userId) {
        LocalDate today = LocalDate.now();
        UserSignIn exist = mapper.selectOne(new LambdaQueryWrapper<UserSignIn>().eq(UserSignIn::getUserId, userId).eq(UserSignIn::getSignDate, today));
        if (exist != null) {
            Integer s = cacheManager.get(STREAK_KEY_PREFIX + userId, Integer.class);
            return s == null ? 1 : s;
        }
        LocalDate yesterday = today.minusDays(1);
        UserSignIn y = mapper.selectOne(new LambdaQueryWrapper<UserSignIn>().eq(UserSignIn::getUserId, userId).eq(UserSignIn::getSignDate, yesterday));
        int streak = y == null ? 1 : ((cacheManager.get(STREAK_KEY_PREFIX + userId, Integer.class) == null ? 1 : cacheManager.get(STREAK_KEY_PREFIX + userId, Integer.class)) + 1);
        UserSignIn rec = new UserSignIn();
        rec.setUserId(userId);
        rec.setSignDate(today);
        rec.setContinueDays(streak);
        rec.setRewardPoints(1);
        mapper.insert(rec);
        cacheManager.set(STREAK_KEY_PREFIX + userId, streak, 86400L * 30);
        return streak;
    }

    @Override
    public int getStreak(Long userId) {
        Integer s = cacheManager.get(STREAK_KEY_PREFIX + userId, Integer.class);
        if (s != null) return s;
        UserSignIn last = mapper.selectOne(new LambdaQueryWrapper<UserSignIn>().eq(UserSignIn::getUserId, userId).orderByDesc(UserSignIn::getSignDate).last("limit 1"));
        int v = last == null ? 0 : last.getContinueDays();
        cacheManager.set(STREAK_KEY_PREFIX + userId, v, 86400L * 30);
        return v;
    }
}