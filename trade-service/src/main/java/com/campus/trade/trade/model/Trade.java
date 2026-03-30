package com.campus.trade.trade.model;

import com.campus.trade.trade.enums.PayChannel;
import com.campus.trade.trade.enums.PayStatus;
import com.campus.trade.trade.enums.TradeStatus;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@Document(collection = "trades")
public class Trade {

    @Id
    private String id;

    private String tradeNo;

    private String itemId;
    private String buyerId;
    private String sellerId;

    /**
     * 成交金额
     */
    private BigDecimal amount;

    private TradeStatus status;
    private PayStatus payStatus;
    private PayChannel payChannel;

    /**
     * 商户订单号
     */
    private String outTradeNo;

    /**
     * 支付平台流水号
     */
    private String providerTradeNo;

    private Date paidAt;
    private Date payExpireAt;
    private Date deliveredAt;
    private Date confirmedAt;
    private Date completedAt;
    private Date cancelledAt;
    private Date refundingAt;
    private Date refundedAt;

    private String cancelReason;
    private String refundReason;

    private ItemSnapshot itemSnapshot;

    private Date createdAt;
    private Date updatedAt;

    @Data
    public static class ItemSnapshot {
        private String itemId;
        private String sellerId;
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
}
