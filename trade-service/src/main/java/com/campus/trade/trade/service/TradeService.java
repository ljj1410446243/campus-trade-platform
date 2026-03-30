package com.campus.trade.trade.service;

import com.campus.trade.trade.dto.CancelTradeRequest;
import com.campus.trade.trade.dto.CreateTradeRequest;
import com.campus.trade.trade.dto.CreateTradeResponse;
import com.campus.trade.trade.dto.InitiatePaymentRequest;
import com.campus.trade.trade.dto.InitiatePaymentResponse;
import com.campus.trade.trade.dto.PaymentCallbackRequest;
import com.campus.trade.trade.dto.PaymentStatusResponse;
import com.campus.trade.trade.dto.RefundTradeRequest;
import com.campus.trade.trade.dto.TradeDetailResponse;
import com.campus.trade.trade.dto.TradeListResponse;

import java.util.List;

public interface TradeService {

    String ping();

    CreateTradeResponse createTrade(String buyerId, CreateTradeRequest request);

    List<TradeListResponse> listBuyingTrades(String buyerId);

    List<TradeListResponse> listSellingTrades(String sellerId);

    TradeDetailResponse getTradeDetail(String userId, String tradeId);

    void cancelTrade(String buyerId, String tradeId, CancelTradeRequest request);

    InitiatePaymentResponse initiatePayment(String buyerId, String tradeId, InitiatePaymentRequest request);

    PaymentStatusResponse queryPaymentStatus(String userId, String tradeId);

    PaymentStatusResponse mockPay(String buyerId, String tradeId);

    void deliverTrade(String sellerId, String tradeId);

    void confirmReceipt(String buyerId, String tradeId);

    void refundTrade(String buyerId, String tradeId, RefundTradeRequest request);

    PaymentStatusResponse handlePaymentCallback(PaymentCallbackRequest request);
}
