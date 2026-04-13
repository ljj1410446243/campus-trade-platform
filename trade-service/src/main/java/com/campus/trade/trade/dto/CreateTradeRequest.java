package com.campus.trade.trade.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

public class CreateTradeRequest {

    @NotBlank(message = "itemId不能为空")
    private String itemId;

    @DecimalMin(value = "0.0", inclusive = false, message = "price必须大于0")
    private Double price;

    private String sellerId;
    private String deliveryMode;
    private String pickupPointId;

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

    public String getDeliveryMode() {
        return deliveryMode;
    }

    public void setDeliveryMode(String deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    public String getPickupPointId() {
        return pickupPointId;
    }

    public void setPickupPointId(String pickupPointId) {
        this.pickupPointId = pickupPointId;
    }
}
