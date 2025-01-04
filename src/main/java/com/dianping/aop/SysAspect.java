package com.dianping.aop;

import com.dianping.annotation.Log;
import com.dianping.dao.SysLogDao;
import com.dianping.entity.SysLog;
import com.dianping.utils.UserHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Component
@Aspect
public class SysAspect {

    @Resource
    private SysLogDao sysLogDao;

    // 定义切点，指定切点为Log注解
    @Pointcut("@annotation(com.dianping.annotation.Log)")
    private void pointcut() {}

    // 定义环绕在切点前后的操作
    @Around("pointcut()")
    public Object doAround(ProceedingJoinPoint point) {
        Object result = null;
        // 记录起始时间
        long start = System.currentTimeMillis();

        try {
            // 调用方法，完成具体逻辑
            result = point.proceed();
        } catch (Throwable e) {
            e.printStackTrace();
        }

        // 计算方法执行时间差
        long time = System.currentTimeMillis() - start;
        // 保存操作日志
        saveLog(point, time);

        return result;
    }

    private void saveLog(ProceedingJoinPoint point, long time) {
        SysLog sysLog = new SysLog();
        // 设置用户名
        Long userId = UserHolder.getUser().getId();
        sysLog.setUsername(userId.toString());
        // 从切点获取方法签名
        MethodSignature signature = (MethodSignature) point.getSignature();
        // 获取方法
        Method method = signature.getMethod();
        // 获取日志注解
        Log annotation = method.getAnnotation(Log.class);

        // 获取日志注解上的描述，并且设置日志行为描述
        if (annotation != null) {
            sysLog.setOperation(annotation.name());
        }

        // 获取类名
        String className = point.getTarget().getClass().getName();
        // 获取方法名
        String methodName = method.getName();
        // 设置方法全名
        sysLog.setMethod(className + "." + methodName + "()");

        // 获取参数
        Object[] args = point.getArgs();
        // 读取参数名
        LocalVariableTableParameterNameDiscoverer l = new LocalVariableTableParameterNameDiscoverer();
        String[] parameterNames = l.getParameterNames(method);
        // 设置参数
        if (args != null && parameterNames != null) {
            StringBuilder params = new StringBuilder();
            for (int i = 0; i < parameterNames.length; i++) {
                params.append(parameterNames[i]);
                params.append(":");
                params.append(args[i].toString());
                params.append(",");
            }
            sysLog.setParams(params.toString());
        }
        // 也可以获取Request对象
//        RequestAttributes reqa = RequestContextHolder.getRequestAttributes();
//        ServletRequestAttributes sra = (ServletRequestAttributes) reqa;
//        HttpServletRequest request = sra.getRequest();
//        // 访问url
//        String url = request.getRequestURI();
//        // 请求方式
//        String reqaMethodName = request.getMethod();
//        // 登入IP
//        getIpaddr(request)
        // 设置时间
        sysLog.setTime((int) time);
        sysLog.setCreateTime(LocalDateTime.now());

        sysLogDao.add(sysLog);
    }
}
