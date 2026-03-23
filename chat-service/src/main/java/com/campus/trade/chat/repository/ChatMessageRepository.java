package com.campus.trade.chat.repository;

import com.campus.trade.chat.entity.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

  /**
   * 查询会话下全部消息，按 seq 正序
   */
  List<ChatMessage> findByConversationIdOrderBySeqAsc(String conversationId);

  /**
   * 查询会话下最大的 seq
   */
  Optional<ChatMessage> findTopByConversationIdOrderBySeqDesc(String conversationId);
}
