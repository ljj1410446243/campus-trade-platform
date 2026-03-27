package com.campus.trade.chat.service.impl;

import com.campus.trade.chat.common.BaseException;
import com.campus.trade.chat.dto.MessageVO;
import com.campus.trade.chat.entity.ChatConversation;
import com.campus.trade.chat.entity.ChatMessage;
import com.campus.trade.chat.repository.ChatMessageRepository;
import com.campus.trade.chat.service.ChatConversationService;
import com.campus.trade.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

  private final ChatMessageRepository chatMessageRepository;
  private final ChatConversationService chatConversationService;

  @Override
  public List<MessageVO> getMessagesByConversationId(String conversationId, String currentUserId) {
    ChatConversation conversation = chatConversationService.getById(conversationId);
    chatConversationService.validateConversationMember(conversation, currentUserId);

    List<ChatMessage> messages = chatMessageRepository.findByConversationIdOrderBySeqAsc(conversationId);

    return messages.stream().map(message -> {
      MessageVO vo = new MessageVO();
      vo.setMessageId(message.getId());
      vo.setConversationId(message.getConversationId());
      vo.setSenderId(message.getSenderId());
      vo.setType(message.getType());
      vo.setContent(message.getContent());
      vo.setSeq(message.getSeq());
      vo.setCreatedAt(message.getCreatedAt());
      return vo;
    }).toList();
  }

  @Override
  public ChatMessage saveMessage(String conversationId, String senderId, String type, String content) {
    ChatConversation conversation = chatConversationService.getById(conversationId);
    chatConversationService.validateConversationMember(conversation, senderId);

    if (type == null || type.isBlank()) {
      throw new BaseException(400, "消息类型不能为空");
    }
    if (content == null || content.isBlank()) {
      throw new BaseException(400, "消息内容不能为空");
    }

    Long seq = nextSeq(conversationId);

    ChatMessage message = new ChatMessage();
    message.setConversationId(conversationId);
    message.setSenderId(senderId);
    message.setType(type);
    message.setContent(content);
    message.setSeq(seq);
    message.setCreatedAt(LocalDateTime.now());

    ChatMessage saved = chatMessageRepository.save(message);

    String preview = buildMessagePreview(type, content);
    chatConversationService.updateConversationAfterMessage(conversation, senderId, preview);

    return saved;
  }

  @Override
  public Long nextSeq(String conversationId) {
    return chatMessageRepository.findTopByConversationIdOrderBySeqDesc(conversationId)
            .map(msg -> msg.getSeq() + 1)
            .orElse(1L);
  }

  private String buildMessagePreview(String type, String content) {
    if ("IMAGE".equalsIgnoreCase(type)) {
      return "[图片]";
    }
    if (content.length() > 50) {
      return content.substring(0, 50);
    }
    return content;
  }
}
