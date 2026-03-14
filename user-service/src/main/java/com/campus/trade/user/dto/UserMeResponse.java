package com.campus.trade.user.dto;

/**
 * 当前用户信息返回 DTO
 */
public class UserMeResponse {

  private String userId;
  private String nickname;
  private String avatarUrl;
  private Boolean campusVerified;
  private Integer creditScore;
  private String role;

  public UserMeResponse() {
  }

  public UserMeResponse(String userId, String nickname, String avatarUrl,
                        Boolean campusVerified, Integer creditScore, String role) {
    this.userId = userId;
    this.nickname = nickname;
    this.avatarUrl = avatarUrl;
    this.campusVerified = campusVerified;
    this.creditScore = creditScore;
    this.role = role;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }

  public Boolean getCampusVerified() {
    return campusVerified;
  }

  public void setCampusVerified(Boolean campusVerified) {
    this.campusVerified = campusVerified;
  }

  public Integer getCreditScore() {
    return creditScore;
  }

  public void setCreditScore(Integer creditScore) {
    this.creditScore = creditScore;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }
}
