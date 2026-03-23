package com.campus.trade.item.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class ItemListResponse {

    private String itemId;
    private String title;
    private BigDecimal price;
    private Integer conditionStar;
    private List<String> images;
    private String status;
    private Date createdAt;

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

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
