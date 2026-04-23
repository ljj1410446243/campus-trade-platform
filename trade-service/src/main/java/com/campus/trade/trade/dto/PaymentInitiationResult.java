package com.campus.trade.trade.dto;

import java.util.Map;

public class PaymentInitiationResult {

    private final String payChannel;
    private final Map<String, Object> paymentData;

    public PaymentInitiationResult(String payChannel, Map<String, Object> paymentData) {
        this.payChannel = payChannel;
        this.paymentData = paymentData;
    }

    public String getPayChannel() {
        return payChannel;
    }

    public Map<String, Object> getPaymentData() {
        return paymentData;
    }
}
