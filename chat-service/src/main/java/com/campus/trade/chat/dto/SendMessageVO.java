package com.campus.trade.chat.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SendMessageVO {

    private String messageId;
    private String conversationId;
    private String senderId;
    private String type;
    private String content;
    private Long seq;
    private LocalDateTime createdAt;
}
