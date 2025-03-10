package com.ywf.mianshiya.aop;

import com.jd.platform.hotkey.client.callback.JdHotKeyStore;
import com.ywf.mianshiya.annotation.HotKey;
import com.ywf.mianshiya.common.BaseResponse;
import com.ywf.mianshiya.common.ResultUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

//@Aspect
//@Component
public class HotKeyInterceptor {

    private final SpelExpressionParser spelExpressionParser = new SpelExpressionParser();

    @Around("@annotation(hotKey)")
    public Object doInterceptor(ProceedingJoinPoint proceedingJoinPoint, HotKey hotKey) throws Throwable {
        MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = proceedingJoinPoint.getArgs();

        // 解析 SpEL 表达式获取 key
        String key = resolveKey(hotKey, method, args);
        if (JdHotKeyStore.isHotKey(key)) {
            Object value = JdHotKeyStore.get(key);
            if (value != null) {
                return ResultUtils.success(value);
            }
        }

        // 执行原方法
        Object result = proceedingJoinPoint.proceed();

        // 缓存结果
        JdHotKeyStore.smartSet(key, ((BaseResponse<?>) result).getData());
        return result;
    }

    private String resolveKey(HotKey hotKey, Method method, Object[] args) {
        // 创建 SpEL 解析上下文
        EvaluationContext context = new StandardEvaluationContext();
        String[] paramNames = Arrays.stream(method.getParameters()).map(Parameter::getName).toArray(String[]::new);
        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }

        // 解析表达式
        Expression expression = spelExpressionParser.parseExpression(hotKey.id());
        Object idValue = expression.getValue(context);
        return hotKey.prefix() + "_" + idValue;
    }
}
