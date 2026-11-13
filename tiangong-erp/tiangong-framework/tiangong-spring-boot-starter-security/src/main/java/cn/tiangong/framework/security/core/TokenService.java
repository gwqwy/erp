package cn.tiangong.framework.security.core;

import cn.tiangong.framework.security.config.SecurityProperties;
import cn.tiangong.framework.security.util.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.UUID;

/**
 * Token 服务：JWT 携带 tokenUuid，完整 LoginUser 会话存 Redis，TTL 与 JWT 过期一致。
 * 登出 = 删除 Redis 会话；修改权限后重新登录即可刷新权限集。
 */
@Slf4j
@RequiredArgsConstructor
public class TokenService {

    private static final String KEY_PREFIX = "tiangong:login_token:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SecurityProperties properties;

    /** 创建 Token 并写入 Redis 会话 */
    @SneakyThrows
    public String createToken(LoginUser loginUser) {
        String tokenUuid = UUID.randomUUID().toString().replace("-", "");
        String token = JwtUtils.generate(loginUser.getId(), tokenUuid,
                properties.getJwtSecret(), properties.getTokenExpire());
        redisTemplate.opsForValue().set(KEY_PREFIX + tokenUuid,
                objectMapper.writeValueAsString(loginUser), properties.getTokenExpire());
        return token;
    }

    /** 校验 Token：无效/过期/已登出返回 null */
    @SneakyThrows
    public LoginUser checkToken(String token) {
        Claims claims = JwtUtils.parse(token, properties.getJwtSecret());
        String tokenUuid = claims.get("uuid", String.class);
        if (tokenUuid == null) {
            return null;
        }
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + tokenUuid);
        if (json == null) {
            return null;
        }
        return objectMapper.readValue(json, LoginUser.class);
    }

    /** 登出：删除 Redis 会话（Token 本身过期前不再可用） */
    public void deleteToken(String token) {
        try {
            Claims claims = JwtUtils.parse(token, properties.getJwtSecret());
            String tokenUuid = claims.get("uuid", String.class);
            if (tokenUuid != null) {
                redisTemplate.delete(KEY_PREFIX + tokenUuid);
            }
        } catch (Exception e) {
            log.debug("[deleteToken] Token 解析失败，忽略: {}", e.getMessage());
        }
    }
}
