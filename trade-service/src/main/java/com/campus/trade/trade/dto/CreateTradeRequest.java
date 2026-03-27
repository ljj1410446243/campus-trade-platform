package com.campus.trade.trade.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.awt.geom.Arc2D;
import java.math.BigDecimal;

public class CreateTradeRequest {

    @NotBlank(message = "itemId不能为空")
    private String itemId;

    @NotNull(message = "price不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "price必须大于0")
    private Double price;

    @NotBlank(message = "sellerId不能为空")
    private String sellerId;

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }
}
