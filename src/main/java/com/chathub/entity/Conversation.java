package com.chathub.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 對話實體類（MongoDB）
 * 儲存兩個使用者之間的對話元資料
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "conversations")
public class Conversation {

    /**
     * MongoDB 內部 ID
     */
    @Id
    private String id;

    /**
     * 對話唯一 ID（與 Message 的 conversationId 一致）
     */
    @Indexed(unique = true)
    private String conversationId;

    /**
     * 參與者 ID 列表（固定 2 人）
     */
    @Indexed
    private List<UUID> participants;

    /**
     * 最後一則訊息資訊
     */
    private LastMessage lastMessage;

    /**
     * 各使用者的未讀訊息數
     * key: userId, value: unreadCount
     */
    @Builder.Default
    private Map<String, Integer> unreadCounts = new HashMap<>();

    /**
     * 對話建立時間
     */
    private Instant createdAt;

    /**
     * 最後更新時間
     */
    @Indexed
    private Instant updatedAt;

    /**
     * 最後一則訊息的內嵌類別
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LastMessage {
        private String messageId;
        private String content;
        private UUID senderId;
        private Instant timestamp;
    }
}