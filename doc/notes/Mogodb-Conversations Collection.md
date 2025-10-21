# 為什麼需要 Conversations Collection？

## 場景：顯示對話列表

使用者打開聊天 App 時，需要看到：

```
┌────────────────────────────────────┐
│  🟢 Bob Wang           3 分鐘前    │
│  晚上要不要一起吃飯？      未讀 3   │
├────────────────────────────────────┤
│  🔴 Charlie Lee        1 小時前    │
│  好的！                未讀 0      │
├────────────────────────────────────┤
│  🟢 David Chen         昨天        │
│  謝謝你的幫忙            未讀 0     │
└────────────────────────────────────┘
```

## ❌ 方案 A：只用 messages Collection

```javascript
// 查詢使用者的所有對話
db.messages.aggregate([
  { $match: { 
      $or: [
        { senderId: "alice" },
        { recipientId: "alice" }
      ]
  }},
  { $sort: { timestamp: -1 }},
  { $group: {
      _id: "$conversationId",
      lastMessage: { $first: "$$ROOT" },
      unreadCount: { $sum: { $cond: [
        { $and: [
          { $eq: ["$recipientId", "alice"] },
          { $eq: ["$isRead", false] }
        ]},
        1,
        0
      ]}}
  }},
  { $sort: { "lastMessage.timestamp": -1 }}
])

// 問題：
// 1. 需要掃描所有訊息（效能差）
// 2. 複雜的聚合查詢
// 3. 使用者訊息量大時極慢
```

**效能：** 10,000 則訊息 = 查詢時間約 **500ms+** 😱

---

## ✅ 方案 B：使用 conversations Collection

```javascript
// 查詢對話列表
db.conversations.find({
  participants: "uuid-alice"
}).sort({
  updatedAt: -1
}).limit(20)

// 優勢：
// 1. 直接查詢，無需聚合
// 2. 索引優化（participants + updatedAt）
// 3. 只返回 20 筆對話資料
```

**效能：** 10,000 則訊息 = 查詢時間約 **5ms** ⚡

---

## 資料更新流程

### 當 Alice 發送訊息給 Bob 時：

```java
// 1. 儲存訊息到 messages
Message msg = messageRepository.save(newMessage);

// 2. 更新 conversations（原子操作）
conversationRepository.updateConversation(
  conversationId: "alice_bob",
  lastMessage: {
    messageId: msg.getMessageId(),
    content: msg.getContent(),
    senderId: "alice",
    timestamp: now()
  },
  increment: {
    "unreadCounts.bob": 1    // Bob 的未讀數 +1
  },
  updatedAt: now()           // 更新時間（用於排序）
);
```

### 當 Bob 讀取訊息時：

```java
// 1. 更新 messages 的 isRead 狀態
messageRepository.markAsRead(conversationId, recipientId);

// 2. 重置 conversations 的未讀數
conversationRepository.resetUnreadCount(conversationId, userId: "bob");
```

---

## 總結

| 功能 | messages | conversations |
|-----|----------|---------------|
| **儲存內容** | 完整訊息歷史 | 對話元資料 |
| **查詢頻率** | 中等（點開對話時） | 極高（每次打開 App） |
| **資料量** | 大（數百萬則） | 小（等於好友數） |
| **更新頻率** | 極高（發送訊息） | 高（更新元資料） |
| **優化重點** | 複合索引、分頁 | 快速查詢、快取 |

## 設計原則

✅ **讀寫分離思維**
- `conversations`：快速讀取對話列表
- `messages`：完整訊息儲存

✅ **空間換時間**
- 冗餘 lastMessage，避免每次都查詢 messages

✅ **原子更新**
- 發送訊息時同時更新兩個 Collection
- MongoDB 的事務支援（4.0+）