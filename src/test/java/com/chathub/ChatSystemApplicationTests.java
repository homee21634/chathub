package com.chathub;

import com.chathub.dto.LoginRequest;
import com.chathub.dto.RegisterRequest;
import com.chathub.dto.friend.FriendInfoDto;
import com.chathub.dto.friend.FriendRequestResponseDto;
import com.chathub.dto.friend.HandleFriendRequestDto;
import com.chathub.dto.friend.SendFriendRequestDto;
import com.chathub.dto.ApiResponse;
import com.chathub.dto.LoginResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ChatSystemApplication.class)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChatSystemApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String userAToken;
    private static String userBToken;
    private static String userAName;  // 新增
    private static String userBName;  // 新增
    private static UUID requestId;
    private static UUID friendId;

    @Test
    @Order(1)
    @DisplayName("1. 註冊使用者 A")
    void registerUserA() throws Exception {
        userAName = "alice" + (System.currentTimeMillis() % 10000);

        RegisterRequest request = new RegisterRequest();
        request.setUsername(userAName);
        request.setPassword("Test1234!");
        request.setConfirmPassword("Test1234!");

        mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isCreated())  // 改為 isCreated()，對應 201
               .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(2)
    @DisplayName("2. 註冊使用者 B")
    void registerUserB() throws Exception {
        userBName = "bob" + (System.currentTimeMillis() % 10000);

        RegisterRequest request = new RegisterRequest();
        request.setUsername(userBName);
        request.setPassword("Test1234!");
        request.setConfirmPassword("Test1234!");

        mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isCreated())  // 改為 isCreated()，對應 201
               .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(3)
    @DisplayName("3. 使用者 A 登入並取得 Token")
    void loginUserA() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername(userAName);  // 使用註冊時的用戶名
        request.setPassword("Test1234!");
        request.setRememberMe(true);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                                               .contentType(MediaType.APPLICATION_JSON)
                                               .content(objectMapper.writeValueAsString(request)))
                                  .andExpect(status().isOk())
                                  .andExpect(jsonPath("$.success").value(true))
                                  .andReturn();

        String json = result.getResponse().getContentAsString();
        ApiResponse<LoginResponse> response = objectMapper.readValue(json,
                                                                     new TypeReference<ApiResponse<LoginResponse>>() {});

        userAToken = response.getData().getAccessToken();
        assertNotNull(userAToken);
    }

    @Test
    @Order(4)
    @DisplayName("4. 使用者 B 登入並取得 Token")
    void loginUserB() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername(userBName);  // 使用註冊時的用戶名
        request.setPassword("Test1234!");
        request.setRememberMe(true);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                                               .contentType(MediaType.APPLICATION_JSON)
                                               .content(objectMapper.writeValueAsString(request)))
                                  .andExpect(status().isOk())
                                  .andReturn();

        String json = result.getResponse().getContentAsString();
        ApiResponse<LoginResponse> response = objectMapper.readValue(json,
                                                                     new TypeReference<ApiResponse<LoginResponse>>() {});

        userBToken = response.getData().getAccessToken();
        assertNotNull(userBToken);
    }

    @org.junit.jupiter.api.Test
    @Order(5)
    @DisplayName("5. 使用者 A 發送好友請求給 B")
    void sendFriendRequest() throws Exception {
        SendFriendRequestDto request = new SendFriendRequestDto();
        request.setUsername(userBName);

        MvcResult result = mockMvc.perform(post("/api/v1/friends/requests")
                                               .header("Authorization", "Bearer " + userAToken)
                                               .contentType(MediaType.APPLICATION_JSON)
                                               .content(objectMapper.writeValueAsString(request)))
                                  .andExpect(status().isOk())
                                  .andExpect(jsonPath("$.success").value(true))
                                  .andReturn();

        String json = result.getResponse().getContentAsString();
        ApiResponse<FriendRequestResponseDto> response = objectMapper.readValue(json,
                                                                                new TypeReference<ApiResponse<FriendRequestResponseDto>>() {});

        requestId = response.getData().getRequestId();
        assertNotNull(requestId);
    }

    @org.junit.jupiter.api.Test
    @Order(6)
    @DisplayName("6. 使用者 B 查看收到的好友請求")
    void getReceivedRequests() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/friends/requests/received")
                                               .header("Authorization", "Bearer " + userBToken))
                                  .andExpect(status().isOk())
                                  .andExpect(jsonPath("$.success").value(true))
                                  .andReturn();

        String json = result.getResponse().getContentAsString();
        ApiResponse<List<FriendRequestResponseDto>> response = objectMapper.readValue(json,
                                                                                      new TypeReference<ApiResponse<List<FriendRequestResponseDto>>>() {});

        assertTrue(response.getData().size() > 0);
    }

    @org.junit.jupiter.api.Test
    @Order(7)
    @DisplayName("7. 使用者 B 接受好友請求")
    void acceptFriendRequest() throws Exception {
        HandleFriendRequestDto request = new HandleFriendRequestDto();
        request.setAction("accept");

        mockMvc.perform(put("/api/v1/friends/requests/" + requestId)
                            .header("Authorization", "Bearer " + userBToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.success").value(true));
    }

    @org.junit.jupiter.api.Test
    @Order(8)
    @DisplayName("8. 使用者 A 查看好友列表")
    void getFriendsListA() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/friends")
                                               .header("Authorization", "Bearer " + userAToken))
                                  .andExpect(status().isOk())
                                  .andExpect(jsonPath("$.success").value(true))
                                  .andReturn();

        String json = result.getResponse().getContentAsString();
        ApiResponse<List<FriendInfoDto>> response = objectMapper.readValue(json,
                                                                           new TypeReference<ApiResponse<List<FriendInfoDto>>>() {});

        assertEquals(1, response.getData().size());
        friendId = response.getData().get(0).getUserId();
    }

    @org.junit.jupiter.api.Test
    @Order(9)
    @DisplayName("9. 使用者 B 查看好友列表")
    void getFriendsListB() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/friends")
                                               .header("Authorization", "Bearer " + userBToken))
                                  .andExpect(status().isOk())
                                  .andReturn();

        String json = result.getResponse().getContentAsString();
        ApiResponse<List<FriendInfoDto>> response = objectMapper.readValue(json,
                                                                           new TypeReference<ApiResponse<List<FriendInfoDto>>>() {});

        assertEquals(1, response.getData().size());
    }

    @org.junit.jupiter.api.Test
    @Order(10)
    @DisplayName("10. 使用者 A 刪除好友")
    void deleteFriend() throws Exception {
        mockMvc.perform(delete("/api/v1/friends/" + friendId)
                            .header("Authorization", "Bearer " + userAToken))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.success").value(true));
    }

    @org.junit.jupiter.api.Test
    @Order(11)
    @DisplayName("11. 驗證好友已刪除")
    void verifyFriendDeleted() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/friends")
                                               .header("Authorization", "Bearer " + userAToken))
                                  .andExpect(status().isOk())
                                  .andReturn();

        String json = result.getResponse().getContentAsString();
        ApiResponse<List<FriendInfoDto>> response = objectMapper.readValue(json,
                                                                           new TypeReference<ApiResponse<List<FriendInfoDto>>>() {});

        assertEquals(0, response.getData().size());
    }

    @org.junit.jupiter.api.Test
    @Order(12)
    @DisplayName("12. 錯誤測試：不能加自己為好友")
    void cannotAddSelfAsFriend() throws Exception {
        SendFriendRequestDto request = new SendFriendRequestDto();
        request.setUsername(userAName);

        mockMvc.perform(post("/api/v1/friends/requests")
                            .header("Authorization", "Bearer " + userAToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isBadRequest());
    }

    @Test
    @Order(13)
    @DisplayName("13. 錯誤測試：目標使用者不存在")
    void targetUserNotFound() throws Exception {
        SendFriendRequestDto request = new SendFriendRequestDto();
        request.setUsername("notexist_user");

        mockMvc.perform(post("/api/v1/friends/requests")
                            .header("Authorization", "Bearer " + userAToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isBadRequest());
    }
}
