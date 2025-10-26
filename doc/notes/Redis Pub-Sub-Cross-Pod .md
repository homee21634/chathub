# Redis Pub/Sub 跨 Pod 通訊技術說明

## 🎯 核心問題

**單一 Pod 限制：**
- User A 連到 Pod 1
- User B 連到 Pod 3
- User A 發訊息 → 只存在 Pod 1 記憶體
- User B 收不到 ❌

## 💡 解決方案：Redis Pub/Sub

**工作原理：**
```
User A (Pod 1) 發訊息
    ↓
儲存到 MongoDB
    ↓
發布到 Redis: PUBLISH user:{UserB_ID} {message}
    ↓
所有 Pod 都訂閱 Redis 頻道
    ↓
Pod 3 收到通知 → 推送給 User B ✅
```

## 🔧 技術架構

**3 個核心組件：**

1. **RedisMessagePublisher** - 發布訊息到 Redis
2. **RedisMessageSubscriber** - 訂閱 Redis 頻道，接收訊息
3. **修改 ChatWebSocketHandler** - 整合 Pub/Sub

## 📊 訊息流程

```
發送流程：
User A 輸入 → WebSocket → Pod 1 Handler
    → 存 MongoDB
    → Redis PUBLISH
    → 所有 Pod 收到
    → Pod 3 找到 User B 的 WebSocket
    → 推送 ✅

接收流程：
Redis Subscriber (監聽所有 user:* 頻道)
    → 收到訊息
    → 解析 JSON
    → 找到對應 WebSocket Session
    → 發送給使用者
```

準備好開始實作了嗎？ 🚀