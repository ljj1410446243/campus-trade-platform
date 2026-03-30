package com.campus.trade.trade.dto;

public class InitiatePaymentRequest {

    private String channel = "MOCK";

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }
}
