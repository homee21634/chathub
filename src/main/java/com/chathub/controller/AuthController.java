package com.chathub.controller;

import com.chathub.dto.LoginRequest;
import com.chathub.dto.ApiResponse;
import com.chathub.dto.LoginResponse;
import com.chathub.dto.RegisterRequest;
import com.chathub.dto.RegisterResponse;
import com.chathub.dto.auth.RefreshTokenRequest;
import com.chathub.exception.AuthenticationException;
import com.chathub.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest httpRequest) {

        String ipAddress = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");

        LoginResponse response = userService.login(request, ipAddress, userAgent);

        return ResponseEntity.ok(ApiResponse.success( "登入成功",response));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
        @Valid @RequestBody RegisterRequest request) {

        RegisterResponse response = userService.registerUser(request);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success("註冊成功", response));
    }

    @GetMapping("/check-username")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkUsername(
        @RequestParam String username) {

        boolean available = userService.isUsernameAvailable(username);
        String message = available ? "此帳號可以使用" : "此帳號已被使用";

        return ResponseEntity.ok(
            ApiResponse.success(message, Map.of("available", available))
        );
    }

    /**
     * Token 刷新端點
     *
     * POST /api/v1/auth/refresh
     *
     * Request Body:
     * {
     *   "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI..."
     * }
     *
     * Response:
     * {
     *   "userId": "uuid",
     *   "username": "alice_wang",
     *   "accessToken": "new-access-token",
     *   "refreshToken": "new-refresh-token (如果輪換)",
     *   "expiresIn": 3600
     * }
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
        @Valid @RequestBody RefreshTokenRequest request,
        HttpServletRequest httpRequest) {

        try {
            // 取得裝置資訊與 IP
            String deviceInfo = httpRequest.getHeader("User-Agent");
            String ipAddress = getClientIpAddress(httpRequest);

            // 刷新 Token
            LoginResponse response = userService.refreshAccessToken(
                request.getRefreshToken(),
                deviceInfo,
                ipAddress
            );

            return ResponseEntity.ok(ApiResponse.success( "Token 刷新成功",response));

        } catch (AuthenticationException e) {
            log.warn("Token 刷新失敗: {}", e.getMessage());
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Token 刷新異常", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Token 刷新失敗，請稍後再試"));
        }
    }

    /**
     * 取得客戶端真實 IP（處理代理情況）
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果有多個代理，取第一個
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 登出端點
     *
     * POST /api/v1/auth/logout
     * Header: Authorization: Bearer {access-token}
     *
     * Response:
     * {
     *   "success": true,
     *   "message": "登出成功",
     *   "data": null
     * }
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        try {
            // 從 Header 提取 Token
            String token = extractTokenFromRequest(request);

            if (token == null) {
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("缺少 Authorization Token"));
            }

            // 執行登出
            userService.logout(token);

            return ResponseEntity.ok(ApiResponse.success( "登出成功",null));

        } catch (AuthenticationException e) {
            log.warn("登出失敗: {}", e.getMessage());
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("登出異常", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("登出失敗，請稍後再試"));
        }
    }

    /**
     * 從 Request 提取 Token（與 Filter 相同邏輯）
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}