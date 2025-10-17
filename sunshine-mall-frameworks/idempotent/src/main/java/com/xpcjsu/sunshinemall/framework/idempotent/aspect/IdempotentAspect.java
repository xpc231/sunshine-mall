package com.xpcjsu.sunshinemall.framework.idempotent.aspect;

import com.xpcjsu.sunshinemall.framework.cache.core.CacheManager;
import com.xpcjsu.sunshinemall.framework.idempotent.annotation.Idempotent;
import com.xpcjsu.sunshinemall.framework.idempotent.exception.IdempotentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
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


/**
 * 幂等性切面
 * <p>
 * 基于Redis实现接口幂等性控制，防止重复提交。
 * <p>
 * 工作原理：
 * <ul>
 * <li>1. 拦截带有@Idempotent注解的方法</li>
 * <li>2. 根据注解配置生成唯一的幂等键</li>
 * <li>3. 尝试在Redis中设置幂等键（NX模式）</li>
 * <li>4. 如果设置成功，执行业务方法并返回</li>
 * <li>5. 如果设置失败，说明重复提交，抛出IdempotentException</li>
 * <li>6. 业务执行完成后，幂等键在过期时间后自动删除</li>
 * </ul>
 * 
 * @author sunshine-mall
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {

    private final CacheManager cacheManager;

    /**
     * Spring Expression Language (SpEL)表达式解析器
     */
    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    /**
     * 参数名发现器
     * 获取方法参数的真实名称而非编译后的变量名
     */
    private static final DefaultParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    /**
     * 幂等性切面环绕通知
     * 
     * @param joinPoint 切点
     * @param idempotent 幂等注解
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(idempotent)")//拦截带有@Idempotent注解的方法
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        // 1. 生成幂等键
        String idempotentKey = generateIdempotentKey(joinPoint, idempotent);
        
        // 2. 尝试设置幂等键（NX模式 - 不存在才设置）
        long expireSeconds = idempotent.timeUnit().toSeconds(idempotent.expireTime());
        //过期时间和单位在使用该注解时配置
        boolean success = cacheManager.setIfAbsent(idempotentKey, "1", expireSeconds);
        
        if (!success) {
            // 幂等键已存在，说明重复提交
            log.warn("检测到重复提交 - 幂等键: {}", idempotentKey);
            throw new IdempotentException(idempotent.message());
        }
        
        try {
            // 3. 执行业务方法
            log.debug("幂等检查通过 - 幂等键: {}, 过期时间: {}秒", idempotentKey, expireSeconds);
            return joinPoint.proceed();
        } catch (Throwable e) {
            // 4. 业务执行失败，删除幂等键，允许重试
            cacheManager.delete(idempotentKey);
            log.warn("业务执行失败，已删除幂等键 - 幂等键: {}", idempotentKey);
            //业务错误让业务层决定如何处理异常
            throw e;
        }
    }

    /**
     * 生成幂等键
     * <p>
     * 格式：prefix:key值
     * 
     * @param joinPoint 切点
     * @param idempotent 幂等注解
     * @return 幂等键
     */
    private String generateIdempotentKey(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
        // 解析SpEL表达式获取key值
        String keyValue = parseSpEL(joinPoint, idempotent.key());
        
        // 获取@Idempotent注解中配置的前缀值,组装完整的幂等键
        return idempotent.prefix() + ":" + keyValue;
    }

    /**
     * 解析SpEL表达式
     * 
     * @param joinPoint 切点
     * @param spel SpEL表达式
     * @return 解析结果
     */
    private String parseSpEL(ProceedingJoinPoint joinPoint, String spel) {
        //实际运行时调用的是框架提供的具体实现类的方法
        // 获取方法签名，Signature中没有getMethod()方法，访问方法详细信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // 获取方法参数名
        String[] parameterNames = NAME_DISCOVERER.getParameterNames(method);
        
        // 获取方法实际参数值
        Object[] args = joinPoint.getArgs();
        
        // 创建SpEL上下文
        //表达式求值时的上下文环境,提供了完整的表达式解析上下文支持
        EvaluationContext context = new StandardEvaluationContext();
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                //注册到上下文中，键值对
                context.setVariable(parameterNames[i], args[i]);
            }
        }
        
        // 解析表达式
        Expression expression = PARSER.parseExpression(spel);
        Object value = expression.getValue(context);
        
        return value != null ? value.toString() : "";
        /*
        上下文环境已设置：#orderId -> 12345L, #userId -> "user001"
        处理：在 StandardEvaluationContext 上下文中计算表达式 "#orderId" 的值
        结果：value = 12345L
        */
    }
}
