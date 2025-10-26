package com.chathub.handler;

import com.chathub.dto.WebSocketMessage;
import com.chathub.entity.Message;
import com.chathub.service.MessageService;
import com.chathub.service.RedisMessagePublisher;
import com.chathub.service.WebSocketSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.UUID;

/**
 * WebSocket 訊息處理器（整合 Redis Pub/Sub）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final MessageService messageService;
    private final WebSocketSessionManager sessionManager;
    private final RedisMessagePublisher redisPublisher;
    private final ObjectMapper objectMapper;

    /**
     * 連線建立時
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 從 session 屬性取得 userId（在攔截器設定）
        String userIdStr = (String) session.getAttributes().get("userId");

        if (userIdStr != null) {
            UUID userId = UUID.fromString(userIdStr);

            // 註冊連線到管理器
            sessionManager.addSession(userId, session);
            log.info("User {} connected", userId);

            // 發送連線成功訊息
            WebSocketMessage connectMsg = WebSocketMessage.builder()
                                                          .type(WebSocketMessage.MessageType.CONNECTION_ESTABLISHED)
                                                          .payload(Map.of("userId", userIdStr))
                                                          .build();
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(connectMsg)));
        }
    }

    /**
     * 收到訊息時（關鍵修改！）
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String userIdStr = (String) session.getAttributes().get("userId");
        UUID userId = UUID.fromString(userIdStr);

        try {
            // 解析訊息
            WebSocketMessage wsMessage = objectMapper.readValue(message.getPayload(), WebSocketMessage.class);

            switch (wsMessage.getType()) {
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
                    log.warn("Unknown message type: {}", wsMessage.getType());
            }

        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
            sendErrorMessage(session, "處理訊息時發生錯誤");
        }
    }

    /**
     * 處理發送訊息（核心修改！）
     */
    private void handleSendMessage(UUID senderId, WebSocketMessage wsMessage) {
        try {
            Map<String, Object> payload = wsMessage.getPayload();
            String recipientIdStr = (String) payload.get("recipientId");
            UUID recipientId = UUID.fromString(recipientIdStr);
            String content = (String) payload.get("content");
            String clientMessageId = (String) payload.get("clientMessageId");

            // 1. 儲存訊息到 MongoDB
            Message savedMessage = messageService.saveMessage(
                senderId, recipientId, content, clientMessageId);

            // 2. 建立要推送的訊息
            Map<String, Object> messagePayload = Map.of(
                "messageId", savedMessage.getMessageId(),
                "conversationId", savedMessage.getConversationId(),
                "senderId", savedMessage.getSenderId().toString(),
                "senderUsername", savedMessage.getSenderUsername(),
                "content", savedMessage.getContent(),
                "timestamp", savedMessage.getTimestamp().toString()
            );

            WebSocketMessage notification = WebSocketMessage.builder()
                                                            .type(WebSocketMessage.MessageType.NEW_MESSAGE)
                                                            .payload(messagePayload)
                                                            .build();

            // 3. 【關鍵】透過 Redis 發布訊息（讓所有 Pod 都收到）
            redisPublisher.publishToUser(recipientId, notification);

            // 4. 發送送達確認給發送者
            WebSocketMessage deliveredMsg = WebSocketMessage.builder()
                                                            .type(WebSocketMessage.MessageType.MESSAGE_DELIVERED)
                                                            .payload(Map.of(
                                                                "messageId", savedMessage.getMessageId(),
                                                                "clientMessageId", clientMessageId
                                                            ))
                                                            .build();

            // 發送者在本 Pod，直接推送
            WebSocketSession senderSession = sessionManager.getSession(senderId);
            if (senderSession != null && senderSession.isOpen()) {
                senderSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(deliveredMsg)));
            }

        } catch (Exception e) {
            log.error("Error sending message", e);
        }
    }

    /**
     * 處理「正在輸入」
     */
    private void handleTypingStart(UUID userId, WebSocketMessage wsMessage) {
        try {
            String recipientIdStr = (String) wsMessage.getPayload().get("recipientId");
            UUID recipientId = UUID.fromString(recipientIdStr);

            WebSocketMessage typingMsg = WebSocketMessage.builder()
                                                         .type(WebSocketMessage.MessageType.USER_TYPING)
                                                         .payload(Map.of("userId", userId.toString(), "isTyping", true))
                                                         .build();

            // 透過 Redis 發布
            redisPublisher.publishToUser(recipientId, typingMsg);

        } catch (Exception e) {
            log.error("Error handling typing start", e);
        }
    }

    /**
     * 處理「停止輸入」
     */
    private void handleTypingStop(UUID userId, WebSocketMessage wsMessage) {
        try {
            String recipientIdStr = (String) wsMessage.getPayload().get("recipientId");
            UUID recipientId = UUID.fromString(recipientIdStr);

            WebSocketMessage typingMsg = WebSocketMessage.builder()
                                                         .type(WebSocketMessage.MessageType.USER_TYPING)
                                                         .payload(Map.of("userId", userId.toString(), "isTyping", false))
                                                         .build();

            // 透過 Redis 發布
            redisPublisher.publishToUser(recipientId, typingMsg);

        } catch (Exception e) {
            log.error("Error handling typing stop", e);
        }
    }

    /**
     * 處理訊息已讀
     */
    private void handleMessageRead(UUID userId, WebSocketMessage wsMessage) {
        try {
            String conversationId = (String) wsMessage.getPayload().get("conversationId");
            String messageId = (String) wsMessage.getPayload().get("messageId");

            // 標記為已讀（需要在 MessageService 新增此方法）
            // messageService.markAsRead(conversationId, messageId);

            // 通知發送者（透過 Redis）
            // 需要從對話中找出另一個使用者
            // 簡化版：假設 conversationId = userId1_userId2
            String[] participants = conversationId.split("_");
            String otherUserIdStr = participants[0].equals(userId.toString()) ? participants[1] : participants[0];
            UUID otherUserId = UUID.fromString(otherUserIdStr);

            WebSocketMessage readReceipt = WebSocketMessage.builder()
                                                           .type(WebSocketMessage.MessageType.MESSAGE_READ_RECEIPT)
                                                           .payload(Map.of(
                                                               "conversationId", conversationId,
                                                               "messageId", messageId,
                                                               "readBy", userId.toString()
                                                           ))
                                                           .build();

            redisPublisher.publishToUser(otherUserId, readReceipt);

        } catch (Exception e) {
            log.error("Error handling message read", e);
        }
    }

    /**
     * 連線關閉時
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userIdStr = (String) session.getAttributes().get("userId");

        if (userIdStr != null) {
            UUID userId = UUID.fromString(userIdStr);
            sessionManager.removeSession(userId);
            log.info("User {} disconnected", userId);
        }
    }

    /**
     * 發送錯誤訊息
     */
    private void sendErrorMessage(WebSocketSession session, String errorMsg) {
        try {
            WebSocketMessage error = WebSocketMessage.builder()
                                                     .type(WebSocketMessage.MessageType.ERROR)
                                                     .payload(Map.of("message", errorMsg))
                                                     .build();
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
        } catch (Exception e) {
            log.error("Error sending error message", e);
        }
    }
}