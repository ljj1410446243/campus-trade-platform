package com.campus.trade.item.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class UpdateItemRequest {

    @NotBlank(message = "title不能为空")
    private String title;

    @NotBlank(message = "description不能为空")
    private String description;

    @NotNull(message = "price不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "price必须大于0")
    private BigDecimal price;

    @NotNull(message = "conditionStar不能为空")
    @Min(value = 1, message = "conditionStar最小为1")
    @Max(value = 5, message = "conditionStar最大为5")
    private Integer conditionStar;

    @NotBlank(message = "categoryId不能为空")
    private String categoryId;

    @NotBlank(message = "categoryName不能为空")
    private String categoryName;

    @NotBlank(message = "tradeMode不能为空")
    private String tradeMode;

    private List<String> images;

    private LocationDTO location;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getTradeMode() {
        return tradeMode;
    }

    public void setTradeMode(String tradeMode) {
        this.tradeMode = tradeMode;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public LocationDTO getLocation() {
        return location;
    }

    public void setLocation(LocationDTO location) {
        this.location = location;
    }
}
