package com.campus.trade.auth.dto;

/**
 * Token 解析结果 DTO
 */
public class TokenPayloadResponse {

  private String userId;
  private String username;

  public TokenPayloadResponse() {
  }

  public TokenPayloadResponse(String userId, String username) {
    this.userId = userId;
    this.username = username;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }
}
