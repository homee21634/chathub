package com.chathub.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * WebSocket 訊息 DTO
 * 用於客戶端與伺服器之間的通訊
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)  // null 欄位不序列化
public class WebSocketMessage {

    /**
     * 訊息類型
     */
    private MessageType type;

    /**
     * 訊息內容（根據 type 不同而不同）
     */
    private Map<String, Object> payload;

    /**
     * 時間戳記
     */
    private Instant timestamp;

    /**
     * 訊息類型枚舉
     */
    public enum MessageType {
        // ===== 客戶端 → 伺服器 =====
        SEND_MESSAGE,      // 發送訊息
        TYPING_START,      // 開始輸入
        TYPING_STOP,       // 停止輸入
        MESSAGE_READ,      // 標記已讀
        PING,              // 心跳

        // ===== 伺服器 → 客戶端 =====
        CONNECTION_ESTABLISHED,  // 連線成功
        NEW_MESSAGE,            // 新訊息
        MESSAGE_DELIVERED,      // 訊息送達確認
        USER_TYPING,           // 對方正在輸入
        MESSAGE_READ_RECEIPT,  // 已讀回執
        PONG,                  // 心跳回應
        ERROR                  // 錯誤訊息
    }

    /**
     * 建立連線成功訊息
     */
    public static WebSocketMessage connectionEstablished(UUID userId) {
        return WebSocketMessage.builder()
                               .type(MessageType.CONNECTION_ESTABLISHED)
                               .payload(Map.of(
                                   "userId", userId.toString(),
                                   "message", "WebSocket 連線成功"
                               ))
                               .timestamp(Instant.now())
                               .build();
    }

    /**
     * 建立新訊息通知
     */
    public static WebSocketMessage newMessage(
        String messageId,
        String conversationId,
        UUID senderId,
        String senderUsername,
        String content,
        Instant timestamp
    ) {
        return WebSocketMessage.builder()
                               .type(MessageType.NEW_MESSAGE)
                               .payload(Map.of(
                                   "messageId", messageId,
                                   "conversationId", conversationId,
                                   "senderId", senderId.toString(),
                                   "senderUsername", senderUsername,
                                   "content", content,
                                   "timestamp", timestamp.toString()
                               ))
                               .timestamp(Instant.now())
                               .build();
    }

    /**
     * 建立訊息送達確認
     */
    public static WebSocketMessage messageDelivered(
        String messageId,
        String clientMessageId
    ) {
        return WebSocketMessage.builder()
                               .type(MessageType.MESSAGE_DELIVERED)
                               .payload(Map.of(
                                   "messageId", messageId,
                                   "clientMessageId", clientMessageId
                               ))
                               .timestamp(Instant.now())
                               .build();
    }

    /**
     * 建立正在輸入通知
     */
    public static WebSocketMessage userTyping(
        UUID userId,
        String username,
        boolean isTyping
    ) {
        return WebSocketMessage.builder()
                               .type(MessageType.USER_TYPING)
                               .payload(Map.of(
                                   "userId", userId.toString(),
                                   "username", username,
                                   "isTyping", isTyping
                               ))
                               .timestamp(Instant.now())
                               .build();
    }

    /**
     * 建立已讀回執
     */
    public static WebSocketMessage messageReadReceipt(
        String conversationId,
        String messageId,
        UUID readBy
    ) {
        return WebSocketMessage.builder()
                               .type(MessageType.MESSAGE_READ_RECEIPT)
                               .payload(Map.of(
                                   "conversationId", conversationId,
                                   "messageId", messageId,
                                   "readBy", readBy.toString(),
                                   "readAt", Instant.now().toString()
                               ))
                               .timestamp(Instant.now())
                               .build();
    }

    /**
     * 建立 PONG 回應
     */
    public static WebSocketMessage pong() {
        return WebSocketMessage.builder()
                               .type(MessageType.PONG)
                               .payload(Map.of("message", "pong"))
                               .timestamp(Instant.now())
                               .build();
    }

    /**
     * 建立錯誤訊息
     */
    public static WebSocketMessage error(String code, String message) {
        return WebSocketMessage.builder()
                               .type(MessageType.ERROR)
                               .payload(Map.of(
                                   "code", code,
                                   "message", message
                               ))
                               .timestamp(Instant.now())
                               .build();
    }
}