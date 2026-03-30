package com.campus.trade.trade.dto;

import com.campus.trade.trade.enums.PayChannel;
import com.campus.trade.trade.enums.PayStatus;
import com.campus.trade.trade.enums.TradeStatus;
import lombok.Data;

import java.util.Date;

@Data
public class PaymentStatusResponse {

    private String tradeId;
    private String tradeNo;
    private TradeStatus tradeStatus;
    private PayStatus payStatus;
    private PayChannel channel;
    private String outTradeNo;
    private String providerTradeNo;
    private Date payExpireAt;
    private Date paidAt;
    private Date updatedAt;
}
