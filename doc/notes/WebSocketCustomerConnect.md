架構總覽
```
客戶端發起連線：ws://localhost:8080/ws/chat?token=xxx
         ↓
JwtHandshakeInterceptor（驗證 Token）
         ↓
ChatWebSocketHandler.afterConnectionEstablished
         ↓
WebSocketSessionManager.addSession（註冊連線）
         ↓
發送 CONNECTION_ESTABLISHED 訊息
         ↓
連線建立完成 ✅