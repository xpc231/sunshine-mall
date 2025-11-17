package com.xpcjsu.sunshinemall.user.service;

import java.time.YearMonth;

public interface UserSignInService {
    
    boolean signToday(Long userId);
    
    boolean isSignedToday(Long userId);
    
    int countMonth(Long userId, YearMonth month);
}