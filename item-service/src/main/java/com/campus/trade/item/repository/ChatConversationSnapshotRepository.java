package com.campus.trade.item.repository;

import com.campus.trade.item.model.ChatConversationSnapshot;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatConversationSnapshotRepository extends MongoRepository<ChatConversationSnapshot, String> {

    List<ChatConversationSnapshot> findByBuyerIdOrderByUpdatedAtDesc(String buyerId, Pageable pageable);

    List<ChatConversationSnapshot> findByUpdatedAtAfter(LocalDateTime updatedAt);
}
