package cn.tiangong.module.system.framework;

import cn.tiangong.framework.security.util.SecurityFrameworkUtils;
import cn.tiangong.module.system.dal.dataobject.SystemOperLogDO;
import cn.tiangong.module.system.dal.mysql.SystemOperLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

/**
 * 操作日志切面：自动记录所有 /admin-api 的写请求（POST/PUT/DELETE）。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperLogAspect {

    private final SystemOperLogMapper operLogMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("execution(* cn.tiangong..controller.admin..*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }
        HttpServletRequest request = attributes.getRequest();
        String method = request.getMethod();
        boolean isWrite = "POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method);
        long start = System.currentTimeMillis();
        Throwable error = null;
        try {
            return joinPoint.proceed();
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            if (isWrite) {
                saveLog(joinPoint, request, method, System.currentTimeMillis() - start, error);
            }
        }
    }

    private void saveLog(ProceedingJoinPoint joinPoint, HttpServletRequest request,
                         String method, long costMs, Throwable error) {
        try {
            SystemOperLogDO logDO = new SystemOperLogDO();
            logDO.setUserId(SecurityFrameworkUtils.getLoginUserId());
            var loginUser = cn.tiangong.framework.security.util.SecurityFrameworkUtils.getLoginUser();
            logDO.setUsername(loginUser != null ? loginUser.getUsername() : "");
            logDO.setUrl(truncate(request.getRequestURI(), 255));
            logDO.setHttpMethod(method);
            logDO.setParamsText(truncate(safeJson(joinPoint.getArgs()), 2000));
            logDO.setCostMs(costMs);
            logDO.setSuccess(error == null);
            logDO.setErrorMsg(error == null ? null : truncate(error.getMessage(), 500));
            operLogMapper.insert(logDO);
        } catch (Exception e) {
            log.warn("[saveLog] 操作日志记录失败: {}", e.getMessage());
        }
    }

    private String safeJson(Object[] args) {
        try {
            return objectMapper.writeValueAsString(Arrays.stream(args)
                    .filter(a -> !(a instanceof jakarta.servlet.ServletRequest)
                            && !(a instanceof jakarta.servlet.ServletResponse)).toArray());
        } catch (Exception e) {
            return "unserializable";
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
