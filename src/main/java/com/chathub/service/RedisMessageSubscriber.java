package com.chathub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

/**
 * Redis 訊息訂閱者
 * 功能：監聽 Redis 頻道，收到訊息後推送給對應的 WebSocket 連線
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisMessageSubscriber implements MessageListener {

    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    /**
     * 當 Redis 收到訊息時觸發
     * @param message Redis 訊息
     * @param pattern 訂閱模式（例如：user:*）
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            // 1. 解析頻道名稱（例如：user:abc-123）
            String channel = new String(message.getChannel());
            String messageBody = new String(message.getBody());

            log.debug("Received message from channel: {}", channel);

            // 2. 提取使用者 ID
            String userId = extractUserIdFromChannel(channel);
            if (userId == null) {
                log.warn("Cannot extract userId from channel: {}", channel);
                return;
            }

            // 3. 找到該使用者的 WebSocket 連線
            WebSocketSession session = sessionManager.getSession(userId);
            if (session == null || !session.isOpen()) {
                log.debug("User {} is not connected or session closed", userId);
                return;
            }

            // 4. 推送訊息給使用者
            session.sendMessage(new TextMessage(messageBody));
            log.debug("Message sent to user: {}", userId);

        } catch (Exception e) {
            log.error("Error processing Redis message", e);
        }
    }

    /**
     * 從頻道名稱提取使用者 ID
     * 例如：user:abc-123 → abc-123
     */
    private String extractUserIdFromChannel(String channel) {
        if (channel.startsWith("user:")) {
            return channel.substring(5);
        }
        return null;
    }
}