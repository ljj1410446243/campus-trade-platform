package com.campus.trade.trade.dto;

import com.campus.trade.trade.enums.PayChannel;
import com.campus.trade.trade.enums.PayStatus;
import com.campus.trade.trade.enums.TradeStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

@Data
public class InitiatePaymentResponse {

    private String tradeId;
    private String tradeNo;
    private TradeStatus tradeStatus;
    private PayStatus payStatus;
    private PayChannel channel;
    private BigDecimal amount;
    private String outTradeNo;
    private String paymentNo;
    private Date payExpireAt;
    private Map<String, Object> paymentData;
}
