package work.licht.music.log.aspect;

import com.google.gson.Gson;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

@Aspect
@Slf4j
public class ApiOperationLogAspect {
    @Resource private Gson gson;

    // 以自定义 @ApiOperationLog 注解为切点，凡是添加 @ApiOperationLog 的方法，都会执行环绕中的代码
    @Pointcut("@annotation(work.licht.music.log.aspect.ApiOperationLog)")
    public void apiOperationLog() {}

    @Around("apiOperationLog()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        // 请求开始时间
        long startTime = System.currentTimeMillis();
        // 获取被请求的类和方法
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        // 请求入参
        Object[] args = joinPoint.getArgs();
        // 入参转 JSON 字符串
        String argsJsonStr = gson.toJson(args);
        // 功能描述信息
        String description = getApiOperationLogDescription(joinPoint);
        // 打印请求相关参数
        log.info("== 请求开始: [{}], 入参: {}, 请求类: {}, 请求方法: {} ==", description, argsJsonStr, className, methodName);
        // 执行切点方法
        Object result = joinPoint.proceed();
        // 执行耗时
        long executionTime = System.currentTimeMillis() - startTime;
        // 返回值转 JSON 字符串
        String resultJsonStr = gson.toJson(result);
        // 打印出参等相关信息
        log.info("== 请求结束: [{}], 耗时: {}ms, 出参: {} ==", description, executionTime, resultJsonStr);
        return result;
    }

    // 获取注解的描述信息
    private String getApiOperationLogDescription(ProceedingJoinPoint joinPoint) {
        // 从 ProceedingJoinPoint 获取 MethodSignature
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        // 使用 MethodSignature 获取被注解的 Method
        Method method = signature.getMethod();
        // 从 Method 中提取 LogExecution 注解
        ApiOperationLog apiOperationLog = method.getAnnotation(ApiOperationLog.class);
        // 从 LogExecution 注解中获取 description 属性
        return apiOperationLog.description();
    }

}