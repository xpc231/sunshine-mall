package com.xpcjsu.sunshinemall.framework.common.interceptors;

import cn.hutool.core.util.StrUtil;
import com.xpcjsu.sunshinemall.framework.common.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;  // 修改为jakarta.servlet
import jakarta.servlet.http.HttpServletResponse; // 修改为jakarta.servlet
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
public class UserInfoInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {

        // 1.优先从网关透传的 header 获取用户ID
        String userInfo = request.getHeader("user-info");
        // 2.兼容 X-User-Id 头（有些网关或文档示例使用该命名）
        String xUserId = request.getHeader("X-User-Id");

        String candidate = StrUtil.isNotBlank(userInfo) ? userInfo : xUserId;
        if (StrUtil.isNotBlank(candidate)) {
            try {
                Long uid = Long.valueOf(candidate.trim());//去除字符串两端空白字符
                UserContext.setUser(uid);
                log.debug("拦截器注入用户ID成功: {}", uid);
            } catch (NumberFormatException e) {
                log.warn("用户ID头格式错误，忽略。user-info={}, X-User-Id={}", userInfo, xUserId);
            }
        } else {
            log.debug("未获取到用户ID头，可能是未登录或绕过网关。path={}", request.getRequestURI());
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
