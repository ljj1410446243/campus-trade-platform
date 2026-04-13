package com.campus.trade.auth.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * 用户实体
 */
@Document(collection = "users")
public class User {

  @Id
  private String id;

  private String username;

  private String passwordHash;

  private String phone;

  private String nickname;

  private String realName;

  private String avatarUrl;

  private String role;

  private String status;

  private boolean campusVerified;

  private String studentId;

  private Date bannedAt;

  private String bannedReason;

  private Integer creditScore;

  private String creditLevel;

  private Integer reviewCount;

  private Double averageRating;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }

  public String getRealName() {
    return realName;
  }

  public void setRealName(String realName) {
    this.realName = realName;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
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

  public void setCampusVerified(boolean campusVerified) {
    this.campusVerified = campusVerified;
  }

  public void setStudentId(String studentId) {
    this.studentId = studentId;
  }

  public Date getBannedAt() {
    return bannedAt;
  }

  public void setBannedAt(Date bannedAt) {
    this.bannedAt = bannedAt;
  }

  public String getBannedReason() {
    return bannedReason;
  }

  public void setBannedReason(String bannedReason) {
    this.bannedReason = bannedReason;
  }

  public boolean isCampusVerified() {
    return campusVerified;
  }

  public String getStudentId() {
    return studentId;
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
}
