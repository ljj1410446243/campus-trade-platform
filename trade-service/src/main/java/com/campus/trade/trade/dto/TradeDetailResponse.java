package com.campus.trade.trade.dto;

import lombok.Data;

import java.util.Date;

@Data
public class TradeDetailResponse {

    private String tradeId;
    private String itemId;
    private String buyerId;
    private String sellerId;

    private String buyerNickname;
    private String sellerNickname;
    private String deliveryMode;
    private String pickupPointId;
    private String pickupPointName;
    private String pickupAddress;
    private Double pickupLat;
    private Double pickupLng;
    private String pickupContactPhone;
    private String pickupBusinessHours;

    private Double price;
    private String status;
    private String tradeNo;
    private String payStatus;
    private String payChannel;
    private String outTradeNo;
    private String providerTradeNo;
    private Date payExpireAt;
    private Date paidAt;
    private Date deliveredAt;
    private Date confirmedAt;
    private Date completedAt;
    private Date cancelledAt;
    private String cancelReason;
    private String refundReason;
    private Date createdAt;
}
