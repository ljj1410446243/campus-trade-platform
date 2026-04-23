package com.campus.trade.item.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Document(collection = "recommendation_logs")
public class RecommendationLog {

    public static class ScoreDetail {
        private Double profileScore;
        private Double behaviorSimilarityScore;
        private Double associationScore;
        private Double hotnessScore;
        private Double freshnessScore;
        private Double distanceScore;
        private Double totalScore;

        public Double getProfileScore() {
            return profileScore;
        }

        public void setProfileScore(Double profileScore) {
            this.profileScore = profileScore;
        }

        public Double getBehaviorSimilarityScore() {
            return behaviorSimilarityScore;
        }

        public void setBehaviorSimilarityScore(Double behaviorSimilarityScore) {
            this.behaviorSimilarityScore = behaviorSimilarityScore;
        }

        public Double getAssociationScore() {
            return associationScore;
        }

        public void setAssociationScore(Double associationScore) {
            this.associationScore = associationScore;
        }

        public Double getHotnessScore() {
            return hotnessScore;
        }

        public void setHotnessScore(Double hotnessScore) {
            this.hotnessScore = hotnessScore;
        }

        public Double getFreshnessScore() {
            return freshnessScore;
        }

        public void setFreshnessScore(Double freshnessScore) {
            this.freshnessScore = freshnessScore;
        }

        public Double getDistanceScore() {
            return distanceScore;
        }

        public void setDistanceScore(Double distanceScore) {
            this.distanceScore = distanceScore;
        }

        public Double getTotalScore() {
            return totalScore;
        }

        public void setTotalScore(Double totalScore) {
            this.totalScore = totalScore;
        }
    }

    @Id
    private String id;

    @Indexed
    private String requestId;

    @Indexed
    private String userId;

    @Indexed
    private String scene;

    @Indexed
    private String itemId;

    private List<String> sourceChannels = new ArrayList<>();
    private Integer rank;
    private ScoreDetail scores;
    private String eventType;
    private String targetItemId;
    private String targetConversationId;
    private String targetTradeId;

    @Indexed
    private Date createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public List<String> getSourceChannels() {
        return sourceChannels;
    }

    public void setSourceChannels(List<String> sourceChannels) {
        this.sourceChannels = sourceChannels;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public ScoreDetail getScores() {
        return scores;
    }

    public void setScores(ScoreDetail scores) {
        this.scores = scores;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
