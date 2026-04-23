package com.campus.trade.chat.service;

import com.campus.trade.chat.dto.MessageVO;
import com.campus.trade.chat.dto.MessagePayload;
import com.campus.trade.chat.dto.SendMessageRequest;
import com.campus.trade.chat.dto.SendMessageVO;
import com.campus.trade.chat.entity.ChatMessage;

import java.util.List;

public interface ChatMessageService {

  /**
   * 查询某会话历史消息
   */
  List<MessageVO> getMessagesByConversationId(String conversationId, String currentUserId);

  /**
   * 保存消息
   */
  ChatMessage saveMessage(String senderId, SendMessageRequest request);

  ChatMessage saveBargainMessage(String conversationId,
                                 String senderId,
                                 MessagePayload.BargainPayload bargainPayload);

  /**
   * 获取下一条消息序号
   */
  Long nextSeq(String conversationId);

  MessageVO toMessageVO(ChatMessage message);

  SendMessageVO toSendMessageVO(ChatMessage message);
}
