package com.campus.trade.item.dto;

import jakarta.validation.constraints.NotBlank;

public class RecommendationFeedbackRequest {

    @NotBlank(message = "requestId不能为空")
    private String requestId;

    @NotBlank(message = "scene不能为空")
    private String scene;

    @NotBlank(message = "eventType不能为空")
    private String eventType;

    @NotBlank(message = "itemId不能为空")
    private String itemId;

    private String targetItemId;
    private String targetConversationId;
    private String targetTradeId;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getTargetItemId() {
        return targetItemId;
    }

    public void setTargetItemId(String targetItemId) {
        this.targetItemId = targetItemId;
    }

    public String getTargetConversationId() {
        return targetConversationId;
    }

    public void setTargetConversationId(String targetConversationId) {
        this.targetConversationId = targetConversationId;
    }

    public String getTargetTradeId() {
        return targetTradeId;
    }

    public void setTargetTradeId(String targetTradeId) {
        this.targetTradeId = targetTradeId;
    }
}
