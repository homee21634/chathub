package com.chathub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * RedisMessageSubscriber 單元測試
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Redis 訊息訂閱者測試")
class RedisMessageSubscriberTest {

    @Mock
    private WebSocketSessionManager sessionManager;

    @Mock
    private WebSocketSession webSocketSession;

    @Mock
    private Message redisMessage;

    private ObjectMapper objectMapper;
    private RedisMessageSubscriber subscriber;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        subscriber = new RedisMessageSubscriber(sessionManager, objectMapper);
    }

    @Test
    @DisplayName("應該成功處理 Redis 訊息並推送給 WebSocket")
    void shouldHandleRedisMessageAndSendToWebSocket() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        String channel = "user:" + userId.toString();
        Map<String, String> messageContent = Map.of("content", "Hello from Redis");
        String jsonMessage = objectMapper.writeValueAsString(messageContent);

        when(redisMessage.getChannel()).thenReturn(channel.getBytes());
        when(redisMessage.getBody()).thenReturn(jsonMessage.getBytes());
        when(sessionManager.getSession(userId.toString())).thenReturn(webSocketSession);
        when(webSocketSession.isOpen()).thenReturn(true);

        // When
        subscriber.onMessage(redisMessage, null);

        // Then
        ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(webSocketSession).sendMessage(messageCaptor.capture());

        String sentMessage = messageCaptor.getValue().getPayload();
        assertThat(sentMessage).contains("Hello from Redis");
    }

    @Test
    @DisplayName("當使用者未連線時，不應該發送訊息")
    void shouldNotSendMessageWhenUserNotConnected() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        String channel = "user:" + userId.toString();
        String jsonMessage = "{\"content\":\"Test\"}";

        when(redisMessage.getChannel()).thenReturn(channel.getBytes());
        when(redisMessage.getBody()).thenReturn(jsonMessage.getBytes());
        when(sessionManager.getSession(userId.toString())).thenReturn(null);

        // When
        subscriber.onMessage(redisMessage, null);

        // Then
        verify(webSocketSession, never()).sendMessage(any());
    }

    @Test
    @DisplayName("當 WebSocket 連線已關閉時，不應該發送訊息")
    void shouldNotSendMessageWhenSessionClosed() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        String channel = "user:" + userId.toString();
        String jsonMessage = "{\"content\":\"Test\"}";

        when(redisMessage.getChannel()).thenReturn(channel.getBytes());
        when(redisMessage.getBody()).thenReturn(jsonMessage.getBytes());
        when(sessionManager.getSession(userId.toString())).thenReturn(webSocketSession);
        when(webSocketSession.isOpen()).thenReturn(false);

        // When
        subscriber.onMessage(redisMessage, null);

        // Then
        verify(webSocketSession, never()).sendMessage(any());
    }

    @Test
    @DisplayName("當頻道格式錯誤時，應該記錄警告")
    void shouldLogWarningWhenChannelFormatInvalid() throws Exception {
        // Given
        String invalidChannel = "invalid-channel-format";
        String jsonMessage = "{\"content\":\"Test\"}";

        when(redisMessage.getChannel()).thenReturn(invalidChannel.getBytes());
        when(redisMessage.getBody()).thenReturn(jsonMessage.getBytes());

        // When
        subscriber.onMessage(redisMessage, null);

        // Then
        verify(sessionManager, never()).getSession(anyString());
        verify(webSocketSession, never()).sendMessage(any());
    }

    @Test
    @DisplayName("當處理訊息發生異常時，應該捕獲並記錄")
    void shouldHandleExceptionWhenProcessingMessage() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        String channel = "user:" + userId.toString();
        String jsonMessage = "{\"content\":\"Test\"}";

        when(redisMessage.getChannel()).thenReturn(channel.getBytes());
        when(redisMessage.getBody()).thenReturn(jsonMessage.getBytes());
        when(sessionManager.getSession(userId.toString())).thenReturn(webSocketSession);
        when(webSocketSession.isOpen()).thenReturn(true);

        // 模擬 WebSocket 發送失敗
        doThrow(new RuntimeException("WebSocket Error")).when(webSocketSession).sendMessage(any());

        // When & Then - 不應該拋出異常
        subscriber.onMessage(redisMessage, null);
    }
}