package com.chathub.config;

import com.chathub.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.UUID;

/**
 * WebSocket 握手攔截器
 * 在建立 WebSocket 連線前驗證 JWT Token
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 握手前處理
     * 從 URL 參數中提取 Token 並驗證
     */
    @Override
    public boolean beforeHandshake(
        ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler wsHandler,
        Map<String, Object> attributes
    ) throws Exception {

        log.info("WebSocket 握手請求：{}", request.getURI());

        try {
            // 從 URL 參數提取 Token
            // 格式：ws://localhost:8080/ws/chat?token=xxx
            String query = request.getURI().getQuery();
            if (query == null || !query.contains("token=")) {
                log.warn("WebSocket 握手失敗：缺少 token 參數");
                return false;
            }

            // 解析 token 參數
            String token = extractTokenFromQuery(query);
            if (token == null || token.isEmpty()) {
                log.warn("WebSocket 握手失敗：token 為空");
                return false;
            }

            // 驗證 Token
            if (!jwtTokenProvider.validateToken(token)) {
                log.warn("WebSocket 握手失敗：token 無效或過期");
                return false;
            }

            // 提取使用者資訊
            Claims claims = jwtTokenProvider.getClaimsFromToken(token);
            String userIdStr = claims.getSubject();
            String username = claims.get("username", String.class);
            UUID userId = UUID.fromString(userIdStr);

            // 將使用者資訊存入 WebSocket Session 屬性
            attributes.put("userId", userId);
            attributes.put("username", username);
            attributes.put("token", token);

            log.info("WebSocket 握手成功：使用者 {} (ID: {})", username, userId);
            return true;

        } catch (Exception e) {
            log.error("WebSocket 握手異常：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 握手後處理
     */
    @Override
    public void afterHandshake(
        ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler wsHandler,
        Exception exception
    ) {
        if (exception != null) {
            log.error("WebSocket 握手後異常：{}", exception.getMessage());
        }
    }

    /**
     * 從 Query String 提取 token
     * 格式：token=xxx&other=yyy
     */
    private String extractTokenFromQuery(String query) {
        String[] params = query.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2 && "token".equals(keyValue[0])) {
                return keyValue[1];
            }
        }
        return null;
    }
}