package com.campus.trade.trade.model;

import com.campus.trade.trade.enums.PayChannel;
import com.campus.trade.trade.enums.PayStatus;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

@Data
@Document(collection = "trade_payments")
public class PaymentRecord {

    @Id
    private String id;

    private String tradeId;
    private String tradeNo;
    private String buyerId;

    private BigDecimal amount;
    private PayChannel channel;
    private PayStatus status;

    private String outTradeNo;
    private String providerTradeNo;

    private Date paidAt;
    private Date createdAt;
    private Date updatedAt;

    /**
     * 预留下游支付参数/回调报文等扩展字段
     */
    private Map<String, Object> extension;
}
