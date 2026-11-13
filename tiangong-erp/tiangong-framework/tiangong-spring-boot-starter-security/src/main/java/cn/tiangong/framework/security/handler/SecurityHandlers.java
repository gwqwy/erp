package cn.tiangong.framework.security.handler;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 401 / 403 处理：按《04-API规范》约定返回 HTTP 200 + 业务码（自有前端 axios 统一拦截）。
 */
public class SecurityHandlers {

    private SecurityHandlers() {
    }

    private static void write(HttpServletResponse response, int code, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream().write(
                new ObjectMapper().writeValueAsBytes(CommonResult.error(code, msg)));
        response.getOutputStream().flush();
    }

    /** 未登录 / Token 失效 */
    public static class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {
        @Override
        public void commence(HttpServletRequest request, HttpServletResponse response,
                             AuthenticationException ex) throws IOException {
            write(response, 401, "账号未登录");
        }
    }

    /** 已登录但无权限 */
    public static class AccessDeniedHandlerImpl implements AccessDeniedHandler {
        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response,
                           AccessDeniedException ex) throws IOException {
            write(response, 403, "没有该操作权限");
        }
    }
}
