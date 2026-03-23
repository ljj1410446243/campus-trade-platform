package com.campus.trade.chat.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "chat_messages")
public class ChatMessage {

  @Id
  private String id;

  /**
   * 所属会话ID
   */
  private String conversationId;

  /**
   * 发送者ID
   */
  private String senderId;

  /**
   * 消息类型：TEXT / IMAGE
   */
  private String type;

  /**
   * 消息内容
   */
  private String content;

  /**
   * 会话内递增序号
   */
  private Long seq;

  /**
   * 发送时间
   */
  private LocalDateTime createdAt;
}
