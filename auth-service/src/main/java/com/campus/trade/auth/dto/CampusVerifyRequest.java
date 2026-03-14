package com.campus.trade.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 校园认证请求
 */
public class CampusVerifyRequest {

  @NotBlank(message = "studentId不能为空")
  private String studentId;

  @NotBlank(message = "schoolEmail不能为空")
  private String schoolEmail;

  public String getStudentId() {
    return studentId;
  }

  public void setStudentId(String studentId) {
    this.studentId = studentId;
  }

  public String getSchoolEmail() {
    return schoolEmail;
  }

  public void setSchoolEmail(String schoolEmail) {
    this.schoolEmail = schoolEmail;
  }
}
