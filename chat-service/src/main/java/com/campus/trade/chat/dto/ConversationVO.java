package com.campus.trade.chat.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ConversationVO {

  private String conversationId;

  private String itemId;
  private String itemTitle;
  private BigDecimal itemPrice;

  private String buyerId;
  private String sellerId;

  private String targetUserId;
  private String targetNickname;
  private String targetAvatarUrl;

  private String lastMessage;
  private Integer unreadCount;
  private LocalDateTime updatedAt;
}
