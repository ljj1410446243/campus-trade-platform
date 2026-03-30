package com.campus.trade.user.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Document(collection = "items")
public class Item {

    @Id
    private String id;

    private String title;
    private BigDecimal price;
    private Integer conditionStar;
    private String coverImage;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
}
