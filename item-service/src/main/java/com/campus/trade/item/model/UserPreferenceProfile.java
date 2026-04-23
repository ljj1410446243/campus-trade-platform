package com.campus.trade.item.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Document(collection = "user_preference_profiles")
public class UserPreferenceProfile {

    public static class WeightedPreference {
        private String key;
        private Double weight;

        public WeightedPreference() {
        }

        public WeightedPreference(String key, Double weight) {
            this.key = key;
            this.weight = weight;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public Double getWeight() {
            return weight;
        }

        public void setWeight(Double weight) {
            this.weight = weight;
        }
    }

    public static class NumericPreference {
        private Integer value;
        private Double weight;

        public NumericPreference() {
        }

        public NumericPreference(Integer value, Double weight) {
            this.value = value;
            this.weight = weight;
        }

        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
        }

        public Double getWeight() {
            return weight;
        }

        public void setWeight(Double weight) {
            this.weight = weight;
        }
    }

    public static class BehaviorSummary {
        private Integer browseCount7d = 0;
        private Integer browseCount30d = 0;
        private Integer browseCount90d = 0;
        private Integer favoriteCount7d = 0;
        private Integer favoriteCount30d = 0;
        private Integer favoriteCount90d = 0;
        private Integer chatCount7d = 0;
        private Integer chatCount30d = 0;
        private Integer chatCount90d = 0;
        private Integer tradeCount7d = 0;
        private Integer tradeCount30d = 0;
        private Integer tradeCount90d = 0;
        private Integer positiveReviewCount90d = 0;

        public Integer getBrowseCount7d() {
            return browseCount7d;
        }

        public void setBrowseCount7d(Integer browseCount7d) {
            this.browseCount7d = browseCount7d;
        }

        public Integer getBrowseCount30d() {
            return browseCount30d;
        }

        public void setBrowseCount30d(Integer browseCount30d) {
            this.browseCount30d = browseCount30d;
        }

        public Integer getBrowseCount90d() {
            return browseCount90d;
        }

        public void setBrowseCount90d(Integer browseCount90d) {
            this.browseCount90d = browseCount90d;
        }

        public Integer getFavoriteCount7d() {
            return favoriteCount7d;
        }

        public void setFavoriteCount7d(Integer favoriteCount7d) {
            this.favoriteCount7d = favoriteCount7d;
        }

        public Integer getFavoriteCount30d() {
            return favoriteCount30d;
        }

        public void setFavoriteCount30d(Integer favoriteCount30d) {
            this.favoriteCount30d = favoriteCount30d;
        }

        public Integer getFavoriteCount90d() {
            return favoriteCount90d;
        }

        public void setFavoriteCount90d(Integer favoriteCount90d) {
            this.favoriteCount90d = favoriteCount90d;
        }

        public Integer getChatCount7d() {
            return chatCount7d;
        }

        public void setChatCount7d(Integer chatCount7d) {
            this.chatCount7d = chatCount7d;
        }

        public Integer getChatCount30d() {
            return chatCount30d;
        }

        public void setChatCount30d(Integer chatCount30d) {
            this.chatCount30d = chatCount30d;
        }

        public Integer getChatCount90d() {
            return chatCount90d;
        }

        public void setChatCount90d(Integer chatCount90d) {
            this.chatCount90d = chatCount90d;
        }

        public Integer getTradeCount7d() {
            return tradeCount7d;
        }

        public void setTradeCount7d(Integer tradeCount7d) {
            this.tradeCount7d = tradeCount7d;
        }

        public Integer getTradeCount30d() {
            return tradeCount30d;
        }

        public void setTradeCount30d(Integer tradeCount30d) {
            this.tradeCount30d = tradeCount30d;
        }

        public Integer getTradeCount90d() {
            return tradeCount90d;
        }

        public void setTradeCount90d(Integer tradeCount90d) {
            this.tradeCount90d = tradeCount90d;
        }

        public Integer getPositiveReviewCount90d() {
            return positiveReviewCount90d;
        }

        public void setPositiveReviewCount90d(Integer positiveReviewCount90d) {
            this.positiveReviewCount90d = positiveReviewCount90d;
        }
    }

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    private BehaviorSummary behaviorSummary = new BehaviorSummary();
    private List<WeightedPreference> categoryPrefs = new ArrayList<>();
    private List<WeightedPreference> keywordPrefs = new ArrayList<>();
    private List<WeightedPreference> priceRangePrefs = new ArrayList<>();
    private List<NumericPreference> conditionPrefs = new ArrayList<>();
    private List<WeightedPreference> tradeModePrefs = new ArrayList<>();
    private Double userVectorNorm = 0D;
    private Date lastBehaviorAt;
    private Date updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public BehaviorSummary getBehaviorSummary() {
        return behaviorSummary;
    }

    public void setBehaviorSummary(BehaviorSummary behaviorSummary) {
        this.behaviorSummary = behaviorSummary;
    }

    public List<WeightedPreference> getCategoryPrefs() {
        return categoryPrefs;
    }

    public void setCategoryPrefs(List<WeightedPreference> categoryPrefs) {
        this.categoryPrefs = categoryPrefs;
    }

    public List<WeightedPreference> getKeywordPrefs() {
        return keywordPrefs;
    }

    public void setKeywordPrefs(List<WeightedPreference> keywordPrefs) {
        this.keywordPrefs = keywordPrefs;
    }

    public List<WeightedPreference> getPriceRangePrefs() {
        return priceRangePrefs;
    }

    public void setPriceRangePrefs(List<WeightedPreference> priceRangePrefs) {
        this.priceRangePrefs = priceRangePrefs;
    }

    public List<NumericPreference> getConditionPrefs() {
        return conditionPrefs;
    }

    public void setConditionPrefs(List<NumericPreference> conditionPrefs) {
        this.conditionPrefs = conditionPrefs;
    }

    public List<WeightedPreference> getTradeModePrefs() {
        return tradeModePrefs;
    }

    public void setTradeModePrefs(List<WeightedPreference> tradeModePrefs) {
        this.tradeModePrefs = tradeModePrefs;
    }

    public Double getUserVectorNorm() {
        return userVectorNorm;
    }

    public void setUserVectorNorm(Double userVectorNorm) {
        this.userVectorNorm = userVectorNorm;
    }

    public Date getLastBehaviorAt() {
        return lastBehaviorAt;
    }

    public void setLastBehaviorAt(Date lastBehaviorAt) {
        this.lastBehaviorAt = lastBehaviorAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
