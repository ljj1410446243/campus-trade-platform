package com.campus.trade.user.dto;

import java.util.Date;

/**
 * 收藏列表项返回 DTO
 */
public class FavoriteItemResponse {

    private String itemId;
    private Date createdAt;

    public FavoriteItemResponse() {
    }

    public FavoriteItemResponse(String itemId, Date createdAt) {
        this.itemId = itemId;
        this.createdAt = createdAt;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
