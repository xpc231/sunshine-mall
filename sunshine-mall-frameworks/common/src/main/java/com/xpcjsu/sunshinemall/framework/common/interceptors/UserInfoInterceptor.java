package com.xpcjsu.sunshinemall.framework.common.interceptors;

import cn.hutool.core.util.StrUtil;
import com.xpcjsu.sunshinemall.framework.common.util.UserContext;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;  // 修改为jakarta.servlet
import jakarta.servlet.http.HttpServletResponse; // 修改为jakarta.servlet

public class UserInfoInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {

        // 1.获取登录用户信息
        String userInfo = request.getHeader("user-info");
        // 2.判断是否获取了用户，如果有，存入ThreadLocal
        if (StrUtil.isNotBlank(userInfo)) {
            UserContext.setUser(Long.valueOf(userInfo));
        }
        // 3.放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {

        // 清理用户
        UserContext.removeUser();
    }
}
