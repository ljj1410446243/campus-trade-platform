package com.campus.trade.trade.service.impl;

import com.campus.trade.trade.dto.InitiatePaymentRequest;
import com.campus.trade.trade.dto.PaymentInitiationResult;
import com.campus.trade.trade.dto.PaymentStatusQueryResult;
import com.campus.trade.trade.model.Trade;
import com.campus.trade.trade.service.PaymentGateway;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class MockPaymentGateway implements PaymentGateway {

    private static final String CHANNEL = "MOCK";

    @Override
    public String getChannel() {
        return CHANNEL;
    }

    @Override
    public PaymentInitiationResult initiate(Trade trade, InitiatePaymentRequest request) {
        Map<String, Object> paymentData = new LinkedHashMap<>();
        paymentData.put("mode", CHANNEL);
        paymentData.put("message", "调用 mock 支付接口完成本地联调");
        paymentData.put("mockAction", "/trades/" + trade.getId() + "/mock-pay");
        return new PaymentInitiationResult(CHANNEL, paymentData);
    }

    @Override
    public PaymentStatusQueryResult query(Trade trade) {
        return new PaymentStatusQueryResult(CHANNEL, trade.getProviderTradeNo());
    }

    @Override
    public String generateProviderTradeNo() {
        return "MOCK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }
}
