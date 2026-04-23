package com.campus.trade.item.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class InternalNegotiatedPriceRequest {

    @NotBlank(message = "sellerId不能为空")
    private String sellerId;

    @NotBlank(message = "conversationId不能为空")
    private String conversationId;

    @NotBlank(message = "bargainRecordId不能为空")
    private String bargainRecordId;

    @NotNull(message = "price不能为空")
    @DecimalMin(value = "0.01", message = "price必须大于0")
    private BigDecimal price;

    @NotBlank(message = "operatorUserId不能为空")
    private String operatorUserId;

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getBargainRecordId() {
        return bargainRecordId;
    }

    public void setBargainRecordId(String bargainRecordId) {
        this.bargainRecordId = bargainRecordId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getOperatorUserId() {
        return operatorUserId;
    }

    public void setOperatorUserId(String operatorUserId) {
        this.operatorUserId = operatorUserId;
    }
}
