package com.chathub.controller;

import com.chathub.dto.ApiResponse;
import com.chathub.entity.Conversation;
import com.chathub.entity.Message;
import com.chathub.service.ConversationService;
import com.chathub.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 對話與訊息 API
 */
@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final MessageService messageService;

    /**
     * 查詢使用者的對話列表
     * GET /api/v1/conversations?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<Conversation>>> getConversations(
        @AuthenticationPrincipal UserDetails userDetails,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        Pageable pageable = PageRequest.of(page, size);

        Page<Conversation> conversations = conversationService
            .getUserConversations(userId, pageable);

        return ResponseEntity.ok(ApiResponse.success("查詢成功", conversations));
    }

    /**
     * 查詢對話的歷史訊息
     * GET /api/v1/conversations/{conversationId}/messages?page=0&size=20
     */
    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<ApiResponse<Page<Message>>> getMessages(
        @PathVariable String conversationId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messages = messageService
            .getConversationMessages(conversationId, pageable);

        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    /**
     * 標記對話訊息為已讀
     * PUT /api/v1/conversations/{conversationId}/read
     */
    @PutMapping("/{conversationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
        @AuthenticationPrincipal UserDetails userDetails,
        @PathVariable String conversationId
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        messageService.markMessagesAsRead(conversationId, userId);

        return ResponseEntity.ok(ApiResponse.success("訊息已標記為已讀",null));
    }

    /**
     * 查詢使用者的未讀訊息總數
     * GET /api/v1/conversations/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        long count = messageService.getUnreadCount(userId);

        return ResponseEntity.ok(ApiResponse.success(count));
    }

}