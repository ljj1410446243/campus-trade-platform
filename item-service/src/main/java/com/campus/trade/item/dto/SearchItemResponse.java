package com.campus.trade.item.dto;

import java.math.BigDecimal;

/**
 * 搜索结果列表项
 */
public class SearchItemResponse {

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
    private SellerInfo seller;

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

    public SellerInfo getSeller() {
        return seller;
    }

    public void setSeller(SellerInfo seller) {
        this.seller = seller;
    }
}
