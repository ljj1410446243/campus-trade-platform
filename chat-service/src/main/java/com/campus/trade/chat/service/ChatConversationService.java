package com.campus.trade.chat.service;

import com.campus.trade.chat.dto.ConversationCreateVO;
import com.campus.trade.chat.dto.ConversationVO;
import com.campus.trade.chat.dto.CreateConversationRequest;
import com.campus.trade.chat.entity.ChatConversation;

import java.util.List;

public interface ChatConversationService {

  List<ConversationVO> getMyConversations(String currentUserId);

  ChatConversation createOrGetConversation(String currentUserId, CreateConversationRequest request);

  ChatConversation getById(String conversationId);

  void validateConversationMember(ChatConversation conversation, String currentUserId);

  void updateConversationAfterMessage(ChatConversation conversation, String senderId, String messagePreview);

  void clearUnread(String conversationId, String currentUserId);

  ConversationCreateVO buildConversationCreateVO(ChatConversation conversation, String currentUserId);
}
