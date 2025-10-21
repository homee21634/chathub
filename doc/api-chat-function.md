# 聊天功能 API 文件

**版本：** 1.0  
**最後更新：** 2025-09-30

---

## 目錄

### REST API
1. [查詢對話列表](#1-查詢對話列表)
2. [查詢歷史訊息](#2-查詢歷史訊息)
3. [標記訊息為已讀](#3-標記訊息為已讀)
4. [查詢未讀訊息總數](#4-查詢未讀訊息總數)

### WebSocket API
5. [WebSocket 連線](#5-websocket-連線)
6. [發送文字訊息](#6-發送文字訊息)
7. [接收新訊息](#7-接收新訊息)
8. [訊息送達確認](#8-訊息送達確認)
9. [正在輸入狀態](#9-正在輸入狀態)
10. [訊息已讀回執](#10-訊息已讀回執)

---

## REST API

## 1. 查詢對話列表

### 基本資訊

- **端點：** `GET /api/v1/conversations`
- **描述：** 查詢使用者的所有對話列表
- **需要認證：** 是

### 請求標頭

```
Authorization: Bearer {access-token}
```

### 查詢參數 (Query Parameters)

| 參數名稱 | 類型 | 必填 | 說明 | 預設值 |
|---------|------|------|------|-------|
| page | Integer | 否 | 頁碼（從 0 開始） | 0 |
| size | Integer | 否 | 每頁筆數 | 20 |

### 請求範例

```bash
curl -X GET "http://localhost:8080/api/v1/conversations?page=0&size=20" \
  -H "Authorization: Bearer {your-token}"
```

### 成功回應 (200 OK)

```json
{
  "success": true,
  "data": {
    "conversations": [
      {
        "conversationId": "conv-550e8400-e29b-41d4-a716-446655440000",
        "participant": {
          "userId": "550e8400-e29b-41d4-a716-446655440001",
          "username": "alice_wang"
        },
        "lastMessage": {
          "messageId": "msg-550e8400-e29b-41d4-a716-446655440003",
    "readBy": "550e8400-e29b-41d4-a716-446655440001",
    "readAt": "2025-09-30T10:35:00Z"
  }
}
```

### 欄位說明

| 欄位路徑 | 類型 | 說明 |
|---------|------|------|
| type | String | 訊息類型 |
| data.conversationId | String (UUID) | 對話 ID |
| data.messageId | String (UUID) | 訊息 ID |
| data.readBy | String (UUID) | 讀取者 ID |
| data.readAt | String (ISO 8601) | 讀取時間 |

---

## 錯誤訊息

### 訊息類型：`ERROR`

### 格式 (SERVER → CLIENT)

```json
{
  "type": "ERROR",
  "data": {
    "code": "INVALID_RECIPIENT",
    "message": "接收者不是好友或不存在",
    "clientMessageId": "client-msg-550e8400-e29b-41d4-a716-446655440002"
  }
}
```

### 常見錯誤代碼

| 錯誤碼 | 說明 | 解決方式 |
|-------|------|---------|
| INVALID_RECIPIENT | 接收者無效 | 確認接收者 ID 是否正確 |
| NOT_FRIENDS | 不是好友關係 | 只能傳訊息給好友 |
| MESSAGE_TOO_LONG | 訊息過長 | 訊息不超過 2000 字元 |
| RATE_LIMIT_EXCEEDED | 發送頻率過高 | 降低發送頻率 |

---

## 心跳機制

### Ping (CLIENT → SERVER)

```json
{
  "type": "PING"
}
```

### Pong (SERVER → CLIENT)

```json
{
  "type": "PONG",
  "data": {
    "timestamp": "2025-09-30T10:30:00Z"
  }
}
```

### 實作建議

```javascript
// 每 30 秒發送一次 Ping
setInterval(() => {
  if (ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ type: 'PING' }));
  }
}, 30000);
```

---

## 完整整合範例

### JavaScript WebSocket 客戶端

```javascript
class ChatService {
  constructor() {
    this.ws = null;
    this.listeners = {};
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
  }

  // 連線
  connect(token) {
    const wsUrl = `ws://localhost:8080/ws/chat?token=${token}`;
    this.ws = new WebSocket(wsUrl);

    this.ws.onopen = () => {
      console.log('WebSocket 已連線');
      this.reconnectAttempts = 0;
      this.startHeartbeat();
    };

    this.ws.onmessage = (event) => {
      const message = JSON.parse(event.data);
      this.handleMessage(message);
    };

    this.ws.onerror = (error) => {
      console.error('WebSocket 錯誤:', error);
    };

    this.ws.onclose = () => {
      console.log('WebSocket 已斷線');
      this.stopHeartbeat();
      this.attemptReconnect(token);
    };
  }

  // 處理接收到的訊息
  handleMessage(message) {
    const { type, data } = message;
    
    if (this.listeners[type]) {
      this.listeners[type].forEach(callback => callback(data));
    }
  }

  // 發送訊息
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

  // 發送正在輸入狀態
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

  // 標記為已讀
  markAsRead(conversationId, messageId) {
    this.send({
      type: 'MESSAGE_READ',
      data: { conversationId, messageId }
    });
  }

  // 發送資料
  send(data) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(data));
    }
  }

  // 註冊事件監聽
  on(eventType, callback) {
    if (!this.listeners[eventType]) {
      this.listeners[eventType] = [];
    }
    this.listeners[eventType].push(callback);
  }

  // 移除事件監聽
  off(eventType, callback) {
    if (this.listeners[eventType]) {
      this.listeners[eventType] = this.listeners[eventType]
        .filter(cb => cb !== callback);
    }
  }

  // 心跳
  startHeartbeat() {
    this.heartbeatInterval = setInterval(() => {
      this.send({ type: 'PING' });
    }, 30000);
  }

  stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
    }
  }

  // 重新連線
  attemptReconnect(token) {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);
      
      console.log(`將在 ${delay}ms 後重新連線...`);
      
      setTimeout(() => {
        this.connect(token);
      }, delay);
    } else {
      console.error('重新連線失敗，已達最大嘗試次數');
    }
  }

  // 斷開連線
  disconnect() {
    if (this.ws) {
      this.send({ type: 'DISCONNECT' });
      this.ws.close();
      this.ws = null;
    }
  }

  // 生成 UUID
  generateUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      const r = Math.random() * 16 | 0;
      const v = c === 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
  }
}

// 使用範例
const chatService = new ChatService();

// 連線
const token = localStorage.getItem('accessToken');
chatService.connect(token);

// 監聽新訊息
chatService.on('NEW_MESSAGE', (data) => {
  console.log('收到新訊息:', data);
  // 顯示訊息、播放提示音
});

// 監聽送達確認
chatService.on('MESSAGE_DELIVERED', (data) => {
  console.log('訊息已送達:', data);
  // 更新訊息狀態
});

// 監聽正在輸入
chatService.on('USER_TYPING', (data) => {
  console.log(`${data.username} 正在輸入...`);
  // 顯示正在輸入指示器
});

// 發送訊息
const clientMsgId = chatService.sendMessage(
  'recipient-user-id',
  'Hello, World!'
);

// 標記為已讀
chatService.markAsRead('conversation-id', 'message-id');

// 斷開連線
chatService.disconnect();
```

---

## React Hook 範例

```javascript
import { useState, useEffect, useCallback } from 'react';

export const useWebSocket = (token) => {
  const [ws, setWs] = useState(null);
  const [messages, setMessages] = useState([]);
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    if (!token) return;

    const websocket = new WebSocket(`ws://localhost:8080/ws/chat?token=${token}`);

    websocket.onopen = () => {
      console.log('WebSocket 已連線');
      setConnected(true);
    };

    websocket.onmessage = (event) => {
      const message = JSON.parse(event.data);
      
      if (message.type === 'NEW_MESSAGE') {
        setMessages(prev => [...prev, message.data]);
      }
    };

    websocket.onclose = () => {
      console.log('WebSocket 已斷線');
      setConnected(false);
    };

    setWs(websocket);

    return () => {
      websocket.close();
    };
  }, [token]);

  const sendMessage = useCallback((recipientId, content) => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      const message = {
        type: 'SEND_MESSAGE',
        data: {
          recipientId,
          content,
          clientMessageId: crypto.randomUUID()
        }
      };
      ws.send(JSON.stringify(message));
    }
  }, [ws]);

  return {
    connected,
    messages,
    sendMessage
  };
};
```

---

## 業務規則

### 1. 訊息發送規則
- 只能發送給好友
- 訊息內容長度：1-2000 字元
- 頻率限制：每分鐘最多 30 則訊息
- 自動過濾 XSS 攻擊內容

### 2. 訊息儲存
- 所有訊息儲存於 MongoDB
- 最近 100 則訊息快取於 Redis
- 訊息保留期：90 天（可配置）

### 3. 線上狀態
- WebSocket 連線時為線上
- 斷線後保持線上狀態 1 小時（Redis TTL）
- 心跳間隔：30 秒

### 4. 未讀訊息
- 即時更新未讀數（Redis）
- 開啟對話時自動標記為已讀
- 未讀數同步到資料庫（定期）

---

## 測試案例

### WebSocket 連線測試

```javascript
// 測試案例 1: 正常連線
const ws = new WebSocket('ws://localhost:8080/ws/chat?token=valid-token');
// 預期: 連線成功，收到 CONNECTION_ESTABLISHED

// 測試案例 2: 無效 Token
const ws = new WebSocket('ws://localhost:8080/ws/chat?token=invalid-token');
// 預期: 連線被拒絕（4401）

// 測試案例 3: 發送訊息
ws.send(JSON.stringify({
  type: 'SEND_MESSAGE',
  data: {
    recipientId: 'valid-user-id',
    content: 'Test message',
    clientMessageId: 'client-uuid'
  }
}));
// 預期: 收到 MESSAGE_DELIVERED

// 測試案例 4: 發送給非好友
ws.send(JSON.stringify({
  type: 'SEND_MESSAGE',
  data: {
    recipientId: 'non-friend-id',
    content: 'Test',
    clientMessageId: 'client-uuid'
  }
}));
// 預期: 收到 ERROR (NOT_FRIENDS)

// 測試案例 5: 訊息過長
ws.send(JSON.stringify({
  type: 'SEND_MESSAGE',
  data: {
    recipientId: 'valid-user-id',
    content: 'x'.repeat(2001),
    clientMessageId: 'client-uuid'
  }
}));
// 預期: 收到 ERROR (MESSAGE_TOO_LONG)
```

---

## 效能考量

### 1. 訊息快取
- Redis 快取最近 100 則訊息
- 命中率目標：> 90%
- TTL：7 天

### 2. 連線數限制
- 單個 Pod：10,000 並發連線
- 使用 Redis Pub/Sub 跨 Pod 通訊

### 3. 訊息吞吐量
- 目標：1,000 訊息/秒 per Pod
- 使用訊息佇列緩衝高峰

---

## 安全性

### 1. 內容過濾
- XSS 防護：HTML 轉義
- 敏感詞過濾（可選）
- URL 驗證

### 2. 頻率限制
- 每分鐘 30 則訊息
- 超過限制返回錯誤

### 3. Token 驗證
- 每個 WebSocket 訊息都驗證 Token
- Token 過期自動斷開連線

---

## 版本歷史

| 版本 | 日期 | 變更內容 |
|-----|------|---------|
| 1.0 | 2025-09-30 | 初版發布 |446655440002",
          "content": "好的，明天見！",
          "senderId": "550e8400-e29b-41d4-a716-446655440001",
          "timestamp": "2025-09-30T10:30:00Z"
        },
        "unreadCount": 2,
        "updatedAt": "2025-09-30T10:30:00Z"
      },
      {
        "conversationId": "conv-550e8400-e29b-41d4-a716-446655440003",
        "participant": {
          "userId": "550e8400-e29b-41d4-a716-446655440004",
          "username": "bob_chen"
        },
        "lastMessage": {
          "messageId": "msg-550e8400-e29b-41d4-a716-446655440005",
          "content": "收到，謝謝！",
          "senderId": "550e8400-e29b-41d4-a716-446655440004",
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

### 成功回應欄位說明

| 欄位路徑 | 類型 | 說明 |
|---------|------|------|
| success | Boolean | 請求是否成功 |
| data.conversations[] | Array | 對話列表 |
| data.conversations[].conversationId | String (UUID) | 對話 ID |
| data.conversations[].participant.userId | String (UUID) | 對話對方 ID |
| data.conversations[].participant.username | String | 對話對方帳號 |
| data.conversations[].lastMessage.messageId | String (UUID) | 最後訊息 ID |
| data.conversations[].lastMessage.content | String | 最後訊息內容 |
| data.conversations[].lastMessage.senderId | String (UUID) | 發送者 ID |
| data.conversations[].lastMessage.timestamp | String (ISO 8601) | 訊息時間 |
| data.conversations[].unreadCount | Integer | 未讀訊息數量 |
| data.conversations[].updatedAt | String (ISO 8601) | 最後更新時間 |
| data.pagination | Object | 分頁資訊 |

### HTTP 狀態碼

| 狀態碼 | 說明 |
|-------|------|
| 200 | 查詢成功 |
| 401 | 未授權 |
| 500 | 伺服器內部錯誤 |

---

## 2. 查詢歷史訊息

### 基本資訊

- **端點：** `GET /api/v1/conversations/{conversationId}/messages`
- **描述：** 查詢指定對話的歷史訊息
- **需要認證：** 是

### 請求標頭

```
Authorization: Bearer {access-token}
```

### 路徑參數

| 參數名稱 | 類型 | 說明 |
|---------|------|------|
| conversationId | UUID | 對話 ID |

### 查詢參數 (Query Parameters)

| 參數名稱 | 類型 | 必填 | 說明 | 預設值 |
|---------|------|------|------|-------|
| page | Integer | 否 | 頁碼（從 0 開始） | 0 |
| size | Integer | 否 | 每頁筆數 | 50 |
| before | String (UUID) | 否 | 查詢此訊息 ID 之前的訊息 | - |

### 請求範例

```bash
curl -X GET "http://localhost:8080/api/v1/conversations/{conversationId}/messages?page=0&size=50" \
  -H "Authorization: Bearer {your-token}"
```

### 成功回應 (200 OK)

```json
{
  "success": true,
  "data": {
    "messages": [
      {
        "messageId": "msg-550e8400-e29b-41d4-a716-446655440001",
        "conversationId": "conv-550e8400-e29b-41d4-a716-446655440000",
        "senderId": "550e8400-e29b-41d4-a716-446655440002",
        "senderUsername": "alice_wang",
        "content": "嗨！你好嗎？",
        "timestamp": "2025-09-30T10:25:00Z",
        "isRead": true
      },
      {
        "messageId": "msg-550e8400-e29b-41d4-a716-446655440003",
        "conversationId": "conv-550e8400-e29b-41d4-a716-446655440000",
        "senderId": "current-user-id",
        "senderUsername": "john_doe",
        "content": "我很好，謝謝！你呢？",
        "timestamp": "2025-09-30T10:26:00Z",
        "isRead": true
      },
      {
        "messageId": "msg-550e8400-e29b-41d4-a716-446655440004",
        "conversationId": "conv-550e8400-e29b-41d4-a716-446655440000",
        "senderId": "550e8400-e29b-41d4-a716-446655440002",
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

### 成功回應欄位說明

| 欄位路徑 | 類型 | 說明 |
|---------|------|------|
| success | Boolean | 請求是否成功 |
| data.messages[] | Array | 訊息列表 |
| data.messages[].messageId | String (UUID) | 訊息 ID |
| data.messages[].conversationId | String (UUID) | 對話 ID |
| data.messages[].senderId | String (UUID) | 發送者 ID |
| data.messages[].senderUsername | String | 發送者帳號 |
| data.messages[].content | String | 訊息內容 |
| data.messages[].timestamp | String (ISO 8601) | 發送時間 |
| data.messages[].isRead | Boolean | 是否已讀 |
| data.pagination.hasMore | Boolean | 是否有更多訊息 |

### HTTP 狀態碼

| 狀態碼 | 說明 |
|-------|------|
| 200 | 查詢成功 |
| 401 | 未授權 |
| 403 | 無權限查看此對話 |
| 404 | 對話不存在 |
| 500 | 伺服器內部錯誤 |

---

## 3. 標記訊息為已讀

### 基本資訊

- **端點：** `PUT /api/v1/conversations/{conversationId}/read`
- **描述：** 標記對話中的訊息為已讀
- **需要認證：** 是

### 請求標頭

```
Authorization: Bearer {access-token}
Content-Type: application/json
```

### 路徑參數

| 參數名稱 | 類型 | 說明 |
|---------|------|------|
| conversationId | UUID | 對話 ID |

### 請求主體 (Request Body)

```json
{
  "lastReadMessageId": "msg-550e8400-e29b-41d4-a716-446655440001"
}
```

### 請求欄位說明

| 欄位名稱 | 類型 | 必填 | 說明 |
|---------|------|------|------|
| lastReadMessageId | String (UUID) | 是 | 最後已讀訊息 ID |

### 請求範例

```bash
curl -X PUT http://localhost:8080/api/v1/conversations/{conversationId}/read \
  -H "Authorization: Bearer {your-token}" \
  -H "Content-Type: application/json" \
  -d '{
    "lastReadMessageId": "msg-550e8400-e29b-41d4-a716-446655440001"
  }'
```

### 成功回應 (200 OK)

```json
{
  "success": true,
  "message": "訊息已標記為已讀"
}
```

### HTTP 狀態碼

| 狀態碼 | 說明 |
|-------|------|
| 200 | 標記成功 |
| 401 | 未授權 |
| 404 | 對話或訊息不存在 |
| 500 | 伺服器內部錯誤 |

---

## 4. 查詢未讀訊息總數

### 基本資訊

- **端點：** `GET /api/v1/conversations/unread-count`
- **描述：** 查詢所有對話的未讀訊息總數
- **需要認證：** 是

### 請求標頭

```
Authorization: Bearer {access-token}
```

### 請求範例

```bash
curl -X GET http://localhost:8080/api/v1/conversations/unread-count \
  -H "Authorization: Bearer {your-token}"
```

### 成功回應 (200 OK)

```json
{
  "success": true,
  "data": {
    "totalUnreadCount": 5,
    "conversationUnreadCounts": {
      "conv-550e8400-e29b-41d4-a716-446655440000": 2,
      "conv-550e8400-e29b-41d4-a716-446655440001": 3
    }
  }
}
```

### 成功回應欄位說明

| 欄位路徑 | 類型 | 說明 |
|---------|------|------|
| success | Boolean | 請求是否成功 |
| data.totalUnreadCount | Integer | 所有對話的未讀訊息總數 |
| data.conversationUnreadCounts | Object | 各對話的未讀數（key 為對話 ID） |

### HTTP 狀態碼

| 狀態碼 | 說明 |
|-------|------|
| 200 | 查詢成功 |
| 401 | 未授權 |
| 500 | 伺服器內部錯誤 |

---

## WebSocket API

## 5. WebSocket 連線

### 基本資訊

- **WebSocket URL：** `ws://localhost:8080/ws/chat?token={access-token}`
- **描述：** 建立即時通訊連線
- **需要認證：** 是（透過 URL 參數傳遞 Token）

### 連線範例

```javascript
const token = 'your-access-token';
const ws = new WebSocket(`ws://localhost:8080/ws/chat?token=${token}`);

ws.onopen = () => {
  console.log('WebSocket 已連線');
};

ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  console.log('收到訊息:', message);
};

ws.onerror = (error) => {
  console.error('WebSocket 錯誤:', error);
};

ws.onclose = () => {
  console.log('WebSocket 已斷線');
};
```

### 連線成功回應

```json
{
  "type": "CONNECTION_ESTABLISHED",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": "2025-09-30T10:00:00Z"
  }
}
```

### 連線失敗

- **HTTP 4401：** Token 無效或已過期
- **連線關閉：** Token 驗證失敗

---

## 6. 發送文字訊息

### 訊息類型

`SEND_MESSAGE`

### 訊息格式 (CLIENT → SERVER)

```json
{
  "type": "SEND_MESSAGE",
  "data": {
    "recipientId": "550e8400-e29b-41d4-a716-446655440001",
    "content": "你好，這是一則測試訊息",
    "clientMessageId": "client-msg-550e8400-e29b-41d4-a716-446655440002"
  }
}
```

### 欄位說明

| 欄位路徑 | 類型 | 必填 | 說明 |
|---------|------|------|------|
| type | String | 是 | 訊息類型（固定為 "SEND_MESSAGE"） |
| data.recipientId | String (UUID) | 是 | 接收者 ID |
| data.content | String | 是 | 訊息內容（1-2000 字元） |
| data.clientMessageId | String (UUID) | 是 | 前端生成的訊息 ID（用於去重） |

### 發送範例

```javascript
const message = {
  type: 'SEND_MESSAGE',
  data: {
    recipientId: '550e8400-e29b-41d4-a716-446655440001',
    content: '你好，這是一則測試訊息',
    clientMessageId: crypto.randomUUID()
  }
};

ws.send(JSON.stringify(message));
```

---

## 7. 接收新訊息

### 訊息類型

`NEW_MESSAGE`

### 訊息格式 (SERVER → CLIENT)

```json
{
  "type": "NEW_MESSAGE",
  "data": {
    "messageId": "msg-550e8400-e29b-41d4-a716-446655440003",
    "conversationId": "conv-550e8400-e29b-41d4-a716-446655440000",
    "senderId": "550e8400-e29b-41d4-a716-446655440001",
    "senderUsername": "alice_wang",
    "content": "你好！",
    "timestamp": "2025-09-30T10:30:00Z",
    "clientMessageId": "client-msg-550e8400-e29b-41d4-a716-446655440002"
  }
}
```

### 欄位說明

| 欄位路徑 | 類型 | 說明 |
|---------|------|------|
| type | String | 訊息類型（"NEW_MESSAGE"） |
| data.messageId | String (UUID) | 伺服器生成的訊息 ID |
| data.conversationId | String (UUID) | 對話 ID |
| data.senderId | String (UUID) | 發送者 ID |
| data.senderUsername | String | 發送者帳號 |
| data.content | String | 訊息內容 |
| data.timestamp | String (ISO 8601) | 發送時間 |
| data.clientMessageId | String (UUID) | 客戶端訊息 ID（如有） |

### 接收範例

```javascript
ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  
  if (message.type === 'NEW_MESSAGE') {
    console.log('收到新訊息:', message.data);
    // 顯示訊息、播放提示音等
  }
};
```

---

## 8. 訊息送達確認

### 訊息類型

`MESSAGE_DELIVERED`

### 訊息格式 (SERVER → CLIENT)

```json
{
  "type": "MESSAGE_DELIVERED",
  "data": {
    "messageId": "msg-550e8400-e29b-41d4-a716-446655440003",
    "clientMessageId": "client-msg-550e8400-e29b-41d4-a716-446655440002",
    "timestamp": "2025-09-30T10:30:00Z"
  }
}
```

### 欄位說明

| 欄位路徑 | 類型 | 說明 |
|---------|------|------|
| type | String | 訊息類型（"MESSAGE_DELIVERED"） |
| data.messageId | String (UUID) | 伺服器生成的訊息 ID |
| data.clientMessageId | String (UUID) | 客戶端訊息 ID |
| data.timestamp | String (ISO 8601) | 送達時間 |

### 用途

- 確認訊息已成功儲存到伺服器
- 更新前端訊息狀態（從「發送中」改為「已送達」）
- 將 clientMessageId 對應到 messageId

---

## 9. 正在輸入狀態

### 9.1 開始輸入

**訊息類型：** `TYPING_START`

**格式 (CLIENT → SERVER)：**
```json
{
  "type": "TYPING_START",
  "data": {
    "recipientId": "550e8400-e29b-41d4-a716-446655440001"
  }
}
```

### 9.2 停止輸入

**訊息類型：** `TYPING_STOP`

**格式 (CLIENT → SERVER)：**
```json
{
  "type": "TYPING_STOP",
  "data": {
    "recipientId": "550e8400-e29b-41d4-a716-446655440001"
  }
}
```

### 9.3 接收輸入狀態

**訊息類型：** `USER_TYPING`

**格式 (SERVER → CLIENT)：**
```json
{
  "type": "USER_TYPING",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440001",
    "username": "alice_wang",
    "isTyping": true
  }
}
```

### 實作範例

```javascript
let typingTimeout;

// 輸入框 onChange 事件
function handleInputChange(e) {
  const content = e.target.value;
  
  // 發送正在輸入狀態
  if (!typingTimeout) {
    ws.send(JSON.stringify({
      type: 'TYPING_START',
      data: { recipientId: currentRecipientId }
    }));
  }
  
  // 清除舊的計時器
  clearTimeout(typingTimeout);
  
  // 3秒後自動停止
  typingTimeout = setTimeout(() => {
    ws.send(JSON.stringify({
      type: 'TYPING_STOP',
      data: { recipientId: currentRecipientId }
    }));
    typingTimeout = null;
  }, 3000);
}
```

---

## 10. 訊息已讀回執

### 10.1 發送已讀通知

**訊息類型：** `MESSAGE_READ`

**格式 (CLIENT → SERVER)：**
```json
{
  "type": "MESSAGE_READ",
  "data": {
    "conversationId": "conv-550e8400-e29b-41d4-a716-446655440000",
    "messageId": "msg-550e8400-e29b-41d4-a716-446655440003"
  }
}
```

### 10.2 接收已讀回執

**訊息類型：** `MESSAGE_READ_RECEIPT`

**格式 (SERVER → CLIENT)：**
```json
{
  "type": "MESSAGE_READ_RECEIPT",
  "data": {
    "conversationId": "conv-550e8400-e29b-41d4-a716-446655440000",
    "messageId": "msg-550e8400-e29b-41d4-a716-