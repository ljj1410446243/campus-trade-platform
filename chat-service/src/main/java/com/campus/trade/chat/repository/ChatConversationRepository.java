package com.campus.trade.chat.repository;

import com.campus.trade.chat.entity.ChatConversation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChatConversationRepository extends MongoRepository<ChatConversation, String> {

  /**
   * 查询某个用户参与的全部会话
   */
  List<ChatConversation> findByBuyerIdOrSellerIdOrderByUpdatedAtDesc(String buyerId, String sellerId);

  /**
   * 查询某个商品下，某买家和某卖家的唯一会话
   */
  Optional<ChatConversation> findByItemIdAndBuyerIdAndSellerId(String itemId, String buyerId, String sellerId);
}
