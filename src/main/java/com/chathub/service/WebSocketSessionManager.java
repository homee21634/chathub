package com.chathub.service;

import com.chathub.dto.WebSocketMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 連線管理器
 * 管理所有活躍的 WebSocket 連線
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSessionManager {

    private final ObjectMapper objectMapper;

    /**
     * 使用者 ID → WebSocket Session 映射
     * key: userId, value: WebSocketSession
     */
    private final Map<UUID, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * Session ID → 使用者 ID 映射（反向查詢用）
     * key: sessionId, value: userId
     */
    private final Map<String, UUID> sessionToUser = new ConcurrentHashMap<>();

    /**
     * 註冊新連線
     */
    public void addSession(UUID userId, WebSocketSession session) {
        // 移除舊連線（如果存在）
        WebSocketSession oldSession = sessions.get(userId);
        if (oldSession != null && oldSession.isOpen()) {
            try {
                oldSession.close();
                log.info("關閉使用者 {} 的舊連線", userId);
            } catch (IOException e) {
                log.error("關閉舊連線失敗：{}", e.getMessage());
            }
        }

        // 註冊新連線
        sessions.put(userId, session);
        sessionToUser.put(session.getId(), userId);

        log.info("使用者 {} 已連線，Session ID: {}, 目前線上人數: {}",
                 userId, session.getId(), sessions.size());
    }

    /**
     * 移除連線
     */
    public void removeSession(UUID userId) {
        WebSocketSession session = sessions.remove(userId);
        if (session != null) {
            sessionToUser.remove(session.getId());
            log.info("使用者 {} 已斷線，目前線上人數: {}", userId, sessions.size());
        }
    }

    /**
     * 根據 Session ID 移除連線
     */
    public void removeSessionById(String sessionId) {
        UUID userId = sessionToUser.remove(sessionId);
        if (userId != null) {
            sessions.remove(userId);
            log.info("Session {} 已斷線（使用者 {}），目前線上人數: {}",
                     sessionId, userId, sessions.size());
        }
    }

    /**
     * 取得使用者的 Session
     */
    public WebSocketSession getSession(UUID userId) {
        return sessions.get(userId);
    }

    /**
     * 根據 Session ID 取得使用者 ID
     */
    public UUID getUserIdBySessionId(String sessionId) {
        return sessionToUser.get(sessionId);
    }

    /**
     * 檢查使用者是否線上
     */
    public boolean isUserOnline(UUID userId) {
        WebSocketSession session = sessions.get(userId);
        return session != null && session.isOpen();
    }

    /**
     * 發送訊息給指定使用者
     */
    public boolean sendMessage(UUID userId, WebSocketMessage message) {
        WebSocketSession session = sessions.get(userId);

        if (session == null || !session.isOpen()) {
            log.warn("無法發送訊息：使用者 {} 不在線上", userId);
            return false;
        }

        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
            log.debug("訊息已發送給使用者 {}：{}", userId, message.getType());
            return true;
        } catch (IOException e) {
            log.error("發送訊息失敗：{}", e.getMessage());
            // 移除失效的連線
            removeSession(userId);
            return false;
        }
    }

    /**
     * 廣播訊息給所有線上使用者（選配）
     */
    public void broadcast(WebSocketMessage message) {
        sessions.forEach((userId, session) -> {
            sendMessage(userId, message);
        });
        log.info("廣播訊息給 {} 個使用者", sessions.size());
    }

    /**
     * 取得線上使用者數量
     */
    public int getOnlineCount() {
        return sessions.size();
    }

    /**
     * 取得所有線上使用者 ID
     */
    public Set<UUID> getOnlineUserIds() {
        return new HashSet<>(sessions.keySet());
    }
}
