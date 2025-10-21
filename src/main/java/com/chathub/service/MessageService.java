package com.chathub.service;

import com.chathub.entity.Message;
import com.chathub.entity.User;
import com.chathub.repository.MessageRepository;
import com.chathub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 訊息服務
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ConversationService conversationService;

    /**
     * 儲存新訊息
     */
    @Transactional
    public Message saveMessage(
        UUID senderId,
        UUID recipientId,
        String content,
        String clientMessageId
    ) {
        // 檢查是否重複訊息（去重）
        if (clientMessageId != null) {
            Optional<Message> existing = messageRepository.findByClientMessageId(clientMessageId);
            if (existing.isPresent()) {
                log.warn("重複訊息，已忽略：{}", clientMessageId);
                return existing.get();
            }
        }

        // 取得發送者資訊
        User sender = userRepository.findById(senderId)
                                    .orElseThrow(() -> new RuntimeException("發送者不存在"));

        // 生成對話 ID
        String conversationId = Message.generateConversationId(senderId, recipientId);

        // 建立訊息
        Message message = Message.builder()
                                 .messageId(UUID.randomUUID().toString())
                                 .conversationId(conversationId)
                                 .senderId(senderId)
                                 .senderUsername(sender.getUsername())
                                 .recipientId(recipientId)
                                 .content(content)
                                 .timestamp(Instant.now())
                                 .isRead(false)
                                 .clientMessageId(clientMessageId)
                                 .createdAt(Instant.now())
                                 .build();

        // 儲存訊息
        Message savedMessage = messageRepository.save(message);
        log.info("訊息已儲存：{} → {}", sender.getUsername(), recipientId);

        // 更新對話資訊
        conversationService.updateConversation(savedMessage);

        return savedMessage;
    }

    /**
     * 查詢對話歷史訊息（分頁）
     */
    public Page<Message> getConversationMessages(String conversationId, Pageable pageable) {
        return messageRepository.findByConversationIdOrderByTimestampDesc(conversationId, pageable);
    }

    /**
     * 標記訊息為已讀
     */
    @Transactional
    public void markMessagesAsRead(String conversationId, UUID recipientId) {
        List<Message> unreadMessages = messageRepository
            .findByConversationIdAndRecipientIdAndIsReadFalse(conversationId, recipientId);

        if (unreadMessages.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        unreadMessages.forEach(msg -> {
            msg.setIsRead(true);
            msg.setReadAt(now);
        });

        messageRepository.saveAll(unreadMessages);

        // 更新對話未讀數
        conversationService.resetUnreadCount(conversationId, recipientId);

        log.info("已標記 {} 則訊息為已讀（對話 {}，使用者 {}）",
                 unreadMessages.size(), conversationId, recipientId);
    }

    /**
     * 查詢使用者的未讀訊息總數
     */
    public long getUnreadCount(UUID userId) {
        return messageRepository.countByRecipientIdAndIsReadFalse(userId);
    }

    /**
     * 查詢特定對話的未讀訊息數
     */
    public long getConversationUnreadCount(String conversationId, UUID userId) {
        return messageRepository.countByConversationIdAndRecipientIdAndIsReadFalse(
            conversationId, userId);
    }
}