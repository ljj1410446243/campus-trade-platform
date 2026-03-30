package com.campus.trade.trade.dto;

import com.campus.trade.trade.enums.PayChannel;
import lombok.Data;

@Data
public class PaymentCallbackRequest {

    /**
     * PAY_SUCCESS / REFUND_SUCCESS
     */
    private String eventType = "PAY_SUCCESS";

    private String tradeId;
    private String tradeNo;
    private String outTradeNo;
    private String providerTradeNo;
    private PayChannel channel = PayChannel.MOCK;
    private String signature;
}
