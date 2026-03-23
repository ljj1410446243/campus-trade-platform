package com.campus.trade.chat.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class WebSocketSessionManager {

  /**
   * userId -> (sessionId -> session)
   */
  private final Map<String, Map<String, WebSocketSession>> userSessions = new ConcurrentHashMap<>();

  /**
   * sessionId -> userId
   */
  private final Map<String, String> sessionUserMapping = new ConcurrentHashMap<>();

  /**
   * 绑定用户与session
   */
  public void bind(String userId, WebSocketSession session) {
    userSessions.computeIfAbsent(userId, key -> new ConcurrentHashMap<>())
            .put(session.getId(), session);
    sessionUserMapping.put(session.getId(), userId);

    log.info("绑定WebSocket会话成功, userId={}, sessionId={}", userId, session.getId());
  }

  /**
   * 移除session
   */
  public void removeSession(WebSocketSession session) {
    if (session == null) {
      return;
    }

    String sessionId = session.getId();
    String userId = sessionUserMapping.remove(sessionId);
    if (userId == null) {
      return;
    }

    Map<String, WebSocketSession> sessionMap = userSessions.get(userId);
    if (sessionMap != null) {
      sessionMap.remove(sessionId);
      if (sessionMap.isEmpty()) {
        userSessions.remove(userId);
      }
    }

    log.info("移除WebSocket会话成功, userId={}, sessionId={}", userId, sessionId);
  }

  /**
   * 获取用户所有在线session
   */
  public List<WebSocketSession> getUserSessions(String userId) {
    Map<String, WebSocketSession> sessionMap = userSessions.get(userId);
    if (sessionMap == null || sessionMap.isEmpty()) {
      return List.of();
    }
    return new ArrayList<>(sessionMap.values());
  }

  /**
   * 判断用户是否在线
   */
  public boolean isOnline(String userId) {
    Map<String, WebSocketSession> sessionMap = userSessions.get(userId);
    return sessionMap != null && !sessionMap.isEmpty();
  }

  /**
   * 根据session获取绑定用户ID
   */
  public String getUserIdBySessionId(String sessionId) {
    return sessionUserMapping.get(sessionId);
  }

  /**
   * 判断当前session是否已认证
   */
  public boolean isAuthenticated(WebSocketSession session) {
    if (session == null) {
      return false;
    }
    return sessionUserMapping.containsKey(session.getId());
  }

  /**
   * 关闭某个用户的所有session（可选，当前先保留）
   */
  public void closeUserSessions(String userId) {
    List<WebSocketSession> sessions = getUserSessions(userId);
    for (WebSocketSession session : sessions) {
      try {
        session.close();
      } catch (IOException e) {
        log.warn("关闭WebSocket会话失败, userId={}, sessionId={}", userId, session.getId(), e);
      }
    }
  }
}
