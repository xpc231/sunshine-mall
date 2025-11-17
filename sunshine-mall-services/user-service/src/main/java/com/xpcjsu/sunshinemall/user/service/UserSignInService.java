package com.xpcjsu.sunshinemall.user.service;

public interface UserSignInService {
    int signIn(Long userId);
    int getStreak(Long userId);
}