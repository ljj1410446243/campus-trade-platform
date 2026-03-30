package com.campus.trade.user.dto;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 浏览记录列表项返回 DTO
 */
public class BrowseHistoryItemResponse {

    private String itemId;
    private String title;
    private BigDecimal price;
    private Date viewedAt;
    private String source;

    public BrowseHistoryItemResponse() {
    }

    public BrowseHistoryItemResponse(String itemId, String title, BigDecimal price, Date viewedAt, String source) {
        this.itemId = itemId;
        this.title = title;
        this.price = price;
        this.viewedAt = viewedAt;
        this.source = source;
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
