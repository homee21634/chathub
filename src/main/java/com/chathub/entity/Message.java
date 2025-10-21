package com.chathub.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

/**
 * 訊息實體類（MongoDB）
 * 儲存所有聊天訊息的歷史記錄
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "messages")
@CompoundIndex(
    name = "conversation_timestamp_idx",
    def = "{'conversationId': 1, 'timestamp': -1}"
)
public class Message {

    /**
     * MongoDB 內部 ID
     */
    @Id
    private String id;

    /**
     * 訊息唯一 ID（UUID）
     */
    @Indexed(unique = true)
    private String messageId;

    /**
     * 對話 ID（由兩個使用者 ID 組成，較小的在前）
     * 格式：{smaller-uuid}_{larger-uuid}
     */
    @Indexed
    private String conversationId;

    /**
     * 發送者 ID
     */
    @Indexed
    private UUID senderId;

    /**
     * 發送者使用者名稱（冗餘欄位，方便查詢）
     */
    private String senderUsername;

    /**
     * 接收者 ID
     */
    @Indexed
    private UUID recipientId;

    /**
     * 訊息內容
     */
    private String content;

    /**
     * 訊息時間戳記
     */
    @Indexed
    private Instant timestamp;

    /**
     * 是否已讀
     */
    private Boolean isRead;

    /**
     * 已讀時間
     */
    private Instant readAt;

    /**
     * 客戶端訊息 ID（用於去重）
     */
    @Indexed(unique = true, sparse = true)
    private String clientMessageId;

    /**
     * 建立時間
     */
    private Instant createdAt;

    /**
     * 生成對話 ID 的靜態方法
     * 確保兩個使用者的對話 ID 一致（與順序無關）
     */
    public static String generateConversationId(UUID userId1, UUID userId2) {
        String id1 = userId1.toString();
        String id2 = userId2.toString();

        // 字典序排序，確保唯一性
        if (id1.compareTo(id2) < 0) {
            return id1 + "_" + id2;
        } else {
            return id2 + "_" + id1;
        }
    }
}