package com.campus.trade.trade.dto;

import com.campus.trade.trade.enums.PayChannel;
import com.campus.trade.trade.enums.PayStatus;
import com.campus.trade.trade.enums.TradeLogAction;
import com.campus.trade.trade.enums.TradeStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class TradeDetailResponse {

    private String tradeId;
    private String tradeNo;
    private String itemId;
    private BigDecimal amount;
    private TradeStatus status;
    private PayStatus payStatus;
    private PayChannel payChannel;
    private String outTradeNo;
    private String providerTradeNo;
    private String cancelReason;
    private String refundReason;

    private Date createdAt;
    private Date updatedAt;
    private Date payExpireAt;
    private Date paidAt;
    private Date deliveredAt;
    private Date confirmedAt;
    private Date completedAt;
    private Date cancelledAt;
    private Date refundingAt;
    private Date refundedAt;

    private TradePartyInfo buyer;
    private TradePartyInfo seller;
    private TradeItemDetail item;
    private PaymentInfo payment;
    private List<TradeLogInfo> logs;

    @Data
    public static class TradePartyInfo {
        private String userId;
        private String nickname;
        private String avatarUrl;
    }

    @Data
    public static class TradeItemDetail {
        private String itemId;
        private String title;
        private String description;
        private String categoryId;
        private String categoryName;
        private BigDecimal price;
        private Integer conditionStar;
        private List<String> images;
        private String coverImage;
        private String tradeMode;
    }

    @Data
    public static class PaymentInfo {
        private PayChannel channel;
        private PayStatus payStatus;
        private String outTradeNo;
        private String providerTradeNo;
        private Date payExpireAt;
        private Date paidAt;
    }

    @Data
    public static class TradeLogInfo {
        private TradeLogAction action;
        private TradeStatus fromStatus;
        private TradeStatus toStatus;
        private String operatorId;
        private String operatorRole;
        private String message;
        private Date createdAt;
    }
}
