package com.campus.trade.chat.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BargainRecordVO {

    private String recordId;
    private String conversationId;
    private String itemId;
    private String buyerId;
    private String sellerId;
    private String initiatorId;
    private BigDecimal price;
    private String status;
    private String parentRecordId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime respondedAt;
    private String respondedBy;
    private String messageId;
}
