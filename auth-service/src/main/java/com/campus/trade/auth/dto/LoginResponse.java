package com.campus.trade.auth.dto;

/**
 * 登录返回DTO
 */
public class LoginResponse {

  private String accessToken;
  private long expiresIn;

  public LoginResponse() {
  }

  public LoginResponse(String accessToken, long expiresIn) {
    this.accessToken = accessToken;
    this.expiresIn = expiresIn;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public long getExpiresIn() {
    return expiresIn;
  }

  public void setExpiresIn(long expiresIn) {
    this.expiresIn = expiresIn;
  }
}
