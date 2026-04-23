package com.campus.trade.chat.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageVO {

  private String messageId;

  private String conversationId;

  private String senderId;

  private String type;

  private String content;

  private MessagePayload.ImagePayload image;

  private MessagePayload.EmojiPayload emoji;

  private MessagePayload.BargainPayload bargain;

  private Long seq;

  private LocalDateTime createdAt;
}
