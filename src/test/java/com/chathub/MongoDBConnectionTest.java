package com.chathub;

import com.chathub.entity.Conversation;
import com.chathub.entity.Message;
import com.chathub.repository.ConversationRepository;
import com.chathub.repository.MessageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MongoDB 連線與基本操作測試
 */
@SpringBootTest
public class MongoDBConnectionTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @AfterEach
    public void cleanup() {
        // 清理測試資料
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
    }

    @Test
    public void testMongoDBConnection() {
        // 測試 MongoTemplate 是否可用
        assertNotNull(mongoTemplate, "MongoTemplate 應該不為 null");
        System.out.println("✅ MongoDB 連線成功！");
    }

    @Test
    public void testSaveAndFindMessage() {
        // 建立測試訊息
        UUID senderId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        String conversationId = Message.generateConversationId(senderId, recipientId);

        Message message = Message.builder()
                                 .messageId(UUID.randomUUID().toString())
                                 .conversationId(conversationId)
                                 .senderId(senderId)
                                 .senderUsername("test_user")
                                 .recipientId(recipientId)
                                 .content("測試訊息內容")
                                 .timestamp(Instant.now())
                                 .isRead(false)
                                 .clientMessageId("client-test-001")
                                 .createdAt(Instant.now())
                                 .build();

        // 儲存訊息
        Message saved = messageRepository.save(message);
        assertNotNull(saved.getId(), "MongoDB _id 應該被自動生成");

        // 查詢訊息
        Message found = messageRepository.findById(saved.getId()).orElse(null);
        assertNotNull(found, "應該能查詢到剛儲存的訊息");
        assertEquals("測試訊息內容", found.getContent());

        System.out.println("✅ 訊息儲存與查詢測試通過！");
        System.out.println("   訊息 ID: " + saved.getId());
        System.out.println("   對話 ID: " + saved.getConversationId());
    }

    @Test
    public void testFindByClientMessageId() {
        // 測試去重功能
        String clientMsgId = "unique-client-001";
        UUID senderId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();

        Message message = Message.builder()
                                 .messageId(UUID.randomUUID().toString())
                                 .conversationId(Message.generateConversationId(senderId, recipientId))
                                 .senderId(senderId)
                                 .senderUsername("alice")
                                 .recipientId(recipientId)
                                 .content("去重測試")
                                 .timestamp(Instant.now())
                                 .isRead(false)
                                 .clientMessageId(clientMsgId)
                                 .createdAt(Instant.now())
                                 .build();

        messageRepository.save(message);

        // 根據 clientMessageId 查詢
        Message found = messageRepository.findByClientMessageId(clientMsgId).orElse(null);
        assertNotNull(found, "應該能透過 clientMessageId 找到訊息");
        assertEquals("去重測試", found.getContent());

        System.out.println("✅ clientMessageId 查詢測試通過！");
    }

    @Test
    public void testSaveAndFindConversation() {
        // 建立測試對話
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        String conversationId = Message.generateConversationId(user1, user2);

        Conversation conversation = Conversation.builder()
                                                .conversationId(conversationId)
                                                .participants(Arrays.asList(user1, user2))
                                                .lastMessage(Conversation.LastMessage.builder()
                                                                                     .messageId(UUID.randomUUID().toString())
                                                                                     .content("最後一則訊息")
                                                                                     .senderId(user1)
                                                                                     .timestamp(Instant.now())
                                                                                     .build())
                                                .unreadCounts(new HashMap<String, Integer>() {{
                                                    put(user1.toString(), 0);
                                                    put(user2.toString(), 3);
                                                }})
                                                .createdAt(Instant.now())
                                                .updatedAt(Instant.now())
                                                .build();

        // 儲存對話
        Conversation saved = conversationRepository.save(conversation);
        assertNotNull(saved.getId());

        // 查詢對話
        Conversation found = conversationRepository
            .findByConversationId(conversationId)
            .orElse(null);

        assertNotNull(found, "應該能查詢到對話");
        assertEquals(2, found.getParticipants().size());
        assertEquals(3, found.getUnreadCounts().get(user2.toString()));

        System.out.println("✅ 對話儲存與查詢測試通過！");
        System.out.println("   對話 ID: " + found.getConversationId());
        System.out.println("   參與者數量: " + found.getParticipants().size());
    }

    @Test
    public void testGenerateConversationId() {
        // 測試對話 ID 生成的一致性
        UUID alice = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID bob = UUID.fromString("22222222-2222-2222-2222-222222222222");

        String id1 = Message.generateConversationId(alice, bob);
        String id2 = Message.generateConversationId(bob, alice);

        assertEquals(id1, id2, "無論順序，對話 ID 應該相同");
        System.out.println("✅ 對話 ID 生成測試通過！");
        System.out.println("   對話 ID: " + id1);
    }
}