package com.chathub.handler;

import com.chathub.dto.WebSocketMessage;
import com.chathub.entity.Message;
import com.chathub.service.FriendshipService;
import com.chathub.service.MessageService;
import com.chathub.service.WebSocketSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.UUID;

/**
 * WebSocket 訊息處理器
 * 處理所有 WebSocket 連線與訊息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    /**
     * 連線建立後
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 從 Session 屬性取得使用者資訊（由握手攔截器設定）
        UUID userId = (UUID) session.getAttributes().get("userId");
        String username = (String) session.getAttributes().get("username");

        if (userId == null) {
            log.error("連線建立失敗：缺少使用者資訊");
            session.close();
            return;
        }

        // 註冊連線
        sessionManager.addSession(userId, session);

        // 發送連線成功訊息
        WebSocketMessage welcomeMsg = WebSocketMessage.connectionEstablished(userId);
        sessionManager.sendMessage(userId, welcomeMsg);

        log.info("✅ WebSocket 連線建立：{} ({})", username, userId);
    }

    /**
     * 接收訊息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("收到訊息：{}", payload);

        try {
            // 解析訊息
            WebSocketMessage wsMessage = objectMapper.readValue(payload, WebSocketMessage.class);
            UUID userId = (UUID) session.getAttributes().get("userId");

            if (userId == null) {
                log.error("處理訊息失敗：使用者未認證");
                return;
            }

            // 根據訊息類型分發處理
            switch (wsMessage.getType()) {
                case PING:
                    handlePing(userId);
                    break;
                case SEND_MESSAGE:
                    handleSendMessage(userId, wsMessage);
                    break;
                case TYPING_START:
                    handleTypingStart(userId, wsMessage);
                    break;
                case TYPING_STOP:
                    handleTypingStop(userId, wsMessage);
                    break;
                case MESSAGE_READ:
                    handleMessageRead(userId, wsMessage);
                    break;
                default:
                    log.warn("未知的訊息類型：{}", wsMessage.getType());
            }

        } catch (Exception e) {
            log.error("處理訊息異常：{}", e.getMessage(), e);

            // 發送錯誤訊息給客戶端
            UUID userId = (UUID) session.getAttributes().get("userId");
            if (userId != null) {
                WebSocketMessage errorMsg = WebSocketMessage.error(
                    "PARSE_ERROR",
                    "訊息格式錯誤"
                );
                sessionManager.sendMessage(userId, errorMsg);
            }
        }
    }

    /**
     * 連線關閉後
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        UUID userId = (UUID) session.getAttributes().get("userId");
        String username = (String) session.getAttributes().get("username");

        if (userId != null) {
            sessionManager.removeSession(userId);
            log.info("❌ WebSocket 連線關閉：{} ({})，原因：{}",
                     username, userId, status.getReason());
        }
    }

    /**
     * 傳輸異常處理
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        UUID userId = (UUID) session.getAttributes().get("userId");
        log.error("WebSocket 傳輸異常（使用者 {}）：{}", userId, exception.getMessage());

        if (userId != null) {
            sessionManager.removeSession(userId);
        }
    }

    // ==================== 訊息處理方法 ====================

    /**
     * 處理心跳訊息
     */
    private void handlePing(UUID userId) {
        WebSocketMessage pong = WebSocketMessage.pong();
        sessionManager.sendMessage(userId, pong);
        log.debug("心跳回應：{}", userId);
    }

    private final MessageService messageService;
    private final FriendshipService friendshipService;

    /**
     * 處理發送訊息
     */
    private void handleSendMessage(UUID senderId, WebSocketMessage wsMessage) {
        try {
            Map<String, Object> payload = wsMessage.getPayload();

            // 提取參數
            String recipientIdStr = (String) payload.get("recipientId");
            String content = (String) payload.get("content");
            String clientMessageId = (String) payload.get("clientMessageId");

            if (recipientIdStr == null || content == null || content.trim().isEmpty()) {
                sessionManager.sendMessage(senderId, WebSocketMessage.error(
                    "INVALID_PARAMS", "缺少必要參數"
                ));
                return;
            }

            UUID recipientId = UUID.fromString(recipientIdStr);

            // 驗證：是否為好友
            if (!friendshipService.areFriends(senderId, recipientId)) {
                sessionManager.sendMessage(senderId, WebSocketMessage.error(
                    "NOT_FRIENDS", "只能發送訊息給好友"
                ));
                return;
            }

            // 驗證：內容長度
            if (content.length() > 2000) {
                sessionManager.sendMessage(senderId, WebSocketMessage.error(
                    "CONTENT_TOO_LONG", "訊息內容不能超過 2000 字元"
                ));
                return;
            }

            // 儲存訊息到 MongoDB
            Message savedMessage = messageService.saveMessage(
                senderId,
                recipientId,
                content.trim(),
                clientMessageId
            );

            // 發送「訊息送達」確認給發送者
            WebSocketMessage deliveredMsg = WebSocketMessage.messageDelivered(
                savedMessage.getMessageId(),
                clientMessageId
            );
            sessionManager.sendMessage(senderId, deliveredMsg);

            // 推送新訊息給接收者（如果在線）
            if (sessionManager.isUserOnline(recipientId)) {
                WebSocketMessage newMsg = WebSocketMessage.newMessage(
                    savedMessage.getMessageId(),
                    savedMessage.getConversationId(),
                    savedMessage.getSenderId(),
                    savedMessage.getSenderUsername(),
                    savedMessage.getContent(),
                    savedMessage.getTimestamp()
                );
                sessionManager.sendMessage(recipientId, newMsg);
                log.info("訊息已推送給接收者：{}", recipientId);
            } else {
                log.info("接收者不在線上：{}，訊息已儲存", recipientId);
            }

        } catch (Exception e) {
            log.error("處理發送訊息失敗：{}", e.getMessage(), e);
            sessionManager.sendMessage(senderId, WebSocketMessage.error(
                "SEND_FAILED", "發送訊息失敗"
            ));
        }
    }

    /**
     * 處理開始輸入
     */
    private void handleTypingStart(UUID userId, WebSocketMessage message) {
        try {
            Map<String, Object> payload = message.getPayload();
            String recipientIdStr = (String) payload.get("recipientId");

            if (recipientIdStr == null) {
                return;
            }

            UUID recipientId = UUID.fromString(recipientIdStr);
            String username = (String) sessionManager.getSession(userId)
                                                     .getAttributes().get("username");

            // 推送給接收者
            if (sessionManager.isUserOnline(recipientId)) {
                WebSocketMessage typingMsg = WebSocketMessage.userTyping(
                    userId, username, true
                );
                sessionManager.sendMessage(recipientId, typingMsg);
                log.debug("使用者 {} 正在輸入，通知 {}", username, recipientId);
            }

        } catch (Exception e) {
            log.error("處理正在輸入失敗：{}", e.getMessage());
        }
    }

    /**
     * 處理停止輸入
     */
    private void handleTypingStop(UUID userId, WebSocketMessage message) {
        try {
            Map<String, Object> payload = message.getPayload();
            String recipientIdStr = (String) payload.get("recipientId");

            if (recipientIdStr == null) {
                return;
            }

            UUID recipientId = UUID.fromString(recipientIdStr);
            String username = (String) sessionManager.getSession(userId)
                                                     .getAttributes().get("username");

            // 推送給接收者
            if (sessionManager.isUserOnline(recipientId)) {
                WebSocketMessage typingMsg = WebSocketMessage.userTyping(
                    userId, username, false
                );
                sessionManager.sendMessage(recipientId, typingMsg);
                log.debug("使用者 {} 停止輸入，通知 {}", username, recipientId);
            }

        } catch (Exception e) {
            log.error("處理停止輸入失敗：{}", e.getMessage());
        }
    }

    /**
     * 處理訊息已讀
     */
    private void handleMessageRead(UUID userId, WebSocketMessage message) {
        try {
            Map<String, Object> payload = message.getPayload();
            String conversationId = (String) payload.get("conversationId");
            String messageId = (String) payload.get("messageId");

            if (conversationId == null) {
                return;
            }

            // 標記訊息為已讀
            messageService.markMessagesAsRead(conversationId, userId);

            // 發送已讀回執給對方（取得對話參與者）
            String[] participants = conversationId.split("_");
            UUID otherUserId = participants[0].equals(userId.toString())
                ? UUID.fromString(participants[1])
                : UUID.fromString(participants[0]);

            if (sessionManager.isUserOnline(otherUserId)) {
                WebSocketMessage readReceipt = WebSocketMessage.messageReadReceipt(
                    conversationId, messageId, userId
                );
                sessionManager.sendMessage(otherUserId, readReceipt);
                log.info("已讀回執已發送給使用者 {}", otherUserId);
            }

        } catch (Exception e) {
            log.error("處理訊息已讀失敗：{}", e.getMessage());
        }
    }
}