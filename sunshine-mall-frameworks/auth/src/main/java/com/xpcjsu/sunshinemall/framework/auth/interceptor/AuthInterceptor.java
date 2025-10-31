package com.xpcjsu.sunshinemall.framework.auth.interceptor;

import com.xpcjsu.sunshinemall.framework.auth.annotation.RequireAuth;
import com.xpcjsu.sunshinemall.framework.auth.core.AuthContext;
import com.xpcjsu.sunshinemall.framework.auth.core.AuthService;
import com.xpcjsu.sunshinemall.framework.base.exception.BusinessException;
import com.xpcjsu.sunshinemall.framework.convention.errorcode.BusinessErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证拦截器
 *
 * @author sunshine-mall
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 只处理Controller方法
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        
        // 检查方法上的@RequireAuth注解
        RequireAuth methodAuth = handlerMethod.getMethodAnnotation(RequireAuth.class);
        
        // 检查类上的@RequireAuth注解
        RequireAuth classAuth = handlerMethod.getBeanType().getAnnotation(RequireAuth.class);
        
        // 方法注解优先级高于类注解
        RequireAuth requireAuth = methodAuth != null ? methodAuth : classAuth;
        
        if (requireAuth == null) {
            return true; // 不需要认证
        }

        try {
            // 提取认证信息
            AuthContext authContext = authService.extractAuthContext(request);
            
            // 设置到当前线程上下文
            AuthContext.setContext(authContext);
            
            log.debug("认证成功 - userId: {}, username: {}", authContext.getUserId(), authContext.getUsername());
            return true;
            
        } catch (BusinessException e) {
            if (requireAuth.required()) {
                // 必须认证但认证失败
                log.warn("认证失败: {}", e.getMessage());
                throw new BusinessException(BusinessErrorCode.USER_NOT_LOGIN, requireAuth.message());
            } else {
                // 可选认证，认证失败时继续执行
                log.debug("可选认证失败，继续执行: {}", e.getMessage());
                return true;
            }
        } catch (Exception e) {
            log.error("认证过程发生异常", e);
            if (requireAuth.required()) {
                throw new BusinessException(BusinessErrorCode.USER_NOT_LOGIN, "认证失败");
            }
            return true;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清除线程上下文
        AuthContext.clearContext();
    }
}