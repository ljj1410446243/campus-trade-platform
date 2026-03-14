package com.campus.trade.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 注册请求
 */
public class RegisterRequest {

  @NotBlank(message = "username不能为空")
  private String username;

  @NotBlank(message = "password不能为空")
  private String password;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
