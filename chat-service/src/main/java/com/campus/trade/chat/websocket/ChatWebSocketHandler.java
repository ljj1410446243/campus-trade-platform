package com.campus.trade.chat.websocket;

import com.campus.trade.chat.dto.MessagePayload;
import com.campus.trade.chat.dto.SendMessageRequest;
import com.campus.trade.chat.dto.SendMessageVO;
import com.campus.trade.chat.dto.ws.WsRequest;
import com.campus.trade.chat.dto.ws.WsResponse;
import com.campus.trade.chat.entity.ChatConversation;
import com.campus.trade.chat.entity.ChatMessage;
import com.campus.trade.chat.service.ChatConversationService;
import com.campus.trade.chat.service.ChatMessageService;
import com.campus.trade.chat.service.ChatRealtimeNotifier;
import com.campus.trade.chat.util.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;
    private final WebSocketSessionManager sessionManager;
    private final ChatConversationService chatConversationService;
    private final ChatMessageService chatMessageService;
    private final ChatRealtimeNotifier chatRealtimeNotifier;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket连接已建立, sessionId={}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            WsRequest request = objectMapper.readValue(message.getPayload(), WsRequest.class);
            if (request.getType() == null || request.getType().isBlank()) {
                send(session, WsResponse.error(request.getRequestId(), "消息类型不能为空"));
                return;
            }

            switch (request.getType()) {
                case WsMessageType.AUTH -> handleAuth(session, request);
                case WsMessageType.TEXT, WsMessageType.IMAGE, WsMessageType.EMOJI -> handleMessage(session, request);
                case WsMessageType.READ -> handleRead(session, request);
                default -> send(session, WsResponse.error(request.getRequestId(), "不支持的消息类型"));
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息异常, sessionId={}", session.getId(), e);
            try {
                send(session, WsResponse.error(null, "消息处理失败"));
            } catch (Exception ignored) {
            }
        }
    }

    private void handleAuth(WebSocketSession session, WsRequest request) throws Exception {
        JsonNode data = request.getData();
        if (data == null || data.get("token") == null || data.get("token").asText().isBlank()) {
            send(session, WsResponse.error(request.getRequestId(), "token不能为空"));
            return;
        }

        String token = data.get("token").asText();
        String userId;
        try {
            userId = jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            log.warn("WebSocket AUTH失败, sessionId={}, 原因=token无效", session.getId());
            send(session, WsResponse.error(request.getRequestId(), "token无效"));
            return;
        }

        if (userId == null || userId.isBlank()) {
            log.warn("WebSocket AUTH失败, sessionId={}, 原因=userId为空", session.getId());
            send(session, WsResponse.error(request.getRequestId(), "token无效"));
            return;
        }

        sessionManager.bind(userId, session);
        log.info("WebSocket AUTH成功, sessionId={}, userId={}", session.getId(), userId);

        Map<String, Object> ackData = new HashMap<>();
        ackData.put("ok", true);
        ackData.put("userId", userId);
        send(session, WsResponse.ack(request.getRequestId(), ackData));
    }

    private void handleMessage(WebSocketSession session, WsRequest request) throws Exception {
        if (!sessionManager.isAuthenticated(session)) {
            send(session, WsResponse.error(request.getRequestId(), "未认证，请先发送AUTH消息"));
            return;
        }

        JsonNode data = request.getData();
        if (data == null) {
            send(session, WsResponse.error(request.getRequestId(), "消息体不能为空"));
            return;
        }

        String conversationId = textValue(data, "conversationId");
        if (conversationId == null || conversationId.isBlank()) {
            send(session, WsResponse.error(request.getRequestId(), "conversationId不能为空"));
            return;
        }

        String senderId = sessionManager.getUserIdBySessionId(session.getId());
        SendMessageRequest sendMessageRequest = buildSendMessageRequest(request.getType(), conversationId, data);
        ChatMessage savedMessage = chatMessageService.saveMessage(senderId, sendMessageRequest);
        SendMessageVO messageVO = chatMessageService.toSendMessageVO(savedMessage);

        Map<String, Object> ackData = new HashMap<>();
        ackData.put("ok", true);
        ackData.put("message", messageVO);
        send(session, WsResponse.ack(request.getRequestId(), ackData));

        ChatConversation conversation = chatConversationService.getById(conversationId);
        chatRealtimeNotifier.pushMessage(conversation, messageVO, senderId);
    }

    private SendMessageRequest buildSendMessageRequest(String type, String conversationId, JsonNode data) {
        SendMessageRequest request = new SendMessageRequest();
        request.setConversationId(conversationId);
        request.setType(type);
        request.setContent(textValue(data, "content"));

        if (WsMessageType.IMAGE.equals(type)) {
            JsonNode imageNode = data.get("image");
            JsonNode source = imageNode != null && !imageNode.isNull() ? imageNode : data;
            MessagePayload.ImagePayload imagePayload = new MessagePayload.ImagePayload();
            imagePayload.setFileId(textValue(source, "fileId"));
            imagePayload.setUrl(textValue(source, "url"));
            imagePayload.setPreviewUrl(textValue(source, "previewUrl"));
            imagePayload.setThumbnailUrl(textValue(source, "thumbnailUrl"));
            imagePayload.setWidth(intValue(source, "width"));
            imagePayload.setHeight(intValue(source, "height"));
            request.setImage(imagePayload);
        }

        if (WsMessageType.EMOJI.equals(type)) {
            MessagePayload.EmojiPayload emojiPayload = new MessagePayload.EmojiPayload();
            JsonNode emojiNode = data.get("emoji");
            if (emojiNode != null && !emojiNode.isNull()) {
                emojiPayload.setCode(textValue(emojiNode, "code"));
                emojiPayload.setDisplay(textValue(emojiNode, "display"));
            } else {
                emojiPayload.setCode(textValue(data, "code"));
                emojiPayload.setDisplay(textValue(data, "display"));
            }
            request.setEmoji(emojiPayload);
        }

        return request;
    }

    private void handleRead(WebSocketSession session, WsRequest request) throws Exception {
        if (!sessionManager.isAuthenticated(session)) {
            send(session, WsResponse.error(request.getRequestId(), "未认证，请先发送AUTH消息"));
            return;
        }

        JsonNode data = request.getData();
        if (data == null) {
            send(session, WsResponse.error(request.getRequestId(), "消息体不能为空"));
            return;
        }

        String conversationId = textValue(data, "conversationId");
        if (conversationId == null || conversationId.isBlank()) {
            send(session, WsResponse.error(request.getRequestId(), "conversationId不能为空"));
            return;
        }

        String currentUserId = sessionManager.getUserIdBySessionId(session.getId());
        chatConversationService.clearUnread(conversationId, currentUserId);

        Map<String, Object> ackData = new HashMap<>();
        ackData.put("ok", true);
        ackData.put("conversationId", conversationId);
        ackData.put("userId", currentUserId);
        ackData.put("lastSeq", longValue(data, "lastSeq"));
        send(session, WsResponse.ack(request.getRequestId(), ackData));
    }

    private String textValue(JsonNode data, String fieldName) {
        JsonNode node = data == null ? null : data.get(fieldName);
        return node == null || node.isNull() ? null : node.asText();
    }

    private Integer intValue(JsonNode data, String fieldName) {
        JsonNode node = data == null ? null : data.get(fieldName);
        return node == null || node.isNull() ? null : node.asInt();
    }

    private Long longValue(JsonNode data, String fieldName) {
        JsonNode node = data == null ? null : data.get(fieldName);
        return node == null || node.isNull() ? null : node.asLong();
    }

    private void send(WebSocketSession session, WsResponse response) throws Exception {
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionManager.removeSession(session);
        log.info("WebSocket连接关闭, sessionId={}, status={}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket传输异常, sessionId={}", session.getId(), exception);
        sessionManager.removeSession(session);
    }
}
