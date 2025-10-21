package com.chathub.repository;

import com.chathub.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 訊息 Repository
 */
@Repository
public interface MessageRepository extends MongoRepository<Message, String> {

    /**
     * 根據對話 ID 查詢訊息（分頁）
     */
    Page<Message> findByConversationIdOrderByTimestampDesc(
        String conversationId,
        Pageable pageable
    );

    /**
     * 根據客戶端訊息 ID 查詢（去重用）
     */
    Optional<Message> findByClientMessageId(String clientMessageId);

    /**
     * 查詢使用者的未讀訊息數量
     */
    long countByRecipientIdAndIsReadFalse(UUID recipientId);

    /**
     * 查詢特定對話的未讀訊息數量
     */
    long countByConversationIdAndRecipientIdAndIsReadFalse(
        String conversationId,
        UUID recipientId
    );

    /**
     * 查詢特定對話的所有未讀訊息
     */
    List<Message> findByConversationIdAndRecipientIdAndIsReadFalse(
        String conversationId,
        UUID recipientId
    );
}