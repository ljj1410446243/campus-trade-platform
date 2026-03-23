package com.campus.trade.chat.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "chat_conversations")
public class ChatConversation {

  @Id
  private String id;

  /**
   * 关联商品ID
   */
  private String itemId;

  /**
   * 买家ID
   */
  private String buyerId;

  /**
   * 卖家ID
   */
  private String sellerId;

  /**
   * 最后一条消息预览
   */
  private String lastMessage;

  /**
   * 买家未读数
   */
  private Integer buyerUnread;

  /**
   * 卖家未读数
   */
  private Integer sellerUnread;

  /**
   * 创建时间
   */
  private LocalDateTime createdAt;

  /**
   * 更新时间
   */
  private LocalDateTime updatedAt;
}
