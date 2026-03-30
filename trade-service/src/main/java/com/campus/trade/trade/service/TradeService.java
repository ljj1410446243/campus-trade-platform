package com.campus.trade.trade.service;

import com.campus.trade.trade.dto.CreateTradeRequest;
import com.campus.trade.trade.dto.InitiatePaymentRequest;
import com.campus.trade.trade.dto.InitiatePaymentResponse;
import com.campus.trade.trade.dto.PaymentStatusResponse;
import com.campus.trade.trade.dto.TradeDetailResponse;
import com.campus.trade.trade.dto.TradeListResponse;

import java.util.List;

public interface TradeService {

    String ping();

    String createTrade(String buyerId, CreateTradeRequest request);

    InitiatePaymentResponse initiatePayment(String buyerId, String tradeId, InitiatePaymentRequest request);

    PaymentStatusResponse queryPaymentStatus(String userId, String tradeId);

    PaymentStatusResponse mockPay(String buyerId, String tradeId);

    List<TradeListResponse> listBuyingTrades(String buyerId);

    List<TradeListResponse> listSellingTrades(String sellerId);

    TradeDetailResponse getTradeDetail(String tradeId);

    void cancelTrade(String userId, String tradeId);

    void completeTrade(String userId, String tradeId);
}
