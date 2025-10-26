package com.chathub.config;

import com.chathub.service.RedisMessageSubscriber;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置類別
 * 功能：配置 RedisTemplate 與 Pub/Sub 訂閱
 */
@Configuration
public class RedisConfig {

    /**
     * RedisTemplate 配置（用於發布訊息）
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key 使用 String 序列化
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value 使用 JSON 序列化
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * ObjectMapper（用於 JSON 轉換）
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * Redis 訊息監聽容器（核心！）
     * 功能：監聽 Redis 頻道，將訊息轉發給 Subscriber
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
        RedisConnectionFactory connectionFactory,
        RedisMessageSubscriber subscriber) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 訂閱所有 user:* 頻道（使用萬用字元）
        // 例如：user:abc-123, user:def-456 都會被監聽
        container.addMessageListener(subscriber, new PatternTopic("user:*"));

        // 如果需要訂閱對話頻道，可以加這行：
        // container.addMessageListener(subscriber, new PatternTopic("conversation:*"));

        return container;
    }
}