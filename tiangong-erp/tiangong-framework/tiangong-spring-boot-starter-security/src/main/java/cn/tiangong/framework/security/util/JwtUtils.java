package cn.tiangong.framework.security.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

/**
 * JWT 工具：HS256 签名。JWT 只承载 userId 与 tokenUuid（Redis 会话键），
 * 不携带权限等敏感信息，权限以 Redis 会话为准（可随时踢下线）。
 */
public class JwtUtils {

    private static SecretKey key(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public static String generate(Long userId, String tokenUuid, String secret, Duration expire) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("uuid", tokenUuid)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expire.toMillis()))
                .signWith(key(secret))
                .compact();
    }

    /** 解析失败（过期/伪造）抛 io.jsonwebtoken.JwtException */
    public static Claims parse(String token, String secret) {
        return Jwts.parser().verifyWith(key(secret)).build().parseSignedClaims(token).getPayload();
    }
}
