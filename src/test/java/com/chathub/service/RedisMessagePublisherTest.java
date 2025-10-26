package com.chathub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * RedisMessagePublisher 單元測試
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Redis 訊息發布者測試")
class RedisMessagePublisherTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    private ObjectMapper objectMapper;
    private RedisMessagePublisher publisher;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        publisher = new RedisMessagePublisher(redisTemplate, objectMapper);
    }

    @Test
    @DisplayName("應該成功發布訊息到使用者頻道（UUID）")
    void shouldPublishMessageToUserChannelWithUUID() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        Map<String, String> message = Map.of("content", "Hello World");

        // When
        publisher.publishToUser(userId, message);

        // Then
        ArgumentCaptor<String> channelCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        verify(redisTemplate).convertAndSend(channelCaptor.capture(), messageCaptor.capture());

        assertThat(channelCaptor.getValue()).isEqualTo("user:" + userId.toString());
        assertThat(messageCaptor.getValue()).contains("Hello World");
    }

    @Test
    @DisplayName("應該成功發布訊息到使用者頻道（String）")
    void shouldPublishMessageToUserChannelWithString() throws Exception {
        // Given
        String userId = "test-user-123";
        Map<String, String> message = Map.of("content", "Test Message");

        // When
        publisher.publishToUser(userId, message);

        // Then
        ArgumentCaptor<String> channelCaptor = ArgumentCaptor.forClass(String.class);

        verify(redisTemplate).convertAndSend(channelCaptor.capture(), anyString());

        assertThat(channelCaptor.getValue()).isEqualTo("user:test-user-123");
    }

    @Test
    @DisplayName("應該成功發布訊息到對話頻道")
    void shouldPublishMessageToConversationChannel() throws Exception {
        // Given
        String conversationId = "conv-abc-123";
        Map<String, Object> message = Map.of(
            "messageId", "msg-123",
            "content", "Hello from conversation"
        );

        // When
        publisher.publishToConversation(conversationId, message);

        // Then
        ArgumentCaptor<String> channelCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        verify(redisTemplate).convertAndSend(channelCaptor.capture(), messageCaptor.capture());

        assertThat(channelCaptor.getValue()).isEqualTo("conversation:conv-abc-123");
        assertThat(messageCaptor.getValue()).contains("msg-123");
    }

    @Test
    @DisplayName("當 JSON 序列化失敗時，應該捕獲異常並記錄")
    void shouldHandleJsonSerializationError() {
        // Given
        ObjectMapper faultyMapper = mock(ObjectMapper.class);
        RedisMessagePublisher faultyPublisher = new RedisMessagePublisher(redisTemplate, faultyMapper);

        // 模擬序列化錯誤
        try {
            when(faultyMapper.writeValueAsString(any())).thenThrow(new RuntimeException("JSON Error"));
        } catch (Exception e) {
            // ignore
        }

        // When & Then - 不應該拋出異常
        UUID userId = UUID.randomUUID();
        faultyPublisher.publishToUser(userId, Map.of("test", "data"));

        // 驗證 Redis 沒有被呼叫
        verify(redisTemplate, never()).convertAndSend(anyString(), anyString());
    }
}