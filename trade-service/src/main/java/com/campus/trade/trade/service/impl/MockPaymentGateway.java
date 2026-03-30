package com.campus.trade.trade.service.impl;

import com.campus.trade.trade.dto.InitiatePaymentResponse;
import com.campus.trade.trade.enums.PayChannel;
import com.campus.trade.trade.model.PaymentRecord;
import com.campus.trade.trade.model.Trade;
import com.campus.trade.trade.service.PaymentGateway;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public PayChannel getChannel() {
        return PayChannel.MOCK;
    }

    @Override
    public InitiatePaymentResponse buildPaymentResponse(Trade trade, PaymentRecord paymentRecord) {
        InitiatePaymentResponse response = new InitiatePaymentResponse();
        response.setTradeId(trade.getId());
        response.setTradeNo(trade.getTradeNo());
        response.setTradeStatus(trade.getStatus());
        response.setPayStatus(trade.getPayStatus());
        response.setChannel(PayChannel.MOCK);
        response.setAmount(trade.getAmount());
        response.setOutTradeNo(trade.getOutTradeNo());
        response.setPaymentNo(paymentRecord.getId());
        response.setPayExpireAt(trade.getPayExpireAt());

        Map<String, Object> paymentData = new LinkedHashMap<>();
        paymentData.put("mode", "MOCK");
        paymentData.put("message", "调用 mock 支付接口即可将订单推进到已支付");
        paymentData.put("mockAction", "/trades/" + trade.getId() + "/mock-pay");
        paymentData.put("pollAction", "/trades/" + trade.getId() + "/pay-status");
        response.setPaymentData(paymentData);
        return response;
    }
}
