package com.campus.trade.chat.controller;

import com.campus.trade.chat.common.ApiResponse;
import com.campus.trade.chat.dto.BargainRecordVO;
import com.campus.trade.chat.dto.ConversationCreateVO;
import com.campus.trade.chat.dto.ConversationVO;
import com.campus.trade.chat.dto.CreateBargainRequest;
import com.campus.trade.chat.dto.CreateConversationRequest;
import com.campus.trade.chat.dto.MessageVO;
import com.campus.trade.chat.dto.SendMessageRequest;
import com.campus.trade.chat.dto.SendMessageVO;
import com.campus.trade.chat.entity.ChatConversation;
import com.campus.trade.chat.entity.ChatMessage;
import com.campus.trade.chat.service.ChatBargainService;
import com.campus.trade.chat.service.ChatConversationService;
import com.campus.trade.chat.service.ChatMessageService;
import com.campus.trade.chat.service.ChatRealtimeNotifier;
import com.campus.trade.chat.util.LoginUserHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatConversationService chatConversationService;
    private final ChatMessageService chatMessageService;
    private final ChatBargainService chatBargainService;
    private final ChatRealtimeNotifier chatRealtimeNotifier;
    private final LoginUserHelper loginUserHelper;

    @PostMapping("/conversations")
    public ApiResponse<ConversationCreateVO> createOrGetConversation(@Valid @RequestBody CreateConversationRequest request,
                                                                     HttpServletRequest httpServletRequest) {
        String currentUserId = loginUserHelper.getCurrentUserId(httpServletRequest);
        ChatConversation conversation = chatConversationService.createOrGetConversation(currentUserId, request);
        ConversationCreateVO vo = chatConversationService.buildConversationCreateVO(conversation, currentUserId);
        return ApiResponse.success(vo);
    }

    @GetMapping("/conversations")
    public ApiResponse<List<ConversationVO>> getMyConversations(HttpServletRequest httpServletRequest) {
        String currentUserId = loginUserHelper.getCurrentUserId(httpServletRequest);
        return ApiResponse.success(chatConversationService.getMyConversations(currentUserId));
    }

    @GetMapping("/messages/{conversationId}")
    public ApiResponse<List<MessageVO>> getMessages(@PathVariable String conversationId,
                                                    HttpServletRequest httpServletRequest) {
        String currentUserId = loginUserHelper.getCurrentUserId(httpServletRequest);
        return ApiResponse.success(chatMessageService.getMessagesByConversationId(conversationId, currentUserId));
    }

    @PostMapping("/messages")
    public ApiResponse<SendMessageVO> sendMessage(@Valid @RequestBody SendMessageRequest request,
                                                  HttpServletRequest httpServletRequest) {
        String currentUserId = loginUserHelper.getCurrentUserId(httpServletRequest);
        ChatMessage message = chatMessageService.saveMessage(currentUserId, request);
        SendMessageVO vo = chatMessageService.toSendMessageVO(message);

        ChatConversation conversation = chatConversationService.getById(request.getConversationId());
        chatRealtimeNotifier.pushMessage(conversation, vo, currentUserId);
        return ApiResponse.success(vo);
    }

    @PostMapping("/conversations/{conversationId}/bargains")
    public ApiResponse<BargainRecordVO> createBargain(@PathVariable String conversationId,
                                                      @Valid @RequestBody CreateBargainRequest request,
                                                      HttpServletRequest httpServletRequest) {
        String currentUserId = loginUserHelper.getCurrentUserId(httpServletRequest);
        return ApiResponse.success(chatBargainService.createBargain(conversationId, currentUserId, request));
    }

    @GetMapping("/conversations/{conversationId}/bargains")
    public ApiResponse<List<BargainRecordVO>> getBargains(@PathVariable String conversationId,
                                                          HttpServletRequest httpServletRequest) {
        String currentUserId = loginUserHelper.getCurrentUserId(httpServletRequest);
        return ApiResponse.success(chatBargainService.listBargains(conversationId, currentUserId));
    }

    @PostMapping("/bargains/{recordId}/accept")
    public ApiResponse<BargainRecordVO> acceptBargain(@PathVariable String recordId,
                                                      HttpServletRequest httpServletRequest) {
        String currentUserId = loginUserHelper.getCurrentUserId(httpServletRequest);
        return ApiResponse.success(chatBargainService.acceptBargain(recordId, currentUserId));
    }

    @PostMapping("/bargains/{recordId}/reject")
    public ApiResponse<BargainRecordVO> rejectBargain(@PathVariable String recordId,
                                                      HttpServletRequest httpServletRequest) {
        String currentUserId = loginUserHelper.getCurrentUserId(httpServletRequest);
        return ApiResponse.success(chatBargainService.rejectBargain(recordId, currentUserId));
    }

    @PostMapping("/conversations/{conversationId}/read")
    public ApiResponse<Void> clearUnread(@PathVariable String conversationId,
                                         HttpServletRequest httpServletRequest) {
        String currentUserId = loginUserHelper.getCurrentUserId(httpServletRequest);
        chatConversationService.clearUnread(conversationId, currentUserId);
        return ApiResponse.success(null);
    }
}
