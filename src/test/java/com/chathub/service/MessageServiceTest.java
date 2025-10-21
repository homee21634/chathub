package com.chathub.service;

import com.chathub.entity.Message;
import com.chathub.entity.User;
import com.chathub.repository.MessageRepository;
import com.chathub.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MessageService 測試
 */
@SpringBootTest
@Transactional
public class MessageServiceTest {

    @Autowired
    private MessageService messageService;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    private User testSender;
    private User testRecipient;

    @BeforeEach
    public void setup() {
        // 清理測試資料
        messageRepository.deleteAll();

        // 建立測試使用者（縮短名稱至 20 字元內）
        testSender = User.builder()
                         .username("sender" + System.currentTimeMillis() % 10000)  // 最多 10 位數
                         .passwordHash("dummy_hash")
                         .isActive(true)
                         .isOnline(false)
                         .build();
        testSender = userRepository.saveAndFlush(testSender);

        testRecipient = User.builder()
                            .username("recipient" + System.currentTimeMillis() % 10000)
                            .passwordHash("dummy_hash")
                            .isActive(true)
                            .isOnline(false)
                            .build();
        testRecipient = userRepository.saveAndFlush(testRecipient);
    }

    @AfterEach
    public void cleanup() {
        messageRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testSaveMessage() {
        // 發送訊息
        Message message = messageService.saveMessage(
            testSender.getUserId(),
            testRecipient.getUserId(),
            "測試訊息內容",
            "client-msg-001"
        );

        assertNotNull(message, "訊息應該被成功儲存");
        assertNotNull(message.getMessageId(), "訊息應該有 ID");
        assertEquals("測試訊息內容", message.getContent());
        assertEquals(testSender.getUsername(), message.getSenderUsername());
        assertFalse(message.getIsRead(), "新訊息應該是未讀狀態");

        System.out.println("✅ 訊息儲存測試通過！");
        System.out.println("   訊息 ID: " + message.getMessageId());
        System.out.println("   對話 ID: " + message.getConversationId());
    }

    @Test
    public void testDuplicateMessagePrevention() {
        // 第一次發送
        String clientMsgId = "client-unique-001";
        Message msg1 = messageService.saveMessage(
            testSender.getUserId(),
            testRecipient.getUserId(),
            "第一次發送",
            clientMsgId
        );

        // 第二次發送（重複）
        Message msg2 = messageService.saveMessage(
            testSender.getUserId(),
            testRecipient.getUserId(),
            "第二次發送（應該被忽略）",
            clientMsgId
        );

        // 應該返回相同的訊息
        assertEquals(msg1.getMessageId(), msg2.getMessageId(), "重複訊息應該返回原始訊息");
        assertEquals("第一次發送", msg2.getContent(), "內容應該是第一次的內容");

        // 資料庫中應該只有一則訊息
        long count = messageRepository.count();
        assertEquals(1, count, "資料庫中應該只有一則訊息");

        System.out.println("✅ 訊息去重測試通過！");
    }

    @Test
    public void testGetConversationMessages() {
        // 發送多則訊息
        for (int i = 1; i <= 5; i++) {
            messageService.saveMessage(
                testSender.getUserId(),
                testRecipient.getUserId(),
                "訊息 " + i,
                "client-msg-" + i
            );
        }

        // 查詢對話訊息（分頁）
        String conversationId = Message.generateConversationId(
            testSender.getUserId(),
            testRecipient.getUserId()
        );

        Page<Message> messages = messageService.getConversationMessages(
            conversationId,
            PageRequest.of(0, 3)  // 第一頁，每頁 3 則
        );

        assertEquals(3, messages.getContent().size(), "應該返回 3 則訊息");
        assertEquals(5, messages.getTotalElements(), "總共應該有 5 則訊息");
        assertEquals(2, messages.getTotalPages(), "應該有 2 頁");

        System.out.println("✅ 查詢對話訊息測試通過！");
        System.out.println("   查詢到 " + messages.getContent().size() + " 則訊息");
    }

    @Test
    public void testMarkMessagesAsRead() {
        // 發送 3 則未讀訊息
        for (int i = 1; i <= 3; i++) {
            messageService.saveMessage(
                testSender.getUserId(),
                testRecipient.getUserId(),
                "未讀訊息 " + i,
                "client-msg-" + i
            );
        }

        String conversationId = Message.generateConversationId(
            testSender.getUserId(),
            testRecipient.getUserId()
        );

        // 檢查未讀數
        long unreadBefore = messageService.getConversationUnreadCount(
            conversationId,
            testRecipient.getUserId()
        );
        assertEquals(3, unreadBefore, "應該有 3 則未讀訊息");

        // 標記為已讀
        messageService.markMessagesAsRead(conversationId, testRecipient.getUserId());

        // 檢查未讀數
        long unreadAfter = messageService.getConversationUnreadCount(
            conversationId,
            testRecipient.getUserId()
        );
        assertEquals(0, unreadAfter, "未讀數應該變為 0");

        System.out.println("✅ 標記已讀測試通過！");
    }

    @Test
    public void testGetUnreadCount() {
        // testRecipient 收到來自不同使用者的訊息
        User anotherUser = User.builder()
                               .username("another_user")
                               .passwordHash("dummy_hash")
                               .isActive(true)
                               .build();
        anotherUser = userRepository.save(anotherUser);

        // testSender 發送 2 則
        messageService.saveMessage(
            testSender.getUserId(),
            testRecipient.getUserId(),
            "訊息 1",
            "client-1"
        );
        messageService.saveMessage(
            testSender.getUserId(),
            testRecipient.getUserId(),
            "訊息 2",
            "client-2"
        );

        // anotherUser 發送 3 則
        messageService.saveMessage(
            anotherUser.getUserId(),
            testRecipient.getUserId(),
            "訊息 3",
            "client-3"
        );
        messageService.saveMessage(
            anotherUser.getUserId(),
            testRecipient.getUserId(),
            "訊息 4",
            "client-4"
        );
        messageService.saveMessage(
            anotherUser.getUserId(),
            testRecipient.getUserId(),
            "訊息 5",
            "client-5"
        );

        // testRecipient 的總未讀數應該是 5
        long totalUnread = messageService.getUnreadCount(testRecipient.getUserId());
        assertEquals(5, totalUnread, "testRecipient 應該有 5 則未讀訊息");

        System.out.println("✅ 查詢未讀數測試通過！");
    }
}