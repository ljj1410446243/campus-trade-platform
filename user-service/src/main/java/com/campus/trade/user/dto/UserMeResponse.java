package com.campus.trade.user.dto;

/**
 * 当前用户信息返回 DTO
 */
public class UserMeResponse {

  private String userId;
  private String username;
  private String nickname;
  private String phone;
  private String realName;
  private String studentId;
  private String avatarUrl;
  private Boolean campusVerified;
  private Integer creditScore;
  private String creditLevel;
  private Integer reviewCount;
  private Double averageRating;
  private String role;
  private String status;
  private String accountStatus;
  private Boolean banned;
  private Boolean disabled;

  public UserMeResponse() {
  }

  public UserMeResponse(String userId, String username, String nickname, String phone,
                        String realName, String studentId, String avatarUrl,
                        Boolean campusVerified, Integer creditScore, String creditLevel,
                        Integer reviewCount, Double averageRating, String role,
                        String status, String accountStatus, Boolean banned, Boolean disabled) {
    this.userId = userId;
    this.username = username;
    this.nickname = nickname;
    this.phone = phone;
    this.realName = realName;
    this.studentId = studentId;
    this.avatarUrl = avatarUrl;
    this.campusVerified = campusVerified;
    this.creditScore = creditScore;
    this.creditLevel = creditLevel;
    this.reviewCount = reviewCount;
    this.averageRating = averageRating;
    this.role = role;
    this.status = status;
    this.accountStatus = accountStatus;
    this.banned = banned;
    this.disabled = disabled;
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

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

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

  public String getCreditLevel() {
    return creditLevel;
  }

  public void setCreditLevel(String creditLevel) {
    this.creditLevel = creditLevel;
  }

  public Integer getReviewCount() {
    return reviewCount;
  }

  public void setReviewCount(Integer reviewCount) {
    this.reviewCount = reviewCount;
  }

  public Double getAverageRating() {
    return averageRating;
  }

  public void setAverageRating(Double averageRating) {
    this.averageRating = averageRating;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getAccountStatus() {
    return accountStatus;
  }

  public void setAccountStatus(String accountStatus) {
    this.accountStatus = accountStatus;
  }

  public Boolean getBanned() {
    return banned;
  }

  public void setBanned(Boolean banned) {
    this.banned = banned;
  }

  public Boolean getDisabled() {
    return disabled;
  }

  public void setDisabled(Boolean disabled) {
    this.disabled = disabled;
  }
}
