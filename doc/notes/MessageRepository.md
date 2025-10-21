# MessageRepository 方法詳解

## 1. 查詢對話歷史訊息（分頁）

```java
Page<Message> findByConversationIdOrderByTimestampDesc(
    String conversationId, 
    Pageable pageable
);
```

### 使用場景
- 使用者點開與 Bob 的對話
- 需要載入最近 20 則訊息
- 向上滾動時載入更多（無限滾動）

### 實際呼叫
```java
// 第一頁（最新 20 則）
Pageable pageable = PageRequest.of(0, 20);
Page<Message> messages = messageRepository
    .findByConversationIdOrderByTimestampDesc("alice_bob", pageable);

// 第二頁（接下來 20 則）
Pageable pageable2 = PageRequest.of(1, 20);
Page<Message> olderMessages = messageRepository
    .findByConversationIdOrderByTimestampDesc("alice_bob", pageable2);
```

### SQL 等價查詢（如果是 SQL）
```sql
SELECT * FROM messages
WHERE conversation_id = 'alice_bob'
ORDER BY timestamp DESC
LIMIT 20 OFFSET 0;
```

### 效能優化
- ✅ 複合索引：`{conversationId: 1, timestamp: -1}`
- ✅ 查詢時間：< 10ms（百萬筆資料）

---

## 2. 客戶端訊息去重

```java
Optional<Message> findByClientMessageId(String clientMessageId);
```

### 使用場景
防止重複發送！

```
使用者點擊「發送」按鈕
    ↓
前端生成 clientMessageId = "client-uuid-123"
    ↓
發送 WebSocket 訊息（包含 clientMessageId）
    ↓
後端檢查：這個 ID 已經存在嗎？
    ↓
如果存在 → 忽略（避免重複儲存）
如果不存在 → 儲存訊息
```

### 實際呼叫
```java
String clientMsgId = "client-uuid-123";

// 檢查是否已經處理過
Optional<Message> existing = messageRepository
    .findByClientMessageId(clientMsgId);

if (existing.isPresent()) {
    // 重複訊息！直接返回已存在的訊息
    return existing.get();
}

// 第一次收到，儲存訊息
Message newMsg = Message.builder()
    .clientMessageId(clientMsgId)
    .content("哈囉")
    .build();
    
messageRepository.save(newMsg);
```

### 為什麼需要去重？

常見問題：

1. **網路延遲**
   ```
   使用者點擊發送 → 網路慢 → 以為沒發送 → 再點一次
   結果：同一則訊息被發送兩次！
   ```

2. **WebSocket 重連**
   ```
   發送訊息 → WebSocket 斷線 → 自動重連 → 重發訊息
   結果：訊息重複！
   ```

### 索引設計
```java
@Indexed(unique = true, sparse = true)
private String clientMessageId;
```

- `unique = true`：確保唯一性（資料庫層級）
- `sparse = true`：允許欄位為 null（舊訊息可能沒有這個欄位）

---

## 3. 查詢未讀訊息數量

```java
// 查詢使用者的所有未讀訊息數
long countByRecipientIdAndIsReadFalse(UUID recipientId);

// 查詢特定對話的未讀訊息數
long countByConversationIdAndRecipientIdAndIsReadFalse(
    String conversationId, 
    UUID recipientId
);
```

### 使用場景 A：顯示總未讀數

```java
// 使用者登入後，顯示所有未讀訊息數量
long totalUnread = messageRepository
    .countByRecipientIdAndIsReadFalse(userId);

// 前端顯示：
// 🔔 (15) ← 紅點提示
```

### 使用場景 B：對話列表的未讀數

```java
// 顯示每個對話的未讀數
long unreadFromBob = messageRepository
    .countByConversationIdAndRecipientIdAndIsReadFalse(
        "alice_bob",
        aliceId
    );

// 前端顯示：
// Bob Wang (3) ← 這個對話有 3 則未讀
```

### ⚠️ 效能問題

如果使用者有 50 個好友，需要查詢 50 次！

```java
// ❌ 糟糕的做法（N+1 問題）
for (Friend friend : friends) {
    long unread = messageRepository.count...(); // 50 次查詢！
}
```

### ✅ 解決方案：使用 Redis 快取

```java
// 方案 A：從 conversations 讀取（推薦）
Conversation conv = conversationRepository
    .findByConversationId("alice_bob");
int unread = conv.getUnreadCounts().get(aliceId.toString());

// 方案 B：Redis Hash
Map<String, String> unreadMap = redisTemplate
    .opsForHash()
    .entries("user:unread:" + userId);
```

---

## 4. 查詢特定對話的所有未讀訊息

```java
List<Message> findByConversationIdAndRecipientIdAndIsReadFalse(
    String conversationId,
    UUID recipientId
);
```

### 使用場景：標記已讀

```java
// 使用者打開與 Bob 的對話
// 1. 查詢所有未讀訊息
List<Message> unreadMessages = messageRepository
    .findByConversationIdAndRecipientIdAndIsReadFalse(
        "alice_bob",
        aliceId
    );

// 2. 批次標記為已讀
for (Message msg : unreadMessages) {
    msg.setIsRead(true);
    msg.setReadAt(Instant.now());
}
messageRepository.saveAll(unreadMessages);

// 3. 發送已讀回執給對方（WebSocket）
webSocketService.sendReadReceipt(bobId, conversationId);
```

### 優化：使用批次更新

```java
// 使用 MongoTemplate 批次更新（更快）
Query query = Query.query(
    Criteria.where("conversationId").is(conversationId)
            .and("recipientId").is(recipientId)
            .and("isRead").is(false)
);

Update update = Update.update("isRead", true)
                      .set("readAt", Instant.now());

mongoTemplate.updateMulti(query, update, Message.class);
```

---

## 總結：Repository 設計原則

| 原則 | 說明 | 範例 |
|-----|------|------|
| **命名規範** | 依 Spring Data 規則 | `findByXxxAndYyy` |
| **返回類型** | 單筆 `Optional`，多筆 `List/Page` | `Optional<Message>` |
| **分頁查詢** | 使用 `Pageable` 參數 | `Page<Message>` |
| **複雜查詢** | 使用 `@Query` 或 `MongoTemplate` | 聚合查詢 |
| **效能優化** | 索引 + 快取 | Redis + 複合索引 |

## 下一步

完成 Repository 後，我們將實作：

1. **ConversationService** - 對話業務邏輯
2. **MessageService** - 訊息業務邏輯
3. **WebSocket Handler** - 即時通訊