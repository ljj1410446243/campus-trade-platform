package com.campus.trade.user.dto;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 收藏列表项返回 DTO
 */
public class FavoriteItemResponse {

    private String itemId;
    private String title;
    private BigDecimal price;
    private Integer conditionStar;
    private String coverImage;
    private Date createdAt;

    public FavoriteItemResponse() {
    }

    public FavoriteItemResponse(String itemId, String title, BigDecimal price,
                                Integer conditionStar, String coverImage, Date createdAt) {
        this.itemId = itemId;
        this.title = title;
        this.price = price;
        this.conditionStar = conditionStar;
        this.coverImage = coverImage;
        this.createdAt = createdAt;
    }

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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
