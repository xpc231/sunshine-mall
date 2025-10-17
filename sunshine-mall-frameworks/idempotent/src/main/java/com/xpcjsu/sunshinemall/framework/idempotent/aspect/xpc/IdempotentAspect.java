package com.xpcjsu.sunshinemall.framework.idempotent.aspect.xpc;


import com.xpcjsu.sunshinemall.framework.cache.core.CacheManager;
import com.xpcjsu.sunshinemall.framework.idempotent.annotation.Idempotent;
import com.xpcjsu.sunshinemall.framework.idempotent.exception.IdempotentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {

    private final CacheManager cacheManager;

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    private static final DefaultParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String idempotentKey = generateIdempotentKey(joinPoint, idempotent);

        long expireSeconds = idempotent.timeUnit().toSeconds(idempotent.expireTime());
        Boolean success = cacheManager.setIfAbsent(idempotentKey, '1', expireSeconds);

        if(!success) {
            log.warn("检测到重复提交 - 幂等键: {}", idempotentKey);
            throw new IdempotentException(idempotent.message());
        }

        try {
            log.debug("幂等检查通过 - 幂等键: {}, 过期时间: {}秒", idempotentKey, expireSeconds);
            return joinPoint.proceed();
        } catch (Throwable e) {
            cacheManager.delete(idempotentKey);
            log.warn("业务执行失败，已删除幂等键 - 幂等键: {}", idempotentKey);
            throw e;
        }
    }

    private String generateIdempotentKey(ProceedingJoinPoint joinPoint, Idempotent idempotent) {

        String keyValue = parseSpEL(joinPoint, idempotent.key());

        return idempotent.prefix() + ":" + keyValue;

    }

    private String parseSpEL(ProceedingJoinPoint joinPoint, String spel) {
        MethodSignature signature =(MethodSignature)joinPoint.getSignature();
        Method method = signature.getMethod();

        String[] parameterNames = NAME_DISCOVERER.getParameterNames(method);

        Object[] args = joinPoint.getArgs();

        EvaluationContext context = new StandardEvaluationContext();
        if(parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }

        Expression expression = PARSER.parseExpression(spel);
        Object value = expression.getValue(context);

        return value != null ? value.toString() : "";

    }
}
