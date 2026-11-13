package cn.tiangong.framework.security.config;

import cn.tiangong.framework.security.core.TokenService;
import cn.tiangong.framework.security.filter.TokenAuthenticationFilter;
import cn.tiangong.framework.security.handler.SecurityHandlers;
import cn.tiangong.framework.security.util.PermissionChecker;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 安全自动配置：无状态 JWT + 方法级权限。CORS 全放开由 Nginx 同源部署收口（企业内网自用）。
 */
@AutoConfiguration
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class TiangongSecurityAutoConfiguration {

    private static final String[] DEFAULT_PERMIT_URLS = {
            "/admin-api/system/auth/login",
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/doc.html", "/webjars/**",
            "/favicon.ico", "/error"
    };

    @Bean
    public TokenService tokenService(org.springframework.data.redis.core.StringRedisTemplate redisTemplate,
                                     ObjectMapper objectMapper,
                                     SecurityProperties properties) {
        return new TokenService(redisTemplate, objectMapper, properties);
    }

    @Bean("ss")
    public PermissionChecker permissionChecker() {
        return new PermissionChecker();
    }

    @Bean
    public cn.tiangong.framework.security.handler.SecurityExceptionHandler securityExceptionHandler() {
        return new cn.tiangong.framework.security.handler.SecurityExceptionHandler();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public TokenAuthenticationFilter tokenAuthenticationFilter(TokenService tokenService,
                                                               SecurityProperties properties) {
        return new TokenAuthenticationFilter(tokenService, properties);
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return new SecurityHandlers.AuthenticationEntryPointImpl();
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return new SecurityHandlers.AccessDeniedHandlerImpl();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   TokenAuthenticationFilter tokenFilter,
                                                   AuthenticationEntryPoint entryPoint,
                                                   AccessDeniedHandler deniedHandler,
                                                   SecurityProperties properties) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reg -> {
                    List<String> permits = new java.util.ArrayList<>(List.of(DEFAULT_PERMIT_URLS));
                    permits.addAll(properties.getPermitAllUrls());
                    reg.requestMatchers(permits.toArray(String[]::new)).permitAll()
                       .requestMatchers(HttpMethod.OPTIONS).permitAll()
                       .anyRequest().authenticated();
                })
                .exceptionHandling(eh -> eh.authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler))
                .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("*"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
