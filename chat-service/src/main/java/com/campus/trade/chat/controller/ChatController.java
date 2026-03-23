package com.campus.trade.chat.controller;

import com.campus.trade.chat.common.ApiResponse;
import com.campus.trade.chat.dto.ConversationCreateVO;
import com.campus.trade.chat.dto.ConversationVO;
import com.campus.trade.chat.dto.CreateConversationRequest;
import com.campus.trade.chat.dto.MessageVO;
import com.campus.trade.chat.dto.SendMessageRequest;
import com.campus.trade.chat.dto.SendMessageVO;
import com.campus.trade.chat.entity.ChatConversation;
import com.campus.trade.chat.entity.ChatMessage;
import com.campus.trade.chat.service.ChatConversationService;
import com.campus.trade.chat.service.ChatMessageService;
import com.campus.trade.chat.util.LoginUserHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

  private final ChatConversationService chatConversationService;
  private final ChatMessageService chatMessageService;
  private final LoginUserHelper loginUserHelper;

  /**
   * 创建或获取会话
   */
  @PostMapping("/conversations")
  public ApiResponse<ConversationCreateVO> createOrGetConversation(@Valid @RequestBody CreateConversationRequest request,
                                                                   HttpServletRequest httpServletRequest) {
    String currentUserId = loginUserHelper.getCurrentUserId(httpServletRequest);

    ChatConversation conversation = chatConversationService.createOrGetConversation(currentUserId, request);
    ConversationCreateVO vo = chatConversationService.buildConversationCreateVO(conversation, currentUserId);

    return ApiResponse.success(vo);
  }

  /**
   * 获取我的会话列表
   */
  @GetMapping("/conversations")
  public ApiResponse<List<ConversationVO>> getMyConversations(HttpServletRequest httpServletRequest) {
    String currentUserId = loginUserHelper.getCurrentUserId(httpServletRequest);
    List<ConversationVO> list = chatConversationService.getMyConversations(currentUserId);
    return ApiResponse.success(list);
  }

  /**
   * 获取会话历史消息
   */
  @GetMapping("/messages/{conversationId}")
  public ApiResponse<List<MessageVO>> getMessages(@PathVariable String conversationId,
                                                  HttpServletRequest httpServletRequest) {
    String currentUserId = loginUserHelper.getCurrentUserId(httpServletRequest);
    List<MessageVO> list = chatMessageService.getMessagesByConversationId(conversationId, currentUserId);
    return ApiResponse.success(list);
  }

  /**
   * REST方式发送消息（用于先测试业务链路）
   */
  @PostMapping("/messages")
  public ApiResponse<SendMessageVO> sendMessage(@Valid @RequestBody SendMessageRequest request,
                                                HttpServletRequest httpServletRequest) {
    String currentUserId = loginUserHelper.getCurrentUserId(httpServletRequest);

    ChatMessage message = chatMessageService.saveMessage(
            request.getConversationId(),
            currentUserId,
            request.getType(),
            request.getContent()
    );

    SendMessageVO vo = new SendMessageVO();
    vo.setMessageId(message.getId());
    vo.setConversationId(message.getConversationId());
    vo.setSenderId(message.getSenderId());
    vo.setType(message.getType());
    vo.setContent(message.getContent());
    vo.setSeq(message.getSeq());
    vo.setCreatedAt(message.getCreatedAt());

    return ApiResponse.success(vo);
  }

  /**
   * 清空当前用户在该会话下的未读数
   */
  @PostMapping("/conversations/{conversationId}/read")
  public ApiResponse<Void> clearUnread(@PathVariable String conversationId,
                                       HttpServletRequest httpServletRequest) {
    String currentUserId = loginUserHelper.getCurrentUserId(httpServletRequest);
    chatConversationService.clearUnread(conversationId, currentUserId);
    return ApiResponse.success(null);
  }
}
