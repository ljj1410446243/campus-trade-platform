package com.campus.trade.chat.dto;

import lombok.Data;

@Data
public class ConversationCreateVO {

    private String conversationId;
    private String itemId;

    private String buyerId;
    private String sellerId;

    private String targetUserId;
    private String targetNickname;
    private String targetAvatarUrl;
}
