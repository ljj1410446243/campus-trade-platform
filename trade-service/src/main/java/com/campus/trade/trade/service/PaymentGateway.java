package com.campus.trade.trade.service;

import com.campus.trade.trade.dto.InitiatePaymentRequest;
import com.campus.trade.trade.dto.PaymentInitiationResult;
import com.campus.trade.trade.dto.PaymentStatusQueryResult;
import com.campus.trade.trade.model.Trade;

public interface PaymentGateway {

    String getChannel();

    PaymentInitiationResult initiate(Trade trade, InitiatePaymentRequest request);

    PaymentStatusQueryResult query(Trade trade);

    String generateProviderTradeNo();
}
