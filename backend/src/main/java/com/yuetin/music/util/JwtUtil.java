package com.yuetin.music.util;

import com.yuetin.music.common.CurrentUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * JWT 签发与校验（HS256）。
 *
 * <p>与前端 mock 层 {@code src/mock/jwt.js} 使用相同的 header/payload 字段名，
 * 因此同一个 token 在两种模式下都能被识别，便于灰度切换。</p>
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${yuetin.jwt.secret}")
    private String secret;

    @Value("${yuetin.jwt.expire-days:7}")
    private int expireDays;

    private Key key;

    @PostConstruct
    public void init() {
        // HS256 要求密钥至少 256 位，不足时补零，避免启动即抛异常
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(raw, 0, padded, 0, raw.length);
            raw = padded;
        }
        this.key = Keys.hmacShaKeyFor(raw);
    }

    public String sign(Long userId, String username, String role) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expireDays * 24 * 3600 * 1000L))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 校验并解析 token，失败返回 null。
     */
    public CurrentUser verify(String token) {
        if (token == null || token.trim().isEmpty()) return null;
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            CurrentUser user = new CurrentUser();
            user.setId(Long.valueOf(claims.getSubject()));
            user.setUsername(claims.get("username", String.class));
            user.setRole(claims.get("role", String.class));
            return user;
        } catch (Exception e) {
            // 过期、签名不合法、格式错误统一视为未登录
            log.debug("JWT 校验失败: {}", e.getMessage());
            return null;
        }
    }
}
