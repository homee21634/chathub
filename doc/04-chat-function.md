# 簡易聊天系統開發文件 - 聊天功能

**文件版本：** 1.0  
**最後更新：** 2025-09-30  
**狀態：** 待審核

---

## 目錄

1. [UI/UX 設計 - 聊天頁面](#1-uiux-設計---聊天頁面)
2. [後端 API 文件 - 聊天功能](#2-後端-api-文件---聊天功能)
3. [WebSocket 通訊協定](#3-websocket-通訊協定)
4. [系統架構圖 - 聊天系統](#4-系統架構圖---聊天系統)
5. [資料庫架構 (MongoDB)](#5-資料庫架構-mongodb)
6. [訊息快取策略 (Redis)](#6-訊息快取策略-redis)
7. [後端實作架構 (Spring Boot)](#7-後端實作架構-spring-boot)
8. [前端實作架構 (React)](#8-前端實作架構-react)
9. [即時通訊機制](#9-即時通訊機制)
10. [效能優化與擴展](#10-效能優化與擴展)
11. [文件版本記錄](#11-文件版本記錄)

---

## 1. UI/UX 設計 - 聊天頁面

### 1.1 頁面佈局設計

**頁面名稱：** 聊天與對話頁面 (Chat & Conversation)

**設計規格：**

- **版面配置：** 雙欄式佈局（左側對話列表 + 右側聊天視窗）
- **響應式設計：**
  - 桌面版 (1200px+)：雙欄並排
  - 手機版 (< 768px)：單欄切換
- **色彩方案：**
  - 主色：#1a1a1a (深黑)
  - 背景：#fafafa (淺灰)
  - 卡片背景：#ffffff (白色)
  - 自己的訊息：#1a1a1a (黑色氣泡)
  - 對方的訊息：#ffffff (白色氣泡)

### 1.2 左側欄 - 對話列表區域

#### 1. 頂部標題列（深色背景）
**元素：**
- 標題：「訊息」（18px，粗體，白色）
- 操作按鈕：
  - 👥 返回好友列表
  - 🚪 登出

#### 2. 分頁載入策略
- 每次載入 50 則訊息
- 滾動到頂部時自動載入更多
- Redis 快取最新 100 則（快速載入）
- 較舊訊息從 MongoDB 查詢

#### 3. 訊息順序保證
- MongoDB 使用單調遞增的時間戳記
- 前端按時間排序顯示
- 處理網路延遲導致的亂序問題

### 9.3 連線狀態管理

#### 1. 心跳機制
```javascript
// 前端每 30 秒發送 PING
setInterval(() => {
  websocket.send({ type: 'PING' });
}, 30000);

// 後端回應 PONG
// 超過 60 秒未收到 PING，斷開連線
```

#### 2. 自動重連策略
- 指數退避算法：1s, 2s, 4s, 8s, 16s, 30s
- 最多重試 5 次
- 重連後自動重新訂閱

#### 3. 斷線處理
```
斷線 → 顯示「連線中斷」提示 → 嘗試重連 → 成功後同步未讀訊息
```

### 9.4 正在輸入指示器

#### 實作流程：
```
User A 輸入 → 節流發送 TYPING_START → User B 顯示「正在輸入...」
             ↓
         5秒後自動過期（Redis TTL）
             ↓
         或收到 TYPING_STOP → User B 移除指示器
```

#### 前端實作：
```javascript
let typingTimeout;

const handleInputChange = (e) => {
  const content = e.target.value;
  
  // 發送正在輸入狀態
  if (!typingTimeout) {
    websocket.sendTypingStart(recipientId);
  }
  
  // 清除舊的計時器
  clearTimeout(typingTimeout);
  
  // 3秒後自動停止
  typingTimeout = setTimeout(() => {
    websocket.sendTypingStop(recipientId);
    typingTimeout = null;
  }, 3000);
};
```

---

## 10. 效能優化與擴展

### 10.1 快取策略

#### 1. 多層快取架構
```
┌─────────────┐
│  前端記憶體  │ ← 最快，當前對話的訊息
└──────┬──────┘
       ↓ Miss
┌─────────────┐
│  Redis 快取 │ ← 快速，最近 100 則訊息
└──────┬──────┘
       ↓ Miss
┌─────────────┐
│  MongoDB    │ ← 完整歷史訊息
└─────────────┘
```

#### 2. 快取更新策略
- **寫入時更新（Write-Through）**
  - 新訊息同時寫入 MongoDB 和 Redis
  - 保證一致性

- **TTL 過期**
  - Redis 快取 7 天後自動過期
  - 減少記憶體使用

- **LRU 淘汰**
  - 只保留最近 100 則訊息
  - 使用 `ZREMRANGEBYRANK` 限制大小

### 10.2 資料庫優化

#### 1. MongoDB 索引優化
```javascript
// 複合索引：對話 + 時間（最常用）
db.messages.createIndex({ conversationId: 1, timestamp: -1 });

// 覆蓋索引：減少回表查詢
db.messages.createIndex(
  { conversationId: 1, timestamp: -1 },
  { name: "conversation_messages_idx" }
);

// 分片鍵（Sharding）
sh.shardCollection("chatdb.messages", { conversationId: "hashed" });
```

#### 2. 查詢優化
```javascript
// 使用投影（Projection）減少資料傳輸
db.messages.find(
  { conversationId: "..." },
  { content: 1, senderId: 1, timestamp: 1, _id: 0 }
);

// 使用 Cursor 處理大量資料
const cursor = db.messages.find({ conversationId: "..." }).sort({ timestamp: -1 });
```

#### 3. 資料歸檔
- 90 天以上的訊息移至冷儲存
- 使用 MongoDB TTL 索引自動刪除（可選）
- 或定期備份後刪除舊資料

### 10.3 WebSocket 擴展

#### 1. 水平擴展（多 Pod 部署）

**挑戰：** 使用者可能連接到不同 Pod

**解決方案：** Redis Pub/Sub 跨 Pod 通訊

```
User A (Pod 1) → 發送訊息 → MongoDB
                           ↓
                    Redis Pub/Sub
                           ↓
User B (Pod 3) ← 接收訊息 ← 訂閱頻道
```

**實作步驟：**
1. 每個 Pod 啟動時訂閱 Redis 頻道
2. 發送訊息時，發布到 Redis
3. 所有 Pod 收到通知，檢查使用者是否在自己的 Session
4. 若在，則推送給使用者

#### 2. 負載均衡

**Sticky Session（會話保持）：**
```yaml
# Kubernetes Ingress 配置
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: chat-ingress
  annotations:
    nginx.ingress.kubernetes.io/affinity: "cookie"
    nginx.ingress.kubernetes.io/session-cookie-name: "ws-route"
spec:
  rules:
  - host: chat.example.com
    http:
      paths:
      - path: /ws/chat
        pathType: Prefix
        backend:
          service:
            name: chat-backend
            port:
              number: 8080
```

#### 3. 連線數監控

**Prometheus Metrics：**
```java
@Service
public class WebSocketMetrics {
    
    private final Counter connectionsTotal = Counter.build()
        .name("websocket_connections_total")
        .help("Total WebSocket connections")
        .register();
    
    private final Gauge activeConnections = Gauge.build()
        .name("websocket_connections_active")
        .help("Active WebSocket connections")
        .register();
    
    public void recordConnection() {
        connectionsTotal.inc();
        activeConnections.inc();
    }
    
    public void recordDisconnection() {
        activeConnections.dec();
    }
}
```

### 10.4 訊息推送優化

#### 1. 批次推送
- 短時間內的多則訊息合併推送
- 減少網路請求次數

#### 2. 壓縮
- 使用 WebSocket 壓縮擴展（permessage-deflate）
- 減少頻寬使用

#### 3. 優先級隊列
```java
@Service
public class MessageQueue {
    
    private final PriorityBlockingQueue<Message> queue = new PriorityBlockingQueue<>(
        100,
        Comparator.comparing(Message::getPriority).reversed()
    );
    
    public void enqueue(Message message) {
        queue.offer(message);
    }
    
    @Scheduled(fixedDelay = 100)
    public void processQueue() {
        Message message = queue.poll();
        if (message != null) {
            deliverMessage(message);
        }
    }
}
```

### 10.5 效能指標

| 指標 | 目標值 | 監控方式 |
|-----|--------|---------|
| 訊息延遲 | < 200ms | Prometheus + Grafana |
| WebSocket 連線數 | 10,000+ per Pod | JMX Metrics |
| 訊息吞吐量 | 1,000 msg/s per Pod | Custom Counter |
| Redis 快取命中率 | > 90% | Redis INFO stats |
| MongoDB 查詢時間 | < 50ms (p95) | MongoDB Profiler |
| CPU 使用率 | < 70% | Kubernetes Metrics |
| 記憶體使用率 | < 80% | Kubernetes Metrics |

---

## 11. 安全性考量

### 11.1 訊息安全

#### 1. 內容驗證與過濾
```java
@Service
public class MessageValidator {
    
    public void validate(String content) {
        // 長度檢查
        if (content.length() > 2000) {
            throw new MessageTooLongException();
        }
        
        // XSS 防護
        content = HtmlUtils.htmlEscape(content);
        
        // 敏感詞過濾（可選）
        content = filterSensitiveWords(content);
        
        // URL 驗證（可選）
        content = validateUrls(content);
    }
}
```

#### 2. 頻率限制（Rate Limiting）
```java
@Service
public class RateLimiter {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    public boolean allowMessage(UUID userId) {
        String key = "rate:message:" + userId;
        Long count = redisTemplate.opsForValue().increment(key);
        
        if (count == 1) {
            redisTemplate.expire(key, 60, TimeUnit.SECONDS);
        }
        
        // 每分鐘最多 30 則訊息
        return count <= 30;
    }
}
```

### 11.2 WebSocket 安全

#### 1. Token 驗證
```java
@Component
public class WebSocketAuthInterceptor extends HandshakeInterceptor {
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Override
    public boolean beforeHandshake(
        ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler wsHandler,
        Map<String, Object> attributes
    ) {
        String token = extractToken(request);
        
        if (token != null && tokenProvider.validateToken(token)) {
            UUID userId = tokenProvider.getUserIdFromToken(token);
            attributes.put("userId", userId);
            return true;
        }
        
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }
}
```

#### 2. CORS 配置
```java
@Configuration
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .setAllowedOrigins("https://yourdomain.com")
                .withSockJS(); // 可選：支援不支援 WebSocket 的瀏覽器
    }
}
```

### 11.3 資料隱私

#### 1. 訊息加密（端對端，可選）
- 使用者生成公私鑰對
- 訊息用接收者公鑰加密
- 伺服器只儲存密文
- 接收者用私鑰解密

#### 2. 資料保留政策
- 訊息保留期限：90 天
- 使用者可刪除訊息
- GDPR 合規：使用者可要求刪除所有資料

---

## 12. 測試策略

### 12.1 單元測試

```java
@SpringBootTest
public class ChatServiceTest {
    
    @Autowired
    private ChatService chatService;
    
    @MockBean
    private MessageRepository messageRepository;
    
    @Test
    public void testSendMessage() {
        UUID senderId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        String content = "Test message";
        
        // Mock 好友關係
        when(friendshipRepository.existsByUserIdAndFriendId(senderId, recipientId))
            .thenReturn(true);
        
        // 執行
        chatService.handleSendMessage(senderId, Map.of(
            "recipientId", recipientId.toString(),
            "content", content,
            "clientMessageId", UUID.randomUUID().toString()
        ));
        
        // 驗證
        verify(messageRepository, times(1)).save(any(Message.class));
    }
}
```

### 12.2 整合測試

```java
@SpringBootTest
@AutoConfigureWebTestClient
public class WebSocketIntegrationTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @Test
    public void testWebSocketConnection() {
        // 建立 WebSocket 連線
        WebSocketClient client = new StandardWebSocketClient();
        
        client.execute(new WebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) {
                // 發送訊息
                session.sendMessage(new TextMessage("{...}"));
            }
            
            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
                // 驗證回應
                assertNotNull(message.getPayload());
            }
        }, "ws://localhost:8080/ws/chat?token=...");
    }
}
```

### 12.3 效能測試

使用 JMeter 或 Gatling 進行壓力測試：

**測試場景：**
1. 1000 個並發 WebSocket 連線
2. 每秒發送 100 則訊息
3. 持續 10 分鐘

**監控指標：**
- 訊息延遲
- 錯誤率
- CPU/記憶體使用率
- 資料庫效能

---

## 13. 文件版本記錄

| 版本 | 日期 | 作者 | 變更說明 |
|-----|------|------|---------|
| 1.0 | 2025-09-30 | Development Team | 初版：聊天功能完整文件 |

---

## 附錄

### A. 相關文件連結

- [註冊功能文件](./01-registration.md)
- [登入功能文件](./02-login.md)
- [好友系統文件](./03-friend-system.md)

### B. WebSocket 測試工具

#### 使用 wscat 測試 WebSocket

```bash
# 安裝 wscat
npm install -g wscat

# 連線測試
wscat -c "ws://localhost:8080/ws/chat?token=YOUR_TOKEN"

# 發送訊息
> {"type":"SEND_MESSAGE","data":{"recipientId":"...","content":"Hello","clientMessageId":"..."}}
```

#### 使用 Postman 測試 WebSocket

1. 新建 WebSocket Request
2. URL: `ws://localhost:8080/ws/chat?token=YOUR_TOKEN`
3. 點擊 Connect
4. 發送 JSON 訊息

### C. MongoDB 查詢範例

#### 查詢最近的對話

```javascript
db.conversations.aggregate([
  { $match: { participants: "user-id" } },
  { $sort: { updatedAt: -1 } },
  { $limit: 20 },
  {
    $lookup: {
      from: "users",
      localField: "participants",
      foreignField: "userId",
      as: "participantDetails"
    }
  }
]);
```

#### 統計未讀訊息

```javascript
db.messages.aggregate([
  { 
    $match: { 
      recipientId: "user-id", 
      isRead: false 
    } 
  },
  { 
    $group: { 
      _id: "$conversationId", 
      count: { $sum: 1 } 
    } 
  }
]);
```

### D. 部署檢查清單

#### 上線前檢查

- [ ] WebSocket 連線測試通過
- [ ] 訊息發送/接收正常
- [ ] 未讀數量計算正確
- [ ] 離線訊息推送正常
- [ ] Redis Pub/Sub 配置正確
- [ ] MongoDB 索引已建立
- [ ] 效能測試達標
- [ ] 安全掃描通過
- [ ] 監控告警已配置
- [ ] 日誌收集已配置

---

**下一步行動：**
1. 審核並確認此文件規格
2. 根據需求調整文件內容
3. 完成「登入功能文件」剩餘部分
4. 開始程式碼實作 搜尋欄
   **元素：**
- 搜尋圖示（🔍）
- 輸入框：「搜尋對話...」
- 即時過濾功能

#### 3. 對話列表項目
**每個項目包含：**
- 頭像（48px，縮寫 + 線上狀態點）
- 對話資訊：
  - 上半部：
    - 好友帳號名稱（15px，粗體，左側）
    - 時間戳記（12px，灰色，右側）
      - 格式：「10:30」/ 「昨天」/ 「週二」
  - 下半部：
    - 最後訊息預覽（14px，灰色，單行省略）
- 未讀徽章（紅色圓形，顯示數字）

**狀態變化：**
- Hover 效果：淺灰背景
- 選中效果：灰色背景
- 未讀對話：訊息預覽加粗顯示

### 1.3 右側欄 - 聊天視窗

#### 1. 聊天標題列
**元素：**
- ⬅️ 返回按鈕（僅手機版）
- 對方頭像（40px）+ 線上狀態指示器
- 對方資訊：
  - 帳號名稱（16px，粗體）
  - 狀態文字（13px，灰色）
    - 線上：「線上」
    - 離線：「離線」
    - 正在輸入：「正在輸入...」
- 操作按鈕：
  - 📞 語音通話
  - 📹 視訊通話
  - ⋮ 更多選項

#### 2. 訊息區域（中間主要區域）

**日期分隔線：**
- 居中顯示
- 白色圓角卡片：「今天」/ 「昨天」/ 「2025/09/28」
- 陰影效果

**訊息氣泡設計：**

**自己發送的訊息（右側）：**
- 背景色：#1a1a1a（黑色）
- 文字色：白色
- 對齊：靠右
- 圓角：12px，右下角 4px
- 最大寬度：60%（桌面）/ 75%（手機）
- 下方顯示時間（11px，灰色）

**對方發送的訊息（左側）：**
- 背景色：#ffffff（白色）
- 文字色：#1a1a1a（黑色）
- 對齊：靠左
- 圓角：12px，左下角 4px
- 陰影：subtle shadow
- 最大寬度：60%（桌面）/ 75%（手機）
- 下方顯示時間（11px，灰色）

**正在輸入指示器：**
- 白色氣泡（左側）
- 三個跳動的圓點動畫
- 灰色圓點（8px）

**訊息間距：**
- 同一人連續訊息：4px
- 不同人訊息：16px

#### 3. 輸入區域（底部）

**佈局：**
```
[表情] [附件] [          輸入框          ] [發送]
  😊      📎     輸入訊息...                ➤
```

**表情按鈕：**
- 圖示：😊
- 點擊展開表情符號選擇器（未來功能）

**附件按鈕：**
- 圖示：📎
- 點擊上傳檔案（未來功能）

**輸入框：**
- 淺灰色背景（#f3f4f6）
- 圓角：12px
- 自動調整高度（1-3 行）
- Placeholder：「輸入訊息...」
- 支援多行文字（Shift + Enter）

**發送按鈕：**
- 黑色背景
- 白色箭頭圖示（➤）
- 圓角：8px
- 空白時禁用（灰色）
- 有內容時啟用（黑色）
- Hover 效果：上浮 + 陰影

**互動行為：**
- Enter 鍵發送訊息
- Shift + Enter 換行
- 發送後清空輸入框
- 自動滾動到最新訊息

### 1.4 空白狀態（未選中對話時）

**元素：**
- 大圖示：💬（64px）
- 標題：「選擇一個對話開始聊天」
- 說明：「從左側選擇好友開始傳送訊息」

---

## 2. 後端 API 文件 - 聊天功能

### 2.1 需求說明

**功能需求：**
1. 建立 WebSocket 連線進行即時通訊
2. 發送文字訊息
3. 接收即時訊息
4. 查詢歷史訊息（分頁）
5. 查詢對話列表
6. 標記訊息為已讀
7. 查詢未讀訊息數量
8. 發送「正在輸入」狀態

**安全需求：**
- WebSocket 連線需要 JWT Token 驗證
- 只能查看與好友的對話
- 訊息只能發送給好友
- 防止訊息內容 XSS 攻擊

### 2.2 REST API 端點列表

#### 2.2.1 查詢對話列表

**端點：** `GET /api/v1/conversations`

**請求標頭：**
```
Authorization: Bearer {access-token}
```

**查詢參數：**

| 參數名稱 | 類型 | 必填 | 說明 | 預設值 |
|---------|------|------|------|-------|
| page | Integer | 否 | 頁碼（從 0 開始） | 0 |
| size | Integer | 否 | 每頁筆數 | 20 |

**成功回應 (200 OK)：**
```json
{
  "success": true,
  "data": {
    "conversations": [
      {
        "conversationId": "uuid-string",
        "participant": {
          "userId": "uuid-string",
          "username": "alice_wang"
        },
        "lastMessage": {
          "messageId": "uuid-string",
          "content": "好的，明天見！",
          "senderId": "uuid-string",
          "timestamp": "2025-09-30T10:30:00Z"
        },
        "unreadCount": 2,
        "updatedAt": "2025-09-30T10:30:00Z"
      },
      {
        "conversationId": "uuid-string-2",
        "participant": {
          "userId": "uuid-string-2",
          "username": "bob_chen"
        },
        "lastMessage": {
          "messageId": "uuid-string-3",
          "content": "收到，謝謝！",
          "senderId": "uuid-string-2",
          "timestamp": "2025-09-29T15:35:00Z"
        },
        "unreadCount": 0,
        "updatedAt": "2025-09-29T15:35:00Z"
      }
    ],
    "pagination": {
      "currentPage": 0,
      "totalPages": 1,
      "totalItems": 2,
      "pageSize": 20
    }
  }
}
```

#### 2.2.2 查詢歷史訊息

**端點：** `GET /api/v1/conversations/{conversationId}/messages`

**請求標頭：**
```
Authorization: Bearer {access-token}
```

**路徑參數：**

| 參數名稱 | 類型 | 說明 |
|---------|------|------|
| conversationId | UUID | 對話 ID |

**查詢參數：**

| 參數名稱 | 類型 | 必填 | 說明 | 預設值 |
|---------|------|------|------|-------|
| page | Integer | 否 | 頁碼（從 0 開始） | 0 |
| size | Integer | 否 | 每頁筆數 | 50 |
| before | String | 否 | 查詢此訊息 ID 之前的訊息 | - |

**成功回應 (200 OK)：**
```json
{
  "success": true,
  "data": {
    "messages": [
      {
        "messageId": "uuid-string",
        "conversationId": "uuid-string",
        "senderId": "uuid-string",
        "senderUsername": "alice_wang",
        "content": "嗨！你好嗎？",
        "timestamp": "2025-09-30T10:25:00Z",
        "isRead": true
      },
      {
        "messageId": "uuid-string-2",
        "conversationId": "uuid-string",
        "senderId": "current-user-id",
        "senderUsername": "john_doe",
        "content": "我很好，謝謝！你呢？",
        "timestamp": "2025-09-30T10:26:00Z",
        "isRead": true
      },
      {
        "messageId": "uuid-string-3",
        "conversationId": "uuid-string",
        "senderId": "uuid-string",
        "senderUsername": "alice_wang",
        "content": "我也很好！明天要一起吃午餐嗎？",
        "timestamp": "2025-09-30T10:28:00Z",
        "isRead": true
      }
    ],
    "pagination": {
      "currentPage": 0,
      "totalPages": 1,
      "totalItems": 3,
      "pageSize": 50,
      "hasMore": false
    }
  }
}
```

#### 2.2.3 標記訊息為已讀

**端點：** `PUT /api/v1/conversations/{conversationId}/read`

**請求標頭：**
```
Authorization: Bearer {access-token}
```

**路徑參數：**

| 參數名稱 | 類型 | 說明 |
|---------|------|------|
| conversationId | UUID | 對話 ID |

**請求主體：**
```json
{
  "lastReadMessageId": "uuid-string"
}
```

**成功回應 (200 OK)：**
```json
{
  "success": true,
  "message": "訊息已標記為已讀"
}
```

#### 2.2.4 查詢未讀訊息總數

**端點：** `GET /api/v1/conversations/unread-count`

**請求標頭：**
```
Authorization: Bearer {access-token}
```

**成功回應 (200 OK)：**
```json
{
  "success": true,
  "data": {
    "totalUnreadCount": 5,
    "conversationUnreadCounts": {
      "uuid-conversation-1": 2,
      "uuid-conversation-2": 3
    }
  }
}
```

---

## 3. WebSocket 通訊協定

### 3.1 連線建立

**WebSocket URL：** `ws://localhost:8080/ws/chat?token={access-token}`

**連線流程：**
1. 前端使用 JWT Token 建立 WebSocket 連線
2. 後端驗證 Token
3. 驗證成功後建立連線，將使用者加入線上列表
4. 驗證失敗則拒絕連線（4401 Unauthorized）

**連線成功回應：**
```json
{
  "type": "CONNECTION_ESTABLISHED",
  "data": {
    "userId": "uuid-string",
    "timestamp": "2025-09-30T10:00:00Z"
  }
}
```

### 3.2 訊息類型定義

#### 1. 發送文字訊息 (CLIENT → SERVER)

**訊息類型：** `SEND_MESSAGE`

**格式：**
```json
{
  "type": "SEND_MESSAGE",
  "data": {
    "recipientId": "uuid-string",
    "content": "訊息內容",
    "clientMessageId": "client-generated-uuid"
  }
}
```

**欄位說明：**

| 欄位名稱 | 類型 | 必填 | 說明 |
|---------|------|------|------|
| recipientId | UUID | 是 | 接收者使用者 ID |
| content | String | 是 | 訊息內容（1-2000 字元） |
| clientMessageId | UUID | 是 | 前端生成的訊息 ID（用於去重） |

#### 2. 接收文字訊息 (SERVER → CLIENT)

**訊息類型：** `NEW_MESSAGE`

**格式：**
```json
{
  "type": "NEW_MESSAGE",
  "data": {
    "messageId": "uuid-string",
    "conversationId": "uuid-string",
    "senderId": "uuid-string",
    "senderUsername": "alice_wang",
    "content": "訊息內容",
    "timestamp": "2025-09-30T10:30:00Z",
    "clientMessageId": "client-generated-uuid"
  }
}
```

#### 3. 訊息發送確認 (SERVER → CLIENT)

**訊息類型：** `MESSAGE_DELIVERED`

**格式：**
```json
{
  "type": "MESSAGE_DELIVERED",
  "data": {
    "messageId": "uuid-string",
    "clientMessageId": "client-generated-uuid",
    "timestamp": "2025-09-30T10:30:00Z"
  }
}
```

#### 4. 發送「正在輸入」狀態 (CLIENT → SERVER)

**訊息類型：** `TYPING_START` / `TYPING_STOP`

**格式：**
```json
{
  "type": "TYPING_START",
  "data": {
    "recipientId": "uuid-string"
  }
}
```

#### 5. 接收「正在輸入」狀態 (SERVER → CLIENT)

**訊息類型：** `USER_TYPING`

**格式：**
```json
{
  "type": "USER_TYPING",
  "data": {
    "userId": "uuid-string",
    "username": "alice_wang",
    "isTyping": true
  }
}
```

#### 6. 訊息已讀回執 (CLIENT → SERVER)

**訊息類型：** `MESSAGE_READ`

**格式：**
```json
{
  "type": "MESSAGE_READ",
  "data": {
    "conversationId": "uuid-string",
    "messageId": "uuid-string"
  }
}
```

#### 7. 接收已讀回執 (SERVER → CLIENT)

**訊息類型：** `MESSAGE_READ_RECEIPT`

**格式：**
```json
{
  "type": "MESSAGE_READ_RECEIPT",
  "data": {
    "conversationId": "uuid-string",
    "messageId": "uuid-string",
    "readBy": "uuid-string",
    "readAt": "2025-09-30T10:35:00Z"
  }
}
```

#### 8. 錯誤訊息 (SERVER → CLIENT)

**訊息類型：** `ERROR`

**格式：**
```json
{
  "type": "ERROR",
  "data": {
    "code": "INVALID_RECIPIENT",
    "message": "接收者不是好友或不存在",
    "clientMessageId": "client-generated-uuid"
  }
}
```

**常見錯誤代碼：**
- `INVALID_RECIPIENT`: 接收者無效
- `NOT_FRIENDS`: 不是好友關係
- `MESSAGE_TOO_LONG`: 訊息過長
- `RATE_LIMIT_EXCEEDED`: 發送頻率過高

### 3.3 心跳機制

**目的：** 保持連線活躍，檢測斷線

**Ping (CLIENT → SERVER)：**
```json
{
  "type": "PING"
}
```

**Pong (SERVER → CLIENT)：**
```json
{
  "type": "PONG",
  "data": {
    "timestamp": "2025-09-30T10:30:00Z"
  }
}
```

**頻率：** 每 30 秒發送一次 Ping

### 3.4 連線斷開

**主動斷開 (CLIENT → SERVER)：**
```json
{
  "type": "DISCONNECT"
}
```

**斷開確認 (SERVER → CLIENT)：**
```json
{
  "type": "DISCONNECTED",
  "data": {
    "reason": "CLIENT_REQUEST",
    "timestamp": "2025-09-30T11:00:00Z"
  }
}
```

**被動斷開原因：**
- `CLIENT_REQUEST`: 使用者主動斷開
- `TOKEN_EXPIRED`: Token 過期
- `TIMEOUT`: 連線逾時
- `SERVER_ERROR`: 伺服器錯誤

---

## 4. 系統架構圖 - 聊天系統

### 4.1 即時訊息發送流程

```
┌─────────┐         ┌─────────┐         ┌──────────────┐         ┌─────────┐         ┌─────────┐
│ User A  │         │  React  │         │ Spring Boot  │         │ MongoDB │         │  Redis  │
│(發送者) │         │Frontend │         │  WebSocket   │         │         │         │         │
└────┬────┘         └────┬────┘         └──────┬───────┘         └────┬────┘         └────┬────┘
     │                   │                      │                      │                   │
     │ 1. 輸入訊息內容   │                      │                      │                   │
     ├──────────────────>│                      │                      │                   │
     │                   │                      │                      │                   │
     │                   │ 2. 點擊發送          │                      │                   │
     │                   │    生成 clientMsgId  │                      │                   │
     │                   │                      │                      │                   │
     │                   │ 3. WebSocket SEND_MESSAGE                   │                   │
     │                   ├─────────────────────>│                      │                   │
     │                   │  {recipientId,       │                      │                   │
     │                   │   content,           │                      │                   │
     │                   │   clientMessageId}   │                      │                   │
     │                   │                      │                      │                   │
     │                   │                      │ 4. 驗證好友關係      │                   │
     │                   │                      │    檢查內容長度      │                   │
     │                   │                      │                      │                   │
     │                   │                      │ 5. 儲存訊息到 MongoDB│                   │
     │                   │                      ├─────────────────────>│                   │
     │                   │                      │  INSERT message      │                   │
     │                   │                      │  UPDATE conversation │                   │
     │                   │                      │                      │                   │
     │                   │                      │ 6. 快取最新訊息      │                   │
     │                   │                      ├──────────────────────────────────────────>│
     │                   │                      │  ZADD conversation:  │                   │
     │                   │                      │  {conversationId}    │                   │
     │                   │                      │                      │                   │
     │                   │ 7. MESSAGE_DELIVERED │                      │                   │
     │                   │<─────────────────────┤                      │                   │
     │                   │  {messageId}         │                      │                   │
     │                   │                      │                      │                   │
     │ 8. 顯示已發送     │                      │                      │                   │
     │<──────────────────┤                      │                      │                   │
     │                   │                      │                      │                   │
     
     
┌─────────┐         ┌─────────┐         ┌──────────────┐
│ User B  │         │  React  │         │ Spring Boot  │
│(接收者) │         │Frontend │         │  WebSocket   │
└────┬────┘         └────┬────┘         └──────┬───────┘
     │                   │                      │
     │                   │ 9. WebSocket NEW_MESSAGE                    
     │                   │<─────────────────────┤
     │                   │  {messageId,         │
     │                   │   senderId,          │
     │                   │   content,           │
     │                   │   timestamp}         │
     │                   │                      │
     │ 10. 顯示新訊息    │                      │
     │     播放提示音    │                      │
     │<──────────────────┤                      │
     │                   │                      │
     │ 11. 使用者閱讀    │                      │
     │     訊息          │                      │
     ├──────────────────>│                      │
     │                   │                      │
     │                   │ 12. WebSocket MESSAGE_READ                  
     │                   ├─────────────────────>│
     │                   │  {conversationId,    │
     │                   │   messageId}         │
     │                   │                      │
```

### 4.2 WebSocket 連線管理架構

```
┌──────────────────────────────────────────────────────┐
│              Kubernetes Cluster                      │
│                                                      │
│  ┌────────────────────────────────────────────────┐ │
│  │         Spring Boot Backend (Pods 1-5)        │ │
│  │                                                │ │
│  │  ┌──────────────────────────────────────────┐ │ │
│  │  │      WebSocket Session Manager           │ │ │
│  │  │                                          │ │ │
│  │  │  User1 → WebSocket Session 1            │ │ │
│  │  │  User2 → WebSocket Session 2            │ │ │
│  │  │  User3 → WebSocket Session 3            │ │ │
│  │  │  ...                                     │ │ │
│  │  └──────────────────────────────────────────┘ │ │
│  │                      ↕                         │ │
│  │  ┌──────────────────────────────────────────┐ │ │
│  │  │         Redis Pub/Sub                    │ │ │
│  │  │  (跨 Pod 訊息廣播)                       │ │ │
│  │  │                                          │ │ │
│  │  │  Channel: user:{userId}                 │ │ │
│  │  │  Channel: conversation:{conversationId} │ │ │
│  │  └──────────────────────────────────────────┘ │ │
│  └────────────────────────────────────────────────┘ │
│                         ↕                            │
│  ┌────────────────────────────────────────────────┐ │
│  │              MongoDB                           │ │
│  │  Collection: messages                          │ │
│  │  Collection: conversations                     │ │
│  └────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────┘
```

---

## 5. 資料庫架構 (MongoDB)

### 5.1 Collection 結構

#### 5.1.1 messages Collection（訊息）

**Collection 名稱：** `messages`

**文件結構：**
```javascript
{
  "_id": ObjectId("..."),
  "messageId": "uuid-string",           // 訊息唯一識別碼
  "conversationId": "uuid-string",      // 對話 ID
  "senderId": "uuid-string",            // 發送者 ID
  "senderUsername": "alice_wang",       // 發送者帳號
  "recipientId": "uuid-string",         // 接收者 ID
  "content": "訊息內容",                 // 訊息文字
  "timestamp": ISODate("2025-09-30T10:30:00Z"),  // 發送時間
  "isRead": false,                      // 是否已讀
  "readAt": null,                       // 讀取時間
  "clientMessageId": "client-uuid",     // 前端生成 ID（去重用）
  "createdAt": ISODate("2025-09-30T10:30:00Z"),
  "updatedAt": ISODate("2025-09-30T10:30:00Z")
}
```

**索引設計：**
```javascript
// 複合索引：查詢對話的訊息（按時間排序）
db.messages.createIndex({ 
  "conversationId": 1, 
  "timestamp": -1 
});

// 索引：查詢未讀訊息
db.messages.createIndex({ 
  "recipientId": 1, 
  "isRead": 1 
});

// 唯一索引：防止重複訊息
db.messages.createIndex({ 
  "clientMessageId": 1 
}, { unique: true });

// TTL 索引：自動刪除舊訊息（可選，例如 90 天）
db.messages.createIndex({ 
  "createdAt": 1 
}, { expireAfterSeconds: 7776000 });
```

#### 5.1.2 conversations Collection（對話）

**Collection 名稱：** `conversations`

**文件結構：**
```javascript
{
  "_id": ObjectId("..."),
  "conversationId": "uuid-string",      // 對話唯一識別碼
  "participants": [                     // 參與者陣列（固定兩人）
    "uuid-user-1",
    "uuid-user-2"
  ],
  "lastMessage": {                      // 最後一則訊息
    "messageId": "uuid-string",
    "content": "訊息內容",
    "senderId": "uuid-string",
    "timestamp": ISODate("2025-09-30T10:30:00Z")
  },
  "unreadCounts": {                     // 各參與者的未讀數
    "uuid-user-1": 0,
    "uuid-user-2": 2
  },
  "createdAt": ISODate("2025-09-25T14:20:00Z"),
  "updatedAt": ISODate("2025-09-30T10:30:00Z")
}
```

**索引設計：**
```javascript
// 唯一索引：保證兩個使用者只有一個對話
db.conversations.createIndex({ 
  "participants": 1 
}, { unique: true });

// 索引：查詢使用者的所有對話（按更新時間排序）
db.conversations.createIndex({ 
  "participants": 1, 
  "updatedAt": -1 
});

// 索引：conversationId 查詢
db.conversations.createIndex({ 
  "conversationId": 1 
}, { unique: true });
```

### 5.2 查詢範例

#### 查詢對話的訊息（分頁）
```javascript
db.messages.find({
  conversationId: "uuid-conversation-id"
}).sort({
  timestamp: -1
}).skip(0).limit(50);
```

#### 查詢未讀訊息數量
```javascript
db.messages.countDocuments({
  recipientId: "uuid-user-id",
  isRead: false
});
```

#### 標記訊息為已讀
```javascript
db.messages.updateMany(
  {
    conversationId: "uuid-conversation-id",
    recipientId: "uuid-user-id",
    isRead: false
  },
  {
    $set: {
      isRead: true,
      readAt: new Date()
    }
  }
);
```

#### 查詢使用者的所有對話
```javascript
db.conversations.find({
  participants: "uuid-user-id"
}).sort({
  updatedAt: -1
});
```

---

## 6. 訊息快取策略 (Redis)

### 6.1 快取資料結構

#### 1. 對話最新訊息快取（Sorted Set）

**Key 格式：** `conversation:messages:{conversationId}`  
**資料結構：** Sorted Set（Score 為時間戳記）  
**TTL：** 7 天

**用途：** 快取最近 100 則訊息，減少 MongoDB 查詢

**Redis 指令：**
```redis
# 新增訊息到快取
ZADD conversation:messages:{conversationId} {timestamp} "{messageJson}"

# 取得最新 50 則訊息
ZREVRANGE conversation:messages:{conversationId} 0 49

# 限制快取大小（只保留最新 100 則）
ZREMRANGEBYRANK conversation:messages:{conversationId} 0 -101

# 設定過期時間
EXPIRE conversation:messages:{conversationId} 604800
```

#### 2. 未讀訊息計數（Hash）

**Key 格式：** `user:unread:{userId}`  
**資料結構：** Hash（Field 為 conversationId，Value 為未讀數）  
**TTL：** 無（持久化）

**Redis 指令：**
```redis
# 增加未讀數
HINCRBY user:unread:{userId} {conversationId} 1

# 取得某對話的未讀數
HGET user:unread:{userId} {conversationId}

# 取得所有未讀數
HGETALL user:unread:{userId}

# 清除某對話的未讀數
HDEL user:unread:{userId} {conversationId}
```

#### 3. WebSocket Session 映射（Hash）

**Key 格式：** `ws:sessions:{userId}`  
**資料結構：** Hash（存儲 session 資訊）  
**TTL：** 1 小時（隨心跳更新）

**Value 結構：**
```json
{
  "sessionId": "session-uuid",
  "connectedAt": "2025-09-30T10:00:00Z",
  "lastHeartbeat": "2025-09-30T10:30:00Z",
  "podId": "backend-pod-3"
}
```

#### 4. 正在輸入狀態（String）

**Key 格式：** `typing:{conversationId}:{userId}`  
**資料結構：** String（存儲輸入狀態）  
**TTL：** 5 秒（自動過期）

**Redis 指令：**
```redis
# 設定正在輸入
SET typing:{conversationId}:{userId} "1" EX 5

# 檢查是否正在輸入
EXISTS typing:{conversationId}:{userId}

# 停止輸入
DEL typing:{conversationId}:{userId}
```

### 6.2 Redis Pub/Sub 頻道

#### 用途：跨 Pod 訊息廣播

**頻道命名：**
- `user:{userId}` - 使用者專屬頻道（接收所有訊息）
- `conversation:{conversationId}` - 對話頻道

**發布訊息範例：**
```redis
PUBLISH user:{recipientId} '{"type":"NEW_MESSAGE","data":{...}}'
```

**訂閱範例（Spring Boot）：**
```java
@Service
public class RedisMessageSubscriber {
    
    @Autowired
    private WebSocketSessionManager sessionManager;
    
    public void subscribeToUserChannel(UUID userId) {
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.subscribe(
                new MessageListener() {
                    public void onMessage(Message message, byte[] pattern) {
                        String payload = new String(message.getBody());
                        sessionManager.sendToUser(userId, payload);
                    }
                },
                ("user:" + userId).getBytes()
            );
            return null;
        });
    }
}
```

---

## 7. 後端實作架構 (Spring Boot)

### 7.1 專案結構

```
src/main/java/com/example/chatsystem/
├── config/
│   ├── WebSocketConfig.java            # WebSocket 配置
│   ├── RedisConfig.java                # Redis Pub/Sub 配置
│   └── MongoConfig.java                # MongoDB 配置
├── websocket/
│   ├── ChatWebSocketHandler.java       # WebSocket 處理器
│   ├── WebSocketSessionManager.java    # Session 管理器
│   └── WebSocketAuthInterceptor.java   # WebSocket 認證攔截器
├── controller/
│   ├── ConversationController.java     # 對話相關 REST API
│   └── MessageController.java          # 訊息相關 REST API
├── service/
│   ├── ChatService.java                # 聊天服務接口
│   ├── ConversationService.java        # 對話服務接口
│   ├── MessageService.java             # 訊息服務接口
│   └── impl/
│       ├── ChatServiceImpl.java
│       ├── ConversationServiceImpl.java
│       └── MessageServiceImpl.java
├── repository/
│   ├── MessageRepository.java          # MongoDB Message Repository
│   └── ConversationRepository.java     # MongoDB Conversation Repository
├── document/
│   ├── Message.java                    # MongoDB Message 文件
│   └── Conversation.java               # MongoDB Conversation 文件
├── dto/
│   ├── WebSocketMessage.java           # WebSocket 訊息 DTO
│   ├── SendMessageDTO.java             # 發送訊息 DTO
│   ├── ConversationDTO.java            # 對話 DTO
│   └── MessageDTO.java                 # 訊息 DTO
├── enums/
│   └── WebSocketMessageType.java       # WebSocket 訊息類型枚舉
└── util/
    ├── MessageValidator.java           # 訊息驗證工具
    └── ConversationIdGenerator.java    # 對話 ID 生成器
```

### 7.2 核心類別實作

#### WebSocketConfig.java
```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;
    
    @Autowired
    private WebSocketAuthInterceptor authInterceptor;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(authInterceptor)
                .setAllowedOrigins("*");
    }
}
```

#### ChatWebSocketHandler.java
```java
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    
    @Autowired
    private ChatService chatService;
    
    @Autowired
    private WebSocketSessionManager sessionManager;
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        UUID userId = getUserIdFromSession(session);
        sessionManager.addSession(userId, session);
        
        // 發送連線成功訊息
        sendMessage(session, new WebSocketMessage(
            WebSocketMessageType.CONNECTION_ESTABLISHED,
            Map.of("userId", userId, "timestamp", Instant.now())
        ));
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        UUID userId = getUserIdFromSession(session);
        WebSocketMessage wsMessage = parseMessage(message.getPayload());
        
        switch (wsMessage.getType()) {
            case SEND_MESSAGE:
                chatService.handleSendMessage(userId, wsMessage.getData());
                break;
            case TYPING_START:
                chatService.handleTypingStart(userId, wsMessage.getData());
                break;
            case TYPING_STOP:
                chatService.handleTypingStop(userId, wsMessage.getData());
                break;
            case MESSAGE_READ:
                chatService.handleMessageRead(userId, wsMessage.getData());
                break;
            case PING:
                sendMessage(session, new WebSocketMessage(
                    WebSocketMessageType.PONG,
                    Map.of("timestamp", Instant.now())
                ));
                break;
            default:
                sendError(session, "UNKNOWN_MESSAGE_TYPE", "未知的訊息類型");
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID userId = getUserIdFromSession(session);
        sessionManager.removeSession(userId, session);
    }
}
```

#### WebSocketSessionManager.java
```java
@Service
public class WebSocketSessionManager {
    
    private final ConcurrentHashMap<UUID, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    public void addSession(UUID userId, WebSocketSession session) {
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                    .add(session);
        
        // 記錄到 Redis
        saveSessionToRedis(userId, session.getId());
    }
    
    public void removeSession(UUID userId, WebSocketSession session) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
            }
        }
        
        // 從 Redis 移除
        removeSessionFromRedis(userId);
    }
    
    public void sendToUser(UUID userId, String message) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.forEach(session -> {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    log.error("Failed to send message to user: {}", userId, e);
                }
            });
        } else {
            // 使用者不在本 Pod，通過 Redis Pub/Sub 廣播
            publishToRedis(userId, message);
        }
    }
    
    private void publishToRedis(UUID userId, String message) {
        redisTemplate.convertAndSend("user:" + userId, message);
    }
}
```

#### ChatServiceImpl.java
```java
@Service
public class ChatServiceImpl implements ChatService {
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    @Autowired
    private FriendshipRepository friendshipRepository;
    
    @Autowired
    private WebSocketSessionManager sessionManager;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Override
    @Transactional
    public void handleSendMessage(UUID senderId, Map<String, Object> data) {
        UUID recipientId = UUID.fromString((String) data.get("recipientId"));
        String content = (String) data.get("content");
        String clientMessageId = (String) data.get("clientMessageId");
        
        // 1. 驗證是否為好友
        if (!friendshipRepository.existsByUserIdAndFriendId(senderId, recipientId)) {
            throw new NotFriendsException("不是好友關係");
        }
        
        // 2. 驗證訊息內容
        if (content == null || content.trim().isEmpty() || content.length() > 2000) {
            throw new InvalidMessageException("訊息內容無效");
        }
        
        // 3. 生成或取得對話 ID
        String conversationId = getOrCreateConversation(senderId, recipientId);
        
        // 4. 建立訊息
        Message message = Message.builder()
                .messageId(UUID.randomUUID().toString())
                .conversationId(conversationId)
                .senderId(senderId.toString())
                .recipientId(recipientId.toString())
                .content(content.trim())
                .clientMessageId(clientMessageId)
                .timestamp(Instant.now())
                .isRead(false)
                .build();
        
        // 5. 儲存到 MongoDB
        messageRepository.save(message);
        
        // 6. 更新對話
        updateConversation(conversationId, message);
        
        // 7. 快取到 Redis
        cacheMessage(conversationId, message);
        
        // 8. 增加未讀數
        incrementUnreadCount(recipientId, conversationId);
        
        // 9. 發送確認給發送者
        WebSocketMessage deliveredMsg = new WebSocketMessage(
            WebSocketMessageType.MESSAGE_DELIVERED,
            Map.of(
                "messageId", message.getMessageId(),
                "clientMessageId", clientMessageId,
                "timestamp", message.getTimestamp()
            )
        );
        sessionManager.sendToUser(senderId, toJson(deliveredMsg));
        
        // 10. 推送訊息給接收者
        WebSocketMessage newMsg = new WebSocketMessage(
            WebSocketMessageType.NEW_MESSAGE,
            Map.of(
                "messageId", message.getMessageId(),
                "conversationId", conversationId,
                "senderId", senderId.toString(),
                "content", content,
                "timestamp", message.getTimestamp(),
                "clientMessageId", clientMessageId
            )
        );
        sessionManager.sendToUser(recipientId, toJson(newMsg));
    }
    
    private String getOrCreateConversation(UUID user1, UUID user2) {
        // 使用固定規則生成 conversationId（確保雙向一致）
        List<UUID> participants = Arrays.asList(user1, user2);
        Collections.sort(participants);
        return ConversationIdGenerator.generate(participants);
    }
    
    private void cacheMessage(String conversationId, Message message) {
        String key = "conversation:messages:" + conversationId;
        redisTemplate.opsForZSet().add(
            key, 
            toJson(message), 
            message.getTimestamp().toEpochMilli()
        );
        
        // 只保留最新 100 則
        redisTemplate.opsForZSet().removeRange(key, 0, -101);
        
        // 設定過期時間 7 天
        redisTemplate.expire(key, 7, TimeUnit.DAYS);
    }
    
    private void incrementUnreadCount(UUID userId, String conversationId) {
        String key = "user:unread:" + userId;
        redisTemplate.opsForHash().increment(key, conversationId, 1);
    }
}
```

### 7.3 MongoDB Repository

```java
@Repository
public interface MessageRepository extends MongoRepository<Message, String> {
    
    // 查詢對話的訊息（分頁）
    Page<Message> findByConversationIdOrderByTimestampDesc(
        String conversationId, 
        Pageable pageable
    );
    
    // 查詢未讀訊息數量
    long countByRecipientIdAndIsRead(String recipientId, boolean isRead);
    
    // 標記訊息為已讀
    @Query("{ 'conversationId': ?0, 'recipientId': ?1, 'isRead': false }")
    @Update("{ '$set': { 'isRead': true, 'readAt': ?2 } }")
    void markAsRead(String conversationId, String recipientId, Instant readAt);
}

@Repository
public interface ConversationRepository extends MongoRepository<Conversation, String> {
    
    // 查詢使用者的所有對話
    @Query("{ 'participants': ?0 }")
    Page<Conversation> findByParticipant(
        String userId, 
        Pageable pageable
    );
    
    // 根據參與者查詢對話
    Optional<Conversation> findByParticipantsContainingAll(List<String> participants);
}
```

---

## 8. 前端實作架構 (React)

### 8.1 專案結構

```
src/
├── components/
│   ├── chat/
│   │   ├── ChatWindow.jsx              # 聊天視窗組件
│   │   ├── MessageList.jsx             # 訊息列表組件
│   │   ├── MessageBubble.jsx           # 訊息氣泡組件
│   │   ├── MessageInput.jsx            # 訊息輸入組件
│   │   ├── TypingIndicator.jsx         # 正在輸入指示器
│   │   └── ConversationList.jsx        # 對話列表組件
│   └── common/
│       ├── Avatar.jsx
│       └── TimeFormatter.jsx
├── pages/
│   └── ChatPage.jsx                    # 聊天頁面
├── services/
│   ├── chatService.js                  # 聊天 REST API 服務
│   └── websocketService.js             # WebSocket 服務
├── hooks/
│   ├── useWebSocket.js                 # WebSocket Hook
│   ├── useMessages.js                  # 訊息管理 Hook
│   ├── useConversations.js             # 對話管理 Hook
│   └── useTypingIndicator.js           # 輸入狀態 Hook
├── context/
│   └── ChatContext.jsx                 # 聊天狀態管理
└── utils/
    ├── messageFormatter.js             # 訊息格式化
    └── audioNotification.js            # 音訊通知
```

### 8.2 WebSocket Service

```javascript
// websocketService.js
class WebSocketService {
  constructor() {
    this.ws = null;
    this.listeners = {};
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
  }

  connect(token) {
    const wsUrl = `ws://localhost:8080/ws/chat?token=${token}`;
    this.ws = new WebSocket(wsUrl);

    this.ws.onopen = () => {
      console.log('WebSocket connected');
      this.reconnectAttempts = 0;
      this.startHeartbeat();
    };

    this.ws.onmessage = (event) => {
      const message = JSON.parse(event.data);
      this.handleMessage(message);
    };

    this.ws.onerror = (error) => {
      console.error('WebSocket error:', error);
    };

    this.ws.onclose = () => {
      console.log('WebSocket disconnected');
      this.stopHeartbeat();
      this.attemptReconnect(token);
    };
  }

  disconnect() {
    if (this.ws) {
      this.send({ type: 'DISCONNECT' });
      this.ws.close();
      this.ws = null;
    }
  }

  send(message) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
    }
  }

  sendMessage(recipientId, content) {
    const clientMessageId = this.generateUUID();
    this.send({
      type: 'SEND_MESSAGE',
      data: {
        recipientId,
        content,
        clientMessageId
      }
    });
    return clientMessageId;
  }

  sendTypingStart(recipientId) {
    this.send({
      type: 'TYPING_START',
      data: { recipientId }
    });
  }

  sendTypingStop(recipientId) {
    this.send({
      type: 'TYPING_STOP',
      data: { recipientId }
    });
  }

  markAsRead(conversationId, messageId) {
    this.send({
      type: 'MESSAGE_READ',
      data: { conversationId, messageId }
    });
  }

  on(eventType, callback) {
    if (!this.listeners[eventType]) {
      this.listeners[eventType] = [];
    }
    this.listeners[eventType].push(callback);
  }

  off(eventType, callback) {
    if (this.listeners[eventType]) {
      this.listeners[eventType] = this.listeners[eventType]
        .filter(cb => cb !== callback);
    }
  }

  handleMessage(message) {
    const { type, data } = message;
    
    if (this.listeners[type]) {
      this.listeners[type].forEach(callback => callback(data));
    }
  }

  startHeartbeat() {
    this.heartbeatInterval = setInterval(() => {
      this.send({ type: 'PING' });
    }, 30000); // 每 30 秒
  }

  stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
    }
  }

  attemptReconnect(token) {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);
      
      console.log(`Reconnecting in ${delay}ms... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
      
      setTimeout(() => {
        this.connect(token);
      }, delay);
    }
  }

  generateUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      const r = Math.random() * 16 | 0;
      const v = c === 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
  }
}

export default new WebSocketService();
```

### 8.3 Custom Hook - useWebSocket

```javascript
// useWebSocket.js
import { useEffect, useCallback } from 'react';
import { useAuth } from './useAuth';
import websocketService from '../services/websocketService';

export const useWebSocket = () => {
  const { accessToken } = useAuth();

  useEffect(() => {
    if (accessToken) {
      websocketService.connect(accessToken);
    }

    return () => {
      websocketService.disconnect();
    };
  }, [accessToken]);

  const sendMessage = useCallback((recipientId, content) => {
    return websocketService.sendMessage(recipientId, content);
  }, []);

  const sendTypingStart = useCallback((recipientId) => {
    websocketService.sendTypingStart(recipientId);
  }, []);

  const sendTypingStop = useCallback((recipientId) => {
    websocketService.sendTypingStop(recipientId);
  }, []);

  const markAsRead = useCallback((conversationId, messageId) => {
    websocketService.markAsRead(conversationId, messageId);
  }, []);

  const onNewMessage = useCallback((callback) => {
    websocketService.on('NEW_MESSAGE', callback);
    return () => websocketService.off('NEW_MESSAGE', callback);
  }, []);

  const onMessageDelivered = useCallback((callback) => {
    websocketService.on('MESSAGE_DELIVERED', callback);
    return () => websocketService.off('MESSAGE_DELIVERED', callback);
  }, []);

  const onUserTyping = useCallback((callback) => {
    websocketService.on('USER_TYPING', callback);
    return () => websocketService.off('USER_TYPING', callback);
  }, []);

  return {
    sendMessage,
    sendTypingStart,
    sendTypingStop,
    markAsRead,
    onNewMessage,
    onMessageDelivered,
    onUserTyping
  };
};
```

### 8.4 Custom Hook - useMessages

```javascript
// useMessages.js
import { useState, useEffect, useCallback } from 'react';
import { chatService } from '../services/chatService';
import { useWebSocket } from './useWebSocket';

export const useMessages = (conversationId) => {
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [hasMore, setHasMore] = useState(true);
  
  const { onNewMessage, onMessageDelivered, markAsRead } = useWebSocket();

  // 載入歷史訊息
  const loadMessages = useCallback(async (page = 0) => {
    try {
      setLoading(true);
      const response = await chatService.getMessages(conversationId, page, 50);
      
      if (page === 0) {
        setMessages(response.data.messages.reverse());
      } else {
        setMessages(prev => [...response.data.messages.reverse(), ...prev]);
      }
      
      setHasMore(response.data.pagination.hasMore);
    } catch (error) {
      console.error('Failed to load messages:', error);
    } finally {
      setLoading(false);
    }
  }, [conversationId]);

  // 發送訊息
  const sendMessage = useCallback((content, recipientId) => {
    const clientMessageId = useWebSocket().sendMessage(recipientId, content);
    
    // 樂觀更新
    const optimisticMessage = {
      messageId: clientMessageId,
      conversationId,
      senderId: 'current-user-id', // 從 auth context 取得
      content,
      timestamp: new Date().toISOString(),
      status: 'sending'
    };
    
    setMessages(prev => [...prev, optimisticMessage]);
    
    return clientMessageId;
  }, [conversationId]);

  // 監聽新訊息
  useEffect(() => {
    const cleanup = onNewMessage((data) => {
      if (data.conversationId === conversationId) {
        setMessages(prev => {
          // 防止重複
          if (prev.some(m => m.messageId === data.messageId)) {
            return prev;
          }
          return [...prev, data];
        });
        
        // 播放提示音
        playNotificationSound();
        
        // 標記為已讀
        markAsRead(conversationId, data.messageId);
      }
    });
    
    return cleanup;
  }, [conversationId, onNewMessage, markAsRead]);

  // 監聽訊息送達
  useEffect(() => {
    const cleanup = onMessageDelivered((data) => {
      setMessages(prev => prev.map(msg => 
        msg.messageId === data.clientMessageId
          ? { ...msg, messageId: data.messageId, status: 'delivered' }
          : msg
      ));
    });
    
    return cleanup;
  }, [onMessageDelivered]);

  // 初始載入
  useEffect(() => {
    if (conversationId) {
      loadMessages(0);
    }
  }, [conversationId, loadMessages]);

  return {
    messages,
    loading,
    hasMore,
    sendMessage,
    loadMore: () => loadMessages(Math.floor(messages.length / 50))
  };
};
```

---

## 9. 即時通訊機制

### 9.1 訊息送達保證

#### 1. 客戶端訊息去重
- 前端生成唯一的 `clientMessageId`
- MongoDB 建立唯一索引防止重複儲存
- 若訊息已存在，返回原訊息 ID

#### 2. 訊息確認機制
```
發送訊息 → 儲存 MongoDB → 返回 MESSAGE_DELIVERED → 前端更新狀態
```

#### 3. 離線訊息處理
- 訊息儲存於 MongoDB（持久化）
- 接收者上線時，主動查詢未讀訊息
- 前端顯示未讀數量徽章

### 9.2 訊息排序與同步

#### 1. 時間戳記處理
- 使用伺服器時間戳記（避免客戶端時間不準）
- MongoDB 按 `timestamp` 排序
- Redis Sorted Set 使用時間戳記作為 Score

#### 2.