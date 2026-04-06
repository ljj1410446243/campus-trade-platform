package com.campus.trade.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求DTO
 */
public class LoginRequest {

  @JsonAlias("username")
  @NotBlank(message = "account不能为空")
  private String account;

  @NotBlank(message = "password不能为空")
  private String password;

  public String getAccount() {
    return account;
  }

  public void setAccount(String account) {
    this.account = account;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
