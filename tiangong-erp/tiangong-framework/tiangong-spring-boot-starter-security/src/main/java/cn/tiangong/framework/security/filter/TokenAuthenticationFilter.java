package cn.tiangong.framework.security.filter;

import cn.tiangong.framework.security.config.SecurityProperties;
import cn.tiangong.framework.security.core.LoginUser;
import cn.tiangong.framework.security.core.TokenService;
import cn.tiangong.framework.security.util.SecurityFrameworkUtils;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Token 认证过滤器：从请求头解析 JWT -> 还原 Redis 会话 -> 写入 SecurityContext。
 * 无 Token / Token 无效时静默放行，由授权规则决定 401。
 */
@Slf4j
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final SecurityProperties properties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = SecurityFrameworkUtils.obtainToken(request, properties.getTokenHeader());
        if (token != null) {
            try {
                LoginUser loginUser = tokenService.checkToken(token);
                if (loginUser != null) {
                    List<SimpleGrantedAuthority> authorities = loginUser.getPermissions().stream()
                            .map(SimpleGrantedAuthority::new).toList();
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(loginUser, token, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException | IllegalArgumentException e) {
                log.debug("[doFilterInternal] Token 无效: {}", e.getMessage());
            }
        }
        chain.doFilter(request, response);
    }
}
