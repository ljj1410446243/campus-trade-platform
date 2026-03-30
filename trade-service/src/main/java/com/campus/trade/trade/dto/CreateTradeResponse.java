package com.campus.trade.trade.dto;

import com.campus.trade.trade.enums.PayChannel;
import com.campus.trade.trade.enums.PayStatus;
import com.campus.trade.trade.enums.TradeStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class CreateTradeResponse {

    private String tradeId;
    private String tradeNo;
    private String itemId;
    private BigDecimal amount;
    private TradeStatus status;
    private PayStatus payStatus;
    private PayChannel defaultPayChannel;
    private String outTradeNo;
    private Date payExpireAt;
    private Date createdAt;
}
