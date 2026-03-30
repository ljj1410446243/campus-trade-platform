package com.campus.trade.trade.dto;

import java.util.Date;
import java.util.Map;

public class InitiatePaymentResponse {

    private String tradeId;
    private String tradeNo;
    private String status;
    private String payStatus;
    private String payChannel;
    private Double amount;
    private String outTradeNo;
    private Date payExpireAt;
    private Map<String, Object> paymentData;

    public String getTradeId() {
        return tradeId;
    }

    public void setTradeId(String tradeId) {
        this.tradeId = tradeId;
    }

    public String getTradeNo() {
        return tradeNo;
    }

    public void setTradeNo(String tradeNo) {
        this.tradeNo = tradeNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPayStatus() {
        return payStatus;
    }

    public void setPayStatus(String payStatus) {
        this.payStatus = payStatus;
    }

    public String getPayChannel() {
        return payChannel;
    }

    public void setPayChannel(String payChannel) {
        this.payChannel = payChannel;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getOutTradeNo() {
        return outTradeNo;
    }

    public void setOutTradeNo(String outTradeNo) {
        this.outTradeNo = outTradeNo;
    }

    public Date getPayExpireAt() {
        return payExpireAt;
    }

    public void setPayExpireAt(Date payExpireAt) {
        this.payExpireAt = payExpireAt;
    }

    public Map<String, Object> getPaymentData() {
        return paymentData;
    }

    public void setPaymentData(Map<String, Object> paymentData) {
        this.paymentData = paymentData;
    }
}
