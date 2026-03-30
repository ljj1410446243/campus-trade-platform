package com.campus.trade.auth.util;

import com.campus.trade.auth.config.JwtProperties;
import com.campus.trade.auth.exception.AuthenticationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 */
@Component
public class JwtUtil {

  private final JwtProperties jwtProperties;

  public JwtUtil(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
  }

  /**
   * 生成 Token
   *
   * @param userId   用户ID
   * @param username 用户名
   * @return JWT Token
   */
  public String generateAccessToken(String userId, String username, String sessionId) {
    Instant now = Instant.now();
    Instant expireAt = now.plusSeconds(jwtProperties.getAccessTokenExpiration());

    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId);
    claims.put("username", username);
    claims.put("type", "access");
    claims.put("sid", sessionId);

    return Jwts.builder()
            .claims(claims)
            .subject(username)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expireAt))
            .signWith(getSignKey())
            .compact();
  }

  /**
   * 解析 Token，获取 Claims
   *
   * @param token JWT Token
   * @return Claims
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
   * 判断 Token 是否有效
   *
   * @param token JWT Token
   * @return 是否有效
   */
  public boolean isTokenValid(String token) {
    try {
      Claims claims = parseToken(token);
      return claims.getExpiration().after(new Date()) && "access".equals(claims.get("type"));
    } catch (AuthenticationException e) {
      return false;
    }
  }

  /**
   * 从 Token 中获取 userId
   */
  public String getUserId(String token) {
    Claims claims = parseToken(token);
    Object value = claims.get("userId");
    if (value == null || value.toString().isBlank()) {
      throw new AuthenticationException("token用户信息无效");
    }
    return value.toString();
  }

  /**
   * 从 Token 中获取 username
   */
  public String getUsername(String token) {
    Claims claims = parseToken(token);
    Object value = claims.get("username");
    if (value == null || value.toString().isBlank()) {
      throw new AuthenticationException("token用户信息无效");
    }
    return value.toString();
  }

  public long getAccessTokenExpiration() {
    return jwtProperties.getAccessTokenExpiration();
  }

  public void validateAccessToken(String token) {
    Claims claims = parseToken(token);
    Object type = claims.get("type");
    if (!"access".equals(type)) {
      throw new AuthenticationException("token类型无效");
    }
  }

  /**
   * 获取签名 Key
   */
  private SecretKey getSignKey() {
    return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
  }
}
