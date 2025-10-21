package com.chathub.service;

import com.chathub.entity.Conversation;
import com.chathub.entity.Message;
import com.chathub.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * 對話服務
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;

    /**
     * 更新對話（新訊息時呼叫）
     */
    @Transactional
    public void updateConversation(Message message) {
        String conversationId = message.getConversationId();

        // 查詢對話是否存在
        Optional<Conversation> existingConv = conversationRepository
            .findByConversationId(conversationId);

        if (existingConv.isPresent()) {
            // 更新現有對話
            Conversation conversation = existingConv.get();

            // 更新最後一則訊息
            Conversation.LastMessage lastMessage = Conversation.LastMessage.builder()
                                                                           .messageId(message.getMessageId())
                                                                           .content(message.getContent())
                                                                           .senderId(message.getSenderId())
                                                                           .timestamp(message.getTimestamp())
                                                                           .build();
            conversation.setLastMessage(lastMessage);

            // 增加接收者的未讀數
            String recipientIdStr = message.getRecipientId().toString();
            int currentUnread = conversation.getUnreadCounts()
                                            .getOrDefault(recipientIdStr, 0);
            conversation.getUnreadCounts().put(recipientIdStr, currentUnread + 1);

            // 更新時間
            conversation.setUpdatedAt(Instant.now());

            conversationRepository.save(conversation);
            log.debug("對話已更新：{}", conversationId);

        } else {
            // 建立新對話
            Conversation.LastMessage lastMessage = Conversation.LastMessage.builder()
                                                                           .messageId(message.getMessageId())
                                                                           .content(message.getContent())
                                                                           .senderId(message.getSenderId())
                                                                           .timestamp(message.getTimestamp())
                                                                           .build();

            Map<String, Integer> unreadCounts = new HashMap<>();
            unreadCounts.put(message.getSenderId().toString(), 0);
            unreadCounts.put(message.getRecipientId().toString(), 1);

            Conversation conversation = Conversation.builder()
                                                    .conversationId(conversationId)
                                                    .participants(Arrays.asList(message.getSenderId(), message.getRecipientId()))
                                                    .lastMessage(lastMessage)
                                                    .unreadCounts(unreadCounts)
                                                    .createdAt(Instant.now())
                                                    .updatedAt(Instant.now())
                                                    .build();

            conversationRepository.save(conversation);
            log.info("新對話已建立：{}", conversationId);
        }
    }

    /**
     * 重置未讀數（標記已讀時呼叫）
     */
    @Transactional
    public void resetUnreadCount(String conversationId, UUID userId) {
        Optional<Conversation> convOpt = conversationRepository
            .findByConversationId(conversationId);

        if (convOpt.isPresent()) {
            Conversation conversation = convOpt.get();
            conversation.getUnreadCounts().put(userId.toString(), 0);
            conversationRepository.save(conversation);
            log.debug("未讀數已重置：對話 {}，使用者 {}", conversationId, userId);
        }
    }

    /**
     * 查詢使用者的對話列表
     */
    public Page<Conversation> getUserConversations(UUID userId, Pageable pageable) {
        return conversationRepository
            .findByParticipantsContainingOrderByUpdatedAtDesc(userId, pageable);
    }

    /**
     * 取得對話詳細資訊
     */
    public Optional<Conversation> getConversation(String conversationId) {
        return conversationRepository.findByConversationId(conversationId);
    }

    /**
     * 取得使用者的總未讀數
     */
    public int getTotalUnreadCount(UUID userId) {
        Page<Conversation> conversations = conversationRepository
            .findByParticipantsContainingOrderByUpdatedAtDesc(
                userId,
                Pageable.unpaged()
            );

        return conversations.stream()
                            .mapToInt(conv -> conv.getUnreadCounts()
                                                  .getOrDefault(userId.toString(), 0))
                            .sum();
    }
}