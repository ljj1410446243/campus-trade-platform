package com.campus.trade.user.dto;

import java.util.Map;

public class InternalNotificationRequest {

    private String recipientUserId;
    private String category;
    private String title;
    private String content;
    private String actionType;
    private String actionTargetId;
    private Map<String, Object> extra;

    public String getRecipientUserId() {
        return recipientUserId;
    }

    public void setRecipientUserId(String recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getActionTargetId() {
        return actionTargetId;
    }

    public void setActionTargetId(String actionTargetId) {
        this.actionTargetId = actionTargetId;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }

    public void setExtra(Map<String, Object> extra) {
        this.extra = extra;
    }
}
