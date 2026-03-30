package com.campus.trade.trade.dto;

import com.campus.trade.trade.enums.PayChannel;
import com.campus.trade.trade.enums.PayStatus;
import com.campus.trade.trade.enums.TradeStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class TradeListResponse {

    private String tradeId;
    private String tradeNo;
    private BigDecimal amount;
    private TradeStatus status;
    private PayStatus payStatus;
    private PayChannel payChannel;
    private Date createdAt;
    private Date paidAt;
    private Date payExpireAt;
    private TradeItemInfo item;
    private TradeUserInfo buyer;
    private TradeUserInfo seller;

    @Data
    public static class TradeItemInfo {
        private String itemId;
        private String title;
        private String coverImage;
        private BigDecimal price;
        private Integer conditionStar;
    }

    @Data
    public static class TradeUserInfo {
        private String userId;
        private String nickname;
        private String avatarUrl;
    }
}
