package com.campus.trade.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 */
@Component
public class JwtUtil {

  @Value("${jwt.secret}")
  private String secret;

  @Value("${jwt.expiration}")
  private long expiration;

  /**
   * 生成 Token
   *
   * @param userId   用户ID
   * @param username 用户名
   * @return JWT Token
   */
  public String generateToken(String userId, String username) {
    Date now = new Date();
    Date expireDate = new Date(now.getTime() + expiration * 1000);

    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId);
    claims.put("username", username);

    return Jwts.builder()
            .claims(claims)
            .subject(username)
            .issuedAt(now)
            .expiration(expireDate)
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
    return Jwts.parser()
            .verifyWith(getSignKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
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
      return claims.getExpiration().after(new Date());
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * 从 Token 中获取 userId
   */
  public String getUserId(String token) {
    Claims claims = parseToken(token);
    Object value = claims.get("userId");
    return value == null ? null : value.toString();
  }

  /**
   * 从 Token 中获取 username
   */
  public String getUsername(String token) {
    Claims claims = parseToken(token);
    Object value = claims.get("username");
    return value == null ? null : value.toString();
  }

  /**
   * 获取签名 Key
   */
  private SecretKey getSignKey() {
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }
}
