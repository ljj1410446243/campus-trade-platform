package com.campus.trade.user.dto;

import java.util.Date;

public class ReportItemResponse {

    private String reportId;
    private String reportType;
    private String targetId;
    private String reporterId;
    private String targetUserId;
    private String itemId;
    private String reasonCode;
    private String description;
    private String status;
    private String reviewedBy;
    private Date reviewedAt;
    private String reviewNote;
    private Date createdAt;
    private Date updatedAt;
    private ReportUserSummary reporter;
    private ReportUserSummary targetUser;
    private ReportItemSummary targetItem;

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getReporterId() {
        return reporterId;
    }

    public void setReporterId(String reporterId) {
        this.reporterId = reporterId;
    }

    public String getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(String targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public Date getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Date reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public ReportUserSummary getReporter() {
        return reporter;
    }

    public void setReporter(ReportUserSummary reporter) {
        this.reporter = reporter;
    }

    public ReportUserSummary getTargetUser() {
        return targetUser;
    }

    public void setTargetUser(ReportUserSummary targetUser) {
        this.targetUser = targetUser;
    }

    public ReportItemSummary getTargetItem() {
        return targetItem;
    }

    public void setTargetItem(ReportItemSummary targetItem) {
        this.targetItem = targetItem;
    }

    public static class ReportUserSummary {
        private String userId;
        private String username;
        private String nickname;

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
    }

    public static class ReportItemSummary {
        private String itemId;
        private String title;

        public String getItemId() {
            return itemId;
        }

        public void setItemId(String itemId) {
            this.itemId = itemId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }
}
