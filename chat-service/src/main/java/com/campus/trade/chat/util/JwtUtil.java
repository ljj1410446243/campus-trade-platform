package com.campus.trade.chat.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtUtil {

  @Value("${jwt.secret}")
  private String secret;

  public String getUserIdFromToken(String token) {
    Claims claims = parseToken(token);
    Object userId = claims.get("userId");
    return userId == null ? null : String.valueOf(userId);
  }

  private Claims parseToken(String token) {
    if (token == null || token.isBlank()) {
      throw new RuntimeException("Token不能为空");
    }

    String realToken = token.startsWith("Bearer ") ? token.substring(7) : token;
    SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

    return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(realToken)
            .getBody();
  }
}
