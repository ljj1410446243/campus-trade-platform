package com.campus.trade.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 校园认证请求
 */
public class CampusVerifyRequest {

  @NotBlank(message = "realName不能为空")
  private String realName;

  @NotBlank(message = "studentId不能为空")
  private String studentId;

  public String getRealName() {
    return realName;
  }

  public void setRealName(String realName) {
    this.realName = realName;
  }

  public String getStudentId() {
    return studentId;
  }

  public void setStudentId(String studentId) {
    this.studentId = studentId;
  }
}
