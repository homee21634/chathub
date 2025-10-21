package com.chathub.config;

import com.chathub.handler.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
            // 註冊 WebSocket 端點
            .addHandler(chatWebSocketHandler, "/ws/chat")

            // 新增握手攔截器（JWT 驗證）
            .addInterceptors(jwtHandshakeInterceptor)

            // 允許跨域（開發環境，生產環境需要限制）
            .setAllowedOrigins("*");

        // 生產環境應該使用：
        // .setAllowedOrigins("https://your-domain.com");
    }
}
