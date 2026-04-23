package com.campus.trade.item.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 搜索结果列表项
 */
public class SearchItemResponse {

    public static class DebugScores {
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

    public static class SellerInfo {
        private String userId;
        private String nickname;
        private String avatarUrl;
        private Integer creditScore;
        private String creditLevel;
        private Integer reviewCount;
        private Double averageRating;

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

    private String itemId;
    private String title;
    private BigDecimal price;
    private Integer conditionStar;
    private String coverImage;
    private Integer hotScore;
    private LocationDTO location;
    private Double distanceMeters;
    private SellerInfo seller;
    private String recommendReason;
    private List<String> sourceChannels;
    private DebugScores debugScores;

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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getConditionStar() {
        return conditionStar;
    }

    public void setConditionStar(Integer conditionStar) {
        this.conditionStar = conditionStar;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public Integer getHotScore() {
        return hotScore;
    }

    public void setHotScore(Integer hotScore) {
        this.hotScore = hotScore;
    }

    public LocationDTO getLocation() {
        return location;
    }

    public void setLocation(LocationDTO location) {
        this.location = location;
    }

    public Double getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(Double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public SellerInfo getSeller() {
        return seller;
    }

    public void setSeller(SellerInfo seller) {
        this.seller = seller;
    }

    public String getRecommendReason() {
        return recommendReason;
    }

    public void setRecommendReason(String recommendReason) {
        this.recommendReason = recommendReason;
    }

    public List<String> getSourceChannels() {
        return sourceChannels;
    }

    public void setSourceChannels(List<String> sourceChannels) {
        this.sourceChannels = sourceChannels;
    }

    public DebugScores getDebugScores() {
        return debugScores;
    }

    public void setDebugScores(DebugScores debugScores) {
        this.debugScores = debugScores;
    }
}
