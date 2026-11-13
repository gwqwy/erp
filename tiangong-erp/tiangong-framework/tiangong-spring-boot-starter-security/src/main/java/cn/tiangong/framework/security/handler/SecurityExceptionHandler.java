package cn.tiangong.framework.security.handler;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Spring Security 相关 MVC 异常：优先级高于 web starter 的兜底处理器（按类型精确匹配）。
 * 401 场景主要由过滤器链的 AuthenticationEntryPoint 覆盖，此处兜方法层抛出的异常。
 */
@Slf4j
@RestControllerAdvice
public class SecurityExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public CommonResult<Void> authenticationExceptionHandler(AuthenticationException ex) {
        return CommonResult.error(401, "账号未登录");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public CommonResult<Void> accessDeniedExceptionHandler(AccessDeniedException ex) {
        log.warn("[accessDeniedExceptionHandler] 拒绝访问: {}", ex.getMessage());
        return CommonResult.error(403, "没有该操作权限");
    }
}
