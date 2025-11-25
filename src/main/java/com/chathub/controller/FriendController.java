package com.chathub.controller;

import com.chathub.dto.ApiResponse;
import com.chathub.dto.friend.FriendInfoDto;
import com.chathub.dto.friend.FriendRequestResponseDto;
import com.chathub.dto.friend.HandleFriendRequestDto;
import com.chathub.dto.friend.SendFriendRequestDto;
import com.chathub.service.FriendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    /**
     * 1. 發送好友請求
     * POST /api/v1/friends/requests
     */
    @PostMapping("/requests")
    public ResponseEntity<ApiResponse<FriendRequestResponseDto>> sendFriendRequest(
        @Valid @RequestBody SendFriendRequestDto request) {

        try {
            UUID userId = getCurrentUserId();
            FriendRequestResponseDto response = friendService.sendFriendRequest(userId, request.getUsername());
            return ResponseEntity.ok(ApiResponse.success( "好友請求已發送",response));

        } catch (Exception e) {
            log.error("發送好友請求失敗", e);
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 2. 查看收到的好友請求
     * GET /api/v1/friends/requests/received
     */
    @GetMapping("/requests/received")
    public ResponseEntity<ApiResponse<List<FriendRequestResponseDto>>> getReceivedRequests() {
        try {
            UUID userId = getCurrentUserId();
            log.info("開始查詢好友請求 - UserId: {}", userId);

            List<FriendRequestResponseDto> requests = friendService.getReceivedRequests(userId);

            log.info("Service 層返回成功，共 {} 筆請求", requests.size());
            log.info("準備返回 ResponseEntity，開始 JSON 序列化...");

            ResponseEntity<ApiResponse<List<FriendRequestResponseDto>>> response =
                ResponseEntity.ok(ApiResponse.success("查詢成功", requests));

            log.info("ResponseEntity 創建成功，準備返回給客戶端");
            return response;

        } catch (Exception e) {
            log.error("查詢好友請求失敗 - 詳細錯誤: {}", e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("查詢失敗: " + e.getMessage()));
        }
    }

    /**
     * 3. 處理好友請求（接受/拒絕）
     * PUT /api/v1/friends/requests/{id}
     */
    @PutMapping("/requests/{id}")
    public ResponseEntity<ApiResponse<Void>> handleFriendRequest(
        @PathVariable("id") UUID requestId,
        @Valid @RequestBody HandleFriendRequestDto request) {

        try {
            UUID userId = getCurrentUserId();

            if ("accept".equalsIgnoreCase(request.getAction())) {
                friendService.acceptFriendRequest(requestId, userId);
                return ResponseEntity.ok(ApiResponse.success( "已接受好友請求",null));

            } else if ("reject".equalsIgnoreCase(request.getAction())) {
                friendService.rejectFriendRequest(requestId, userId);
                return ResponseEntity.ok(ApiResponse.success( "已拒絕好友請求",null));

            } else {
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("action 必須為 accept 或 reject"));
            }

        } catch (Exception e) {
            log.error("處理好友請求失敗", e);
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 4. 查看好友列表
     * GET /api/v1/friends
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FriendInfoDto>>> getFriendsList() {
        try {
            UUID userId = getCurrentUserId();
            List<FriendInfoDto> friends = friendService.getFriendsList(userId);
            return ResponseEntity.ok(ApiResponse.success("查詢成功",friends));

        } catch (Exception e) {
            log.error("查詢好友列表失敗", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("查詢失敗"));
        }
    }

    /**
     * 5. 刪除好友
     * DELETE /api/v1/friends/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFriend(@PathVariable("id") UUID friendId) {
        try {
            UUID userId = getCurrentUserId();
            friendService.deleteFriend(userId, friendId);
            return ResponseEntity.ok(ApiResponse.success( "已刪除好友",null));

        } catch (Exception e) {
            log.error("刪除好友失敗", e);
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 從 SecurityContext 取得當前使用者 ID
     */
    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("未找到認證信息");
        }

        Object principal = authentication.getPrincipal();
        String userId;

        // JWT 過濾器設置的是 UserDetails 物件
        if (principal instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) principal;
            userId = userDetails.getUsername();  // username 實際存儲的是 userId
        }
        // 向後兼容：如果 principal 直接是 String
        else if (principal instanceof String) {
            userId = (String) principal;
        }
        // 不支持的類型
        else {
            throw new IllegalStateException(
                "不支持的 principal 類型: " + principal.getClass().getName()
            );
        }

        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("無效的用戶 ID 格式: " + userId, e);
        }
    }
}