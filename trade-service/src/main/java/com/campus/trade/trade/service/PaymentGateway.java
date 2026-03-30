package com.campus.trade.trade.service;

import com.campus.trade.trade.dto.InitiatePaymentResponse;
import com.campus.trade.trade.enums.PayChannel;
import com.campus.trade.trade.model.PaymentRecord;
import com.campus.trade.trade.model.Trade;

public interface PaymentGateway {

    PayChannel getChannel();

    InitiatePaymentResponse buildPaymentResponse(Trade trade, PaymentRecord paymentRecord);
}
