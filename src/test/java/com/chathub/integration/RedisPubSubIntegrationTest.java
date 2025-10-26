package com.chathub.integration;

import com.chathub.service.RedisMessagePublisher;
import com.chathub.service.RedisMessageSubscriber;
import com.chathub.service.WebSocketSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Redis Pub/Sub 整合測試（使用 Testcontainers）
 */
@SpringBootTest
@Testcontainers
@DisplayName("Redis Pub/Sub 整合測試")
class RedisPubSubIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.0-alpine")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private RedisMessagePublisher publisher;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WebSocketSessionManager sessionManager;

    @MockitoBean
    private WebSocketSession webSocketSession;

    @Test
    @DisplayName("完整測試：發布訊息 → Redis → 訂閱 → WebSocket")
    void shouldPublishAndReceiveMessageThroughRedis() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        Map<String, String> message = Map.of(
            "type", "NEW_MESSAGE",
            "content", "Hello from Pod 1"
        );

        // 設定 WebSocket Session Mock
        when(sessionManager.getSession(userId.toString())).thenReturn(webSocketSession);
        when(webSocketSession.isOpen()).thenReturn(true);

        // 建立訂閱者
        RedisMessageSubscriber subscriber = new RedisMessageSubscriber(sessionManager, objectMapper);

        // 建立監聽容器
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisTemplate.getConnectionFactory());
        container.addMessageListener(subscriber, new ChannelTopic("user:" + userId));
        container.afterPropertiesSet();
        container.start();

        // 使用 CountDownLatch 等待訊息
        CountDownLatch latch = new CountDownLatch(1);

        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(webSocketSession).sendMessage(any(TextMessage.class));

        try {
            // When - 發布訊息
            publisher.publishToUser(userId, message);

            // 等待訊息處理（最多 5 秒）
            boolean received = latch.await(5, TimeUnit.SECONDS);

            // Then
            assertThat(received).isTrue();

            // 注意：可能會收到多次（因為 Redis Pub/Sub 特性），至少收到 1 次
            ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
            verify(webSocketSession, atLeastOnce()).sendMessage(messageCaptor.capture());

            String receivedMessage = messageCaptor.getValue().getPayload();
            assertThat(receivedMessage).contains("Hello from Pod 1");
            assertThat(receivedMessage).contains("NEW_MESSAGE");

        } finally {
            container.stop();
        }
    }

    @Test
    @DisplayName("測試：多個訂閱者同時接收訊息（模擬多 Pod）")
    void shouldDeliverMessageToMultipleSubscribers() throws Exception {
        // Given
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();

        WebSocketSession session1 = mock(WebSocketSession.class);
        WebSocketSession session2 = mock(WebSocketSession.class);

        when(sessionManager.getSession(user1.toString())).thenReturn(session1);
        when(sessionManager.getSession(user2.toString())).thenReturn(session2);
        when(session1.isOpen()).thenReturn(true);
        when(session2.isOpen()).thenReturn(true);

        RedisMessageSubscriber subscriber = new RedisMessageSubscriber(sessionManager, objectMapper);

        // 建立兩個監聽容器（模擬兩個 Pod）
        RedisMessageListenerContainer container1 = createContainer(subscriber, "user:" + user1);
        RedisMessageListenerContainer container2 = createContainer(subscriber, "user:" + user2);

        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);

        doAnswer(inv -> {
            latch1.countDown();
            return null;
        }).when(session1).sendMessage(any());

        doAnswer(inv -> {
            latch2.countDown();
            return null;
        }).when(session2).sendMessage(any());

        try {
            // When - 發送訊息給 user1 和 user2
            publisher.publishToUser(user1, Map.of("content", "Message for User 1"));
            publisher.publishToUser(user2, Map.of("content", "Message for User 2"));

            // Then
            boolean user1Received = latch1.await(5, TimeUnit.SECONDS);
            boolean user2Received = latch2.await(5, TimeUnit.SECONDS);

            assertThat(user1Received).isTrue();
            assertThat(user2Received).isTrue();

            // 注意：使用 atLeastOnce() 因為可能收到多次
            verify(session1, atLeastOnce()).sendMessage(any(TextMessage.class));
            verify(session2, atLeastOnce()).sendMessage(any(TextMessage.class));

        } finally {
            container1.stop();
            container2.stop();
        }
    }

    /**
     * 建立 Redis 監聽容器
     */
    private RedisMessageListenerContainer createContainer(
        RedisMessageSubscriber subscriber,
        String channel) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisTemplate.getConnectionFactory());
        container.addMessageListener(subscriber, new ChannelTopic(channel));
        container.afterPropertiesSet();
        container.start();
        return container;
    }
}