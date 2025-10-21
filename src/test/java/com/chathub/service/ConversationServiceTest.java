package com.chathub.service;

import com.chathub.entity.Conversation;
import com.chathub.entity.Message;
import com.chathub.repository.ConversationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConversationService 測試
 */
@SpringBootTest
public class ConversationServiceTest {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private ConversationRepository conversationRepository;

    @AfterEach
    public void cleanup() {
        conversationRepository.deleteAll();
    }

    @Test
    public void testUpdateConversation_CreateNew() {
        // 建立測試訊息
        UUID senderId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        String conversationId = Message.generateConversationId(senderId, recipientId);

        Message message = Message.builder()
                                 .messageId(UUID.randomUUID().toString())
                                 .conversationId(conversationId)
                                 .senderId(senderId)
                                 .senderUsername("alice")
                                 .recipientId(recipientId)
                                 .content("第一則訊息")
                                 .timestamp(Instant.now())
                                 .isRead(false)
                                 .createdAt(Instant.now())
                                 .build();

        // 更新對話（應該建立新對話）
        conversationService.updateConversation(message);

        // 驗證對話已建立
        Optional<Conversation> convOpt = conversationRepository
            .findByConversationId(conversationId);

        assertTrue(convOpt.isPresent(), "對話應該被建立");

        Conversation conv = convOpt.get();
        assertEquals(conversationId, conv.getConversationId());
        assertEquals(2, conv.getParticipants().size());
        assertEquals("第一則訊息", conv.getLastMessage().getContent());
        assertEquals(1, conv.getUnreadCounts().get(recipientId.toString()));
        assertEquals(0, conv.getUnreadCounts().get(senderId.toString()));

        System.out.println("✅ 建立新對話測試通過！");
    }

    @Test
    public void testUpdateConversation_UpdateExisting() {
        // 先建立一個對話
        UUID senderId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        String conversationId = Message.generateConversationId(senderId, recipientId);

        Message firstMessage = Message.builder()
                                      .messageId(UUID.randomUUID().toString())
                                      .conversationId(conversationId)
                                      .senderId(senderId)
                                      .senderUsername("alice")
                                      .recipientId(recipientId)
                                      .content("第一則訊息")
                                      .timestamp(Instant.now())
                                      .isRead(false)
                                      .createdAt(Instant.now())
                                      .build();

        conversationService.updateConversation(firstMessage);

        // 發送第二則訊息
        Message secondMessage = Message.builder()
                                       .messageId(UUID.randomUUID().toString())
                                       .conversationId(conversationId)
                                       .senderId(senderId)
                                       .senderUsername("alice")
                                       .recipientId(recipientId)
                                       .content("第二則訊息")
                                       .timestamp(Instant.now())
                                       .isRead(false)
                                       .createdAt(Instant.now())
                                       .build();

        conversationService.updateConversation(secondMessage);

        // 驗證對話已更新
        Conversation conv = conversationRepository
            .findByConversationId(conversationId)
            .orElseThrow();

        assertEquals("第二則訊息", conv.getLastMessage().getContent(),
                     "最後一則訊息應該被更新");
        assertEquals(2, conv.getUnreadCounts().get(recipientId.toString()),
                     "未讀數應該累加");

        System.out.println("✅ 更新對話測試通過！");
    }

    @Test
    public void testResetUnreadCount() {
        // 建立對話並設定未讀數
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        String conversationId = Message.generateConversationId(user1, user2);

        Message message = Message.builder()
                                 .messageId(UUID.randomUUID().toString())
                                 .conversationId(conversationId)
                                 .senderId(user1)
                                 .senderUsername("user1")
                                 .recipientId(user2)
                                 .content("測試訊息")
                                 .timestamp(Instant.now())
                                 .isRead(false)
                                 .createdAt(Instant.now())
                                 .build();

        conversationService.updateConversation(message);

        // 驗證未讀數
        Conversation convBefore = conversationRepository
            .findByConversationId(conversationId)
            .orElseThrow();
        assertEquals(1, convBefore.getUnreadCounts().get(user2.toString()));

        // 重置未讀數
        conversationService.resetUnreadCount(conversationId, user2);

        // 驗證未讀數已重置
        Conversation convAfter = conversationRepository
            .findByConversationId(conversationId)
            .orElseThrow();
        assertEquals(0, convAfter.getUnreadCounts().get(user2.toString()));

        System.out.println("✅ 重置未讀數測試通過！");
    }

    @Test
    public void testGetUserConversations() {
        UUID userId = UUID.randomUUID();

        // 建立 3 個對話
        for (int i = 1; i <= 3; i++) {
            UUID friendId = UUID.randomUUID();
            String conversationId = Message.generateConversationId(userId, friendId);

            Message message = Message.builder()
                                     .messageId(UUID.randomUUID().toString())
                                     .conversationId(conversationId)
                                     .senderId(friendId)
                                     .senderUsername("friend" + i)
                                     .recipientId(userId)
                                     .content("訊息 " + i)
                                     .timestamp(Instant.now().plusSeconds(i))
                                     .isRead(false)
                                     .createdAt(Instant.now())
                                     .build();

            conversationService.updateConversation(message);
        }

        // 查詢使用者的對話列表
        Page<Conversation> conversations = conversationService
            .getUserConversations(userId, PageRequest.of(0, 10));

        assertEquals(3, conversations.getTotalElements(),
                     "使用者應該有 3 個對話");

        System.out.println("✅ 查詢對話列表測試通過！");
    }

    @Test
    public void testGetTotalUnreadCount() {
        UUID userId = UUID.randomUUID();

        // 建立 2 個對話，各有不同未讀數
        // 對話 1：2 則未讀
        UUID friend1 = UUID.randomUUID();
        String conv1 = Message.generateConversationId(userId, friend1);

        for (int i = 1; i <= 2; i++) {
            Message msg = Message.builder()
                                 .messageId(UUID.randomUUID().toString())
                                 .conversationId(conv1)
                                 .senderId(friend1)
                                 .senderUsername("friend1")
                                 .recipientId(userId)
                                 .content("訊息 " + i)
                                 .timestamp(Instant.now())
                                 .isRead(false)
                                 .createdAt(Instant.now())
                                 .build();
            conversationService.updateConversation(msg);
        }

        // 對話 2：3 則未讀
        UUID friend2 = UUID.randomUUID();
        String conv2 = Message.generateConversationId(userId, friend2);

        for (int i = 1; i <= 3; i++) {
            Message msg = Message.builder()
                                 .messageId(UUID.randomUUID().toString())
                                 .conversationId(conv2)
                                 .senderId(friend2)
                                 .senderUsername("friend2")
                                 .recipientId(userId)
                                 .content("訊息 " + i)
                                 .timestamp(Instant.now())
                                 .isRead(false)
                                 .createdAt(Instant.now())
                                 .build();
            conversationService.updateConversation(msg);
        }

        // 總未讀數應該是 5
        int totalUnread = conversationService.getTotalUnreadCount(userId);
        assertEquals(5, totalUnread, "總未讀數應該是 5");

        System.out.println("✅ 查詢總未讀數測試通過！");
    }
}