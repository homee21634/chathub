package com.chathub.repository;

import com.chathub.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 對話 Repository
 */
@Repository
public interface ConversationRepository extends MongoRepository<Conversation, String> {

    /**
     * 根據對話 ID 查詢
     */
    Optional<Conversation> findByConversationId(String conversationId);

    /**
     * 查詢使用者的所有對話（按更新時間排序）
     */
    @Query("{ 'participants': ?0 }")
    Page<Conversation> findByParticipantsContainingOrderByUpdatedAtDesc(
        UUID userId,
        Pageable pageable
    );

    /**
     * 檢查對話是否存在
     */
    boolean existsByConversationId(String conversationId);
}