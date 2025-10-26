package com.chathub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Redis 訊息發布者
 * 功能：將 WebSocket 訊息發布到 Redis 頻道，讓所有 Pod 都能收到
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisMessagePublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 發布訊息到使用者專屬頻道（UUID 版本）
     * @param userId 目標使用者 ID（UUID）
     * @param message 訊息物件（會自動轉 JSON）
     */
    public void publishToUser(UUID userId, Object message) {
        publishToUser(userId.toString(), message);
    }

    /**
     * 發布訊息到使用者專屬頻道（String 版本）
     * @param userId 目標使用者 ID（String）
     * @param message 訊息物件（會自動轉 JSON）
     */
    public void publishToUser(String userId, Object message) {
        try {
            String channel = "user:" + userId;
            String jsonMessage = objectMapper.writeValueAsString(message);

            redisTemplate.convertAndSend(channel, jsonMessage);
            log.debug("Published message to channel: {}", channel);

        } catch (Exception e) {
            log.error("Failed to publish message to user: {}", userId, e);
        }
    }

    /**
     * 發布訊息到對話頻道
     * @param conversationId 對話 ID
     * @param message 訊息物件
     */
    public void publishToConversation(String conversationId, Object message) {
        try {
            String channel = "conversation:" + conversationId;
            String jsonMessage = objectMapper.writeValueAsString(message);

            redisTemplate.convertAndSend(channel, jsonMessage);
            log.debug("Published message to channel: {}", channel);

        } catch (Exception e) {
            log.error("Failed to publish message to conversation: {}", conversationId, e);
        }
    }
}