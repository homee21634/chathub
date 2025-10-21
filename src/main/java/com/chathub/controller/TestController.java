package com.chathub.controller;

import com.chathub.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 測試受保護端點的 Controller
 */
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestController {

    /**
     * 測試 JWT 認證是否正常運作
     *
     * GET /api/v1/test/protected
     * Header: Authorization: Bearer {access-token}
     */
    @GetMapping("/protected")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testProtectedEndpoint() {
        // 從 SecurityContext 取得當前使用者資訊
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String userId = (String) authentication.getPrincipal();
        String username = (String) authentication.getDetails();

        Map<String, Object> data = new HashMap<>();
        data.put("message", "您已成功通過 JWT 認證！");
        data.put("userId", userId);
        data.put("username", username);
        data.put("authenticated", authentication.isAuthenticated());

        return ResponseEntity.ok(ApiResponse.success( "JWT 認證成功",data));
    }

    /**
     * 測試取得當前使用者資訊
     *
     * GET /api/v1/test/me
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, String>>> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Map<String, String> data = new HashMap<>();
        data.put("userId", (String) authentication.getPrincipal());
        data.put("username", (String) authentication.getDetails());

        return ResponseEntity.ok(ApiResponse.success( "使用者資訊",data));
    }
}