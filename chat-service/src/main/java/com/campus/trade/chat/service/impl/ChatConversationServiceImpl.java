package com.campus.trade.chat.service.impl;

import com.campus.trade.chat.common.BaseException;
import com.campus.trade.chat.dto.ConversationCreateVO;
import com.campus.trade.chat.dto.ConversationVO;
import com.campus.trade.chat.dto.CreateConversationRequest;
import com.campus.trade.chat.entity.ChatConversation;
import com.campus.trade.chat.entity.Item;
import com.campus.trade.chat.entity.User;
import com.campus.trade.chat.repository.ChatConversationRepository;
import com.campus.trade.chat.repository.ItemRepository;
import com.campus.trade.chat.repository.UserRepository;
import com.campus.trade.chat.service.ChatConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatConversationServiceImpl implements ChatConversationService {

  private final ChatConversationRepository chatConversationRepository;
  private final UserRepository userRepository;
  private final ItemRepository itemRepository;

  @Override
  public List<ConversationVO> getMyConversations(String currentUserId) {
    List<ChatConversation> conversations =
            chatConversationRepository.findByBuyerIdOrSellerIdOrderByUpdatedAtDesc(currentUserId, currentUserId);

    return conversations.stream().map(conversation -> buildConversationVO(conversation, currentUserId)).toList();
  }

  @Override
  public ChatConversation createOrGetConversation(String currentUserId, CreateConversationRequest request) {
    String buyerId = currentUserId;
    String sellerId = request.getSellerId();

    if (buyerId.equals(sellerId)) {
      throw new BaseException(400, "不能和自己创建会话");
    }

    return chatConversationRepository
            .findByItemIdAndBuyerIdAndSellerId(request.getItemId(), buyerId, sellerId)
            .orElseGet(() -> {
              ChatConversation conversation = new ChatConversation();
              conversation.setItemId(request.getItemId());
              conversation.setBuyerId(buyerId);
              conversation.setSellerId(sellerId);
              conversation.setLastMessage("");
              conversation.setBuyerUnread(0);
              conversation.setSellerUnread(0);
              conversation.setCreatedAt(LocalDateTime.now());
              conversation.setUpdatedAt(LocalDateTime.now());
              return chatConversationRepository.save(conversation);
            });
  }

  @Override
  public ChatConversation getById(String conversationId) {
    return chatConversationRepository.findById(conversationId)
            .orElseThrow(() -> new BaseException(404, "会话不存在"));
  }

  @Override
  public void validateConversationMember(ChatConversation conversation, String currentUserId) {
    boolean isBuyer = currentUserId.equals(conversation.getBuyerId());
    boolean isSeller = currentUserId.equals(conversation.getSellerId());

    if (!isBuyer && !isSeller) {
      throw new BaseException(403, "无权访问该会话");
    }
  }

  @Override
  public void updateConversationAfterMessage(ChatConversation conversation, String senderId, String messagePreview) {
    conversation.setLastMessage(messagePreview);
    conversation.setUpdatedAt(LocalDateTime.now());

    if (senderId.equals(conversation.getBuyerId())) {
      Integer unread = conversation.getSellerUnread() == null ? 0 : conversation.getSellerUnread();
      conversation.setSellerUnread(unread + 1);
    } else if (senderId.equals(conversation.getSellerId())) {
      Integer unread = conversation.getBuyerUnread() == null ? 0 : conversation.getBuyerUnread();
      conversation.setBuyerUnread(unread + 1);
    }

    chatConversationRepository.save(conversation);
  }

  @Override
  public void clearUnread(String conversationId, String currentUserId) {
    ChatConversation conversation = getById(conversationId);
    validateConversationMember(conversation, currentUserId);

    if (currentUserId.equals(conversation.getBuyerId())) {
      conversation.setBuyerUnread(0);
    } else if (currentUserId.equals(conversation.getSellerId())) {
      conversation.setSellerUnread(0);
    }

    chatConversationRepository.save(conversation);
  }

  @Override
  public ConversationCreateVO buildConversationCreateVO(ChatConversation conversation, String currentUserId) {
    validateConversationMember(conversation, currentUserId);

    String targetUserId = resolveTargetUserId(conversation, currentUserId);
    User targetUser = userRepository.findById(targetUserId).orElse(null);

    ConversationCreateVO vo = new ConversationCreateVO();
    vo.setConversationId(conversation.getId());
    vo.setItemId(conversation.getItemId());
    vo.setBuyerId(conversation.getBuyerId());
    vo.setSellerId(conversation.getSellerId());
    vo.setTargetUserId(targetUserId);

    if (targetUser != null) {
      vo.setTargetNickname(resolveDisplayName(targetUser));
      vo.setTargetAvatarUrl(targetUser.getAvatarUrl());
    }

    return vo;
  }

  private ConversationVO buildConversationVO(ChatConversation conversation, String currentUserId) {
    String targetUserId = resolveTargetUserId(conversation, currentUserId);
    Integer unreadCount = resolveUnreadCount(conversation, currentUserId);

    Optional<User> targetUserOpt = userRepository.findById(targetUserId);
    Optional<Item> itemOpt = itemRepository.findById(conversation.getItemId());

    ConversationVO vo = new ConversationVO();
    vo.setConversationId(conversation.getId());
    vo.setItemId(conversation.getItemId());
    vo.setBuyerId(conversation.getBuyerId());
    vo.setSellerId(conversation.getSellerId());
    vo.setTargetUserId(targetUserId);
    vo.setUnreadCount(unreadCount == null ? 0 : unreadCount);
    vo.setLastMessage(conversation.getLastMessage());
    vo.setUpdatedAt(conversation.getUpdatedAt());

    targetUserOpt.ifPresent(user -> {
      vo.setTargetNickname(resolveDisplayName(user));
      vo.setTargetAvatarUrl(user.getAvatarUrl());
    });

    itemOpt.ifPresent(item -> {
      vo.setItemTitle(item.getTitle());
      vo.setItemPrice(item.getPrice());
    });

    return vo;
  }

  private String resolveTargetUserId(ChatConversation conversation, String currentUserId) {
    if (currentUserId.equals(conversation.getBuyerId())) {
      return conversation.getSellerId();
    }
    return conversation.getBuyerId();
  }

  private Integer resolveUnreadCount(ChatConversation conversation, String currentUserId) {
    if (currentUserId.equals(conversation.getBuyerId())) {
      return conversation.getBuyerUnread();
    }
    return conversation.getSellerUnread();
  }

  private String resolveDisplayName(User user) {
    if (user.getNickname() != null && !user.getNickname().isBlank()) {
      return user.getNickname();
    }
    return user.getUsername();
  }
}
