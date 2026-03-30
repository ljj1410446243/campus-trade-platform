package com.campus.trade.user.util;

import com.campus.trade.user.exception.AuthenticationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT 工具类
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    /**
     * 解析 Token
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSignKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new AuthenticationException("access token已过期");
        } catch (JwtException | IllegalArgumentException e) {
            throw new AuthenticationException("access token无效");
        }
    }

    /**
     * 获取 userId
     */
    public String getUserId(String token) {
        Claims claims = parseToken(token);
        Object type = claims.get("type");
        if (!"access".equals(type)) {
            throw new AuthenticationException("token类型无效");
        }

        Object value = claims.get("userId");
        if (value == null || value.toString().isBlank()) {
            throw new AuthenticationException("token用户信息无效");
        }
        return value.toString();
    }

    /**
     * 获取签名 Key
     */
    private SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
