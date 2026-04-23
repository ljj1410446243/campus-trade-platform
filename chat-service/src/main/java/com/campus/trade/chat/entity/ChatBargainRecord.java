package com.campus.trade.chat.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Document(collection = "chat_bargain_records")
@CompoundIndex(name = "conversation_status_created_idx", def = "{'conversationId': 1, 'status': 1, 'createdAt': -1}")
public class ChatBargainRecord {

    @Id
    private String id;

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
