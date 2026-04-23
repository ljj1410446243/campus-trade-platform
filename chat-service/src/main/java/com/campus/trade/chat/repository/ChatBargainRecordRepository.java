package com.campus.trade.chat.repository;

import com.campus.trade.chat.entity.ChatBargainRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChatBargainRecordRepository extends MongoRepository<ChatBargainRecord, String> {

    List<ChatBargainRecord> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    Optional<ChatBargainRecord> findTopByConversationIdAndStatusOrderByCreatedAtDesc(String conversationId, String status);
}
