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

    private Double price;
    private String status;
    private Date createdAt;
}
