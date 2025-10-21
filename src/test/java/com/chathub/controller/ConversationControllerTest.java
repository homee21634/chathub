package com.chathub.controller;

import com.chathub.entity.User;
import com.chathub.repository.ConversationRepository;
import com.chathub.repository.MessageRepository;
import com.chathub.repository.UserRepository;
import com.chathub.security.JwtTokenProvider;
import com.chathub.service.MessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * ConversationController 整合測試
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageService messageService;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser1;
    private User testUser2;
    private String user1Token;
    private String user2Token;

    @BeforeEach
    public void setup() {
        // 清理測試資料
        messageRepository.deleteAll();
        conversationRepository.deleteAll();

        // 建立測試使用者（縮短名稱）
        long timestamp = System.currentTimeMillis() % 10000;

        testUser1 = User.builder()
                        .username("user1_" + timestamp)  // 最多 11 位數
                        .passwordHash(passwordEncoder.encode("Password123!"))
                        .isActive(true)
                        .isOnline(false)
                        .build();
        testUser1 = userRepository.saveAndFlush(testUser1);

        testUser2 = User.builder()
                        .username("user2_" + timestamp)
                        .passwordHash(passwordEncoder.encode("Password123!"))
                        .isActive(true)
                        .isOnline(false)
                        .build();
        testUser2 = userRepository.saveAndFlush(testUser2);

        // 生成 JWT Token
        user1Token = jwtTokenProvider.generateAccessToken(
            testUser1.getUserId(),
            testUser1.getUsername()
        );
        user2Token = jwtTokenProvider.generateAccessToken(
            testUser2.getUserId(),
            testUser2.getUsername()
        );

        // 建立測試訊息
        for (int i = 1; i <= 5; i++) {
            messageService.saveMessage(
                testUser1.getUserId(),
                testUser2.getUserId(),
                "測試訊息 " + i,
                "client-msg-" + i
            );
        }
    }

    @AfterEach
    public void cleanup() {
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testGetConversations() throws Exception {
        mockMvc.perform(get("/api/v1/conversations")
                            .header("Authorization", "Bearer " + user2Token)
                            .param("page", "0")
                            .param("size", "20"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.success").value(true))
               .andExpect(jsonPath("$.data.content").isArray())
               .andExpect(jsonPath("$.data.content", hasSize(1)))
               .andExpect(jsonPath("$.data.content[0].conversationId").exists())
               .andExpect(jsonPath("$.data.content[0].participants", hasSize(2)))
               .andExpect(jsonPath("$.data.content[0].lastMessage.content").value("測試訊息 5"));

        System.out.println("✅ 查詢對話列表 API 測試通過！");
    }

    @Test
    public void testGetMessages() throws Exception {
        // 取得對話 ID
        String conversationId = com.chathub.entity.Message.generateConversationId(
            testUser1.getUserId(),
            testUser2.getUserId()
        );

        mockMvc.perform(get("/api/v1/conversations/{conversationId}/messages", conversationId)
                            .header("Authorization", "Bearer " + user1Token)
                            .param("page", "0")
                            .param("size", "20"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.success").value(true))
               .andExpect(jsonPath("$.data.content").isArray())
               .andExpect(jsonPath("$.data.content", hasSize(5)))
               .andExpect(jsonPath("$.data.totalElements").value(5));

        System.out.println("✅ 查詢歷史訊息 API 測試通過！");
    }

    @Test
    public void testMarkAsRead() throws Exception {
        String conversationId = com.chathub.entity.Message.generateConversationId(
            testUser1.getUserId(),
            testUser2.getUserId()
        );

        // 標記為已讀
        mockMvc.perform(put("/api/v1/conversations/{conversationId}/read", conversationId)
                            .header("Authorization", "Bearer " + user2Token))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.success").value(true))
               .andExpect(jsonPath("$.message").value("訊息已標記為已讀"));

        // 驗證未讀數已清零
        long unreadCount = messageService.getConversationUnreadCount(
            conversationId,
            testUser2.getUserId()
        );

        assert unreadCount == 0 : "未讀數應該為 0";

        System.out.println("✅ 標記已讀 API 測試通過！");
    }

    @Test
    public void testGetUnreadCount() throws Exception {
        mockMvc.perform(get("/api/v1/conversations/unread-count")
                            .header("Authorization", "Bearer " + user2Token))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.success").value(true))
               .andExpect(jsonPath("$.data").value(5));  // user2 有 5 則未讀

        System.out.println("✅ 查詢未讀數 API 測試通過！");
    }

    @Test
    public void testUnauthorizedAccess() throws Exception {
        // 403 是正確的（Spring Security 預設行為）
        mockMvc.perform(get("/api/v1/conversations"))
               .andExpect(status().isForbidden());  // 改為 403

        System.out.println("✅ 未授權存取測試通過！");
    }

    @Test
    public void testPagination() throws Exception {
        // 建立更多訊息
        for (int i = 6; i <= 25; i++) {
            messageService.saveMessage(
                testUser1.getUserId(),
                testUser2.getUserId(),
                "訊息 " + i,
                "client-msg-" + i
            );
        }

        String conversationId = com.chathub.entity.Message.generateConversationId(
            testUser1.getUserId(),
            testUser2.getUserId()
        );

        // 測試第一頁
        mockMvc.perform(get("/api/v1/conversations/{conversationId}/messages", conversationId)
                            .header("Authorization", "Bearer " + user1Token)
                            .param("page", "0")
                            .param("size", "10"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.data.content", hasSize(10)))
               .andExpect(jsonPath("$.data.totalElements").value(25))
               .andExpect(jsonPath("$.data.totalPages").value(3));

        // 測試第二頁
        mockMvc.perform(get("/api/v1/conversations/{conversationId}/messages", conversationId)
                            .header("Authorization", "Bearer " + user1Token)
                            .param("page", "1")
                            .param("size", "10"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.data.content", hasSize(10)))
               .andExpect(jsonPath("$.data.number").value(1));  // 頁碼

        System.out.println("✅ 分頁查詢測試通過！");
    }
}