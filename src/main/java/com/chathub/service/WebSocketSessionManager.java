package com.chathub.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 連線管理器
 * 功能：儲存 userId → WebSocketSession 的映射關係
 */
@Slf4j
@Service
public class WebSocketSessionManager {

    // 儲存 userId → WebSocketSession（使用 String 作為 Key，方便查詢）
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * 註冊連線
     * @param userId 使用者 ID（UUID）
     * @param session WebSocket Session
     */
    public void addSession(UUID userId, WebSocketSession session) {
        sessions.put(userId.toString(), session);
        log.info("WebSocket session added for user: {}, total sessions: {}", userId, sessions.size());
    }

    /**
     * 移除連線
     * @param userId 使用者 ID（UUID）
     */
    public void removeSession(UUID userId) {
        WebSocketSession removed = sessions.remove(userId.toString());
        if (removed != null) {
            log.info("WebSocket session removed for user: {}, total sessions: {}", userId, sessions.size());
        }
    }

    /**
     * 取得使用者的 WebSocket Session（UUID 版本）
     * @param userId 使用者 ID（UUID）
     * @return WebSocketSession（可能為 null）
     */
    public WebSocketSession getSession(UUID userId) {
        return sessions.get(userId.toString());
    }

    /**
     * 取得使用者的 WebSocket Session（String 版本）
     * @param userId 使用者 ID（String）
     * @return WebSocketSession（可能為 null）
     */
    public WebSocketSession getSession(String userId) {
        return sessions.get(userId);
    }

    /**
     * 檢查使用者是否在線
     * @param userId 使用者 ID（UUID）
     * @return true 如果連線存在且開啟中
     */
    public boolean isUserOnline(UUID userId) {
        WebSocketSession session = sessions.get(userId.toString());
        return session != null && session.isOpen();
    }

    /**
     * 檢查使用者是否在線（String 版本）
     */
    public boolean isUserOnline(String userId) {
        WebSocketSession session = sessions.get(userId);
        return session != null && session.isOpen();
    }

    /**
     * 取得目前所有在線使用者數量
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }
}