package com.campus.trade.user.dto;

import java.util.Date;

/**
 * 浏览记录列表项返回 DTO
 */
public class BrowseHistoryItemResponse {

    private String itemId;
    private Date viewedAt;
    private String source;

    public BrowseHistoryItemResponse() {
    }

    public BrowseHistoryItemResponse(String itemId, Date viewedAt, String source) {
        this.itemId = itemId;
        this.viewedAt = viewedAt;
        this.source = source;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public Date getViewedAt() {
        return viewedAt;
    }

    public void setViewedAt(Date viewedAt) {
        this.viewedAt = viewedAt;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
