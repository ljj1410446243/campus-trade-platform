package com.campus.trade.trade.dto;

public class PaymentStatusQueryResult {

    private final String payChannel;
    private final String providerTradeNo;

    public PaymentStatusQueryResult(String payChannel, String providerTradeNo) {
        this.payChannel = payChannel;
        this.providerTradeNo = providerTradeNo;
    }

    public String getPayChannel() {
        return payChannel;
    }

    public String getProviderTradeNo() {
        return providerTradeNo;
    }
}
