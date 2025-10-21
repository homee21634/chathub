# 好友系統 API 文件

**版本：** 1.0  
**最後更新：** 2025-09-30

---

## API 列表

1. [發送好友請求](#1-發送好友請求)
2. [查看收到的好友請求](#2-查看收到的好友請求)
3. [處理好友請求](#3-處理好友請求)
4. [查看好友列表](#4-查看好友列表)
5. [刪除好友](#5-刪除好友)
6. [批次查詢線上狀態](#6-批次查詢線上狀態)

---

## 1. 發送好友請求

### 基本資訊

- **端點：** `POST /api/v1/friends/requests`
- **描述：** 向指定使用者發送好友請求
- **需要認證：** 是

### 請求標頭

```
Authorization: Bearer {access-token}
Content-Type: application/json
```

### 請求主體 (Request Body)

```json
{
  "targetUsername": "string"
}
```

### 請求欄位說明

| 欄位名稱 | 類型 | 必填 | 說明 |
|---------|------|------|------|
| targetUsername | String | 是 | 目標使用者的帳號 |

### 請求範例

```bash
curl -X POST http://localhost:8080/api/v1/friends/requests \
  -H "Authorization: Bearer {your-token}" \
  -H "Content-Type: application/json" \
  -d '{
    "targetUsername": "alice_wang"
  }'
```

### 成功回應 (201 Created)

```json
{
  "success": true,
  "message": "好友請求已發送",
  "data": {
    "requestId": "req-550e8400-e29b-41d4-a716-446655440000",
    "targetUser": {
      "userId": "550e8400-e29b-41d4-a716-446655440001",
      "username": "alice_wang"
    },
    "status": "PENDING",
    "createdAt": "2025-09-30T10:30:00Z"
  }
}
```

### 成功回應欄位說明

| 欄位路徑 | 類型 | 說明 |
|---------|------|------|
| success | Boolean | 請求是否成功 |
| message | String | 回應訊息 |
| data.requestId | String (UUID) | 好友請求 ID |
| data.targetUser.userId | String (UUID) | 目標使用者 ID |
| data.targetUser.username | String | 目標使用者帳號 |
| data.status | String | 請求狀態（PENDING, ACCEPTED, REJECTED） |
| data.createdAt | String (ISO 8601) | 請求建立時間 |

### 錯誤回應

#### 400 Bad Request - 不能加自己為好友

```json
{
  "success": false,
  "message": "不能加自己為好友"
}
```

#### 404 Not Found - 使用者不存在

```json
{
  "success": false,
  "message": "使用者不存在"
}
```

#### 409 Conflict - 請求已存在

```json
{
  "success": false,
  "message": "已發送過好友請求，請等待對方回應"
}
```

#### 409 Conflict - 已是好友

```json
{
  "success": false,
  "message": "已經是好友關係"
}
```

### HTTP 狀態碼

| 狀態碼 | 說明 |
|-------|------|
| 201 | 請求發送成功 |
| 400 | 參數錯誤或不能加自己為好友 |
| 401 | 未授權 |
| 404 | 目標使用者不存在 |
| 409 | 請求已存在或已是好友 |
| 500 | 伺服器內部錯誤 |

---

## 2. 查看收到的好友請求

### 基本資訊

- **端點：** `GET /api/v1/friends/requests/received`
- **描述：** 查看收到的好友請求列表
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
| status | String | 否 | 請求狀態 | PENDING |

### 請求範例

```bash
curl -X GET "http://localhost:8080/api/v1/friends/requests/received?page=0&size=20&status=PENDING" \
  -H "Authorization: Bearer {your-token}"
```

### 成功回應 (200 OK)

```json
{
  "success": true,
  "data": {
    "requests": [
      {
        "requestId": "req-550e8400-e29b-41d4-a716-446655440000",
        "fromUser": {
          "userId": "550e8400-e29b-41d4-a716-446655440001",
          "username": "bob_chen"
        },
        "status": "PENDING",
        "createdAt": "2025-09-30T09:00:00Z"
      },
      {
        "requestId": "req-550e8400-e29b-41d4-a716-446655440002",
        "fromUser": {
          "userId": "550e8400-e29b-41d4-a716-446655440003",
          "username": "carol_lin"
        },
        "status": "PENDING",
        "createdAt": "2025-09-30T08:30:00Z"
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
| data.requests[] | Array | 好友請求列表 |
| data.requests[].requestId | String (UUID) | 請求 ID |
| data.requests[].fromUser.userId | String (UUID) | 發送者 ID |
| data.requests[].fromUser.username | String | 發送者帳號 |
| data.requests[].status | String | 請求狀態 |
| data.requests[].createdAt | String (ISO 8601) | 請求時間 |
| data.pagination.currentPage | Integer | 當前頁碼 |
| data.pagination.totalPages | Integer | 總頁數 |
| data.pagination.totalItems | Integer | 總筆數 |
| data.pagination.pageSize | Integer | 每頁筆數 |

### HTTP 狀態碼

| 狀態碼 | 說明 |
|-------|------|
| 200 | 查詢成功 |
| 401 | 未授權 |
| 500 | 伺服器內部錯誤 |

---

## 3. 處理好友請求

### 基本資訊

- **端點：** `PUT /api/v1/friends/requests/{requestId}`
- **描述：** 接受或拒絕好友請求
- **需要認證：** 是

### 請求標頭

```
Authorization: Bearer {access-token}
Content-Type: application/json
```

### 路徑參數

| 參數名稱 | 類型 | 說明 |
|---------|------|------|
| requestId | UUID | 好友請求 ID |

### 請求主體 (Request Body)

```json
{
  "action": "ACCEPT"
}
```

或

```json
{
  "action": "REJECT"
}
```

### 請求欄位說明

| 欄位名稱 | 類型 | 必填 | 說明 | 可選值 |
|---------|------|------|------|-------|
| action | String | 是 | 處理動作 | ACCEPT, REJECT |

### 請求範例

```bash
# 接受請求
curl -X PUT http://localhost:8080/api/v1/friends/requests/{requestId} \
  -H "Authorization: Bearer {your-token}" \
  -H "Content-Type: application/json" \
  -d '{"action": "ACCEPT"}'

# 拒絕請求
curl -X PUT http://localhost:8080/api/v1/friends/requests/{requestId} \
  -H "Authorization: Bearer {your-token}" \
  -H "Content-Type: application/json" \
  -d '{"action": "REJECT"}'
```

### 成功回應 - 接受 (200 OK)

```json
{
  "success": true,
  "message": "已接受好友請求",
  "data": {
    "friendshipId": "friend-550e8400-e29b-41d4-a716-446655440000",
    "friend": {
      "userId": "550e8400-e29b-41d4-a716-446655440001",
      "username": "bob_chen"
    },
    "createdAt": "2025-09-30T10:35:00Z"
  }
}
```

### 成功回應 - 拒絕 (200 OK)

```json
{
  "success": true,
  "message": "已拒絕好友請求"
}
```

### 成功回應欄位說明（接受）

| 欄位路徑 | 類型 | 說明 |
|---------|------|------|
| success | Boolean | 請求是否成功 |
| message | String | 回應訊息 |
| data.friendshipId | String (UUID) | 好友關係 ID |
| data.friend.userId | String (UUID) | 好友 ID |
| data.friend.username | String | 好友帳號 |
| data.createdAt | String (ISO 8601) | 好友關係建立時間 |

### 錯誤回應

#### 404 Not Found - 請求不存在

```json
{
  "success": false,
  "message": "好友請求不存在或已處理"
}
```

#### 403 Forbidden - 無權限

```json
{
  "success": false,
  "message": "無權限處理此好友請求"
}
```

### HTTP 狀態碼

| 狀態碼 | 說明 |
|-------|------|
| 200 | 處理成功 |
| 400 | 參數錯誤 |
| 401 | 未授權 |
| 403 | 無權限處理 |
| 404 | 請求不存在 |
| 500 | 伺服器內部錯誤 |

---

## 4. 查看好友列表

### 基本資訊

- **端點：** `GET /api/v1/friends`
- **描述：** 查看已接受的好友列表
- **需要認證：** 是

### 請求標頭

```
Authorization: Bearer {access-token}
```

### 查詢參數 (Query Parameters)

| 參數名稱 | 類型 | 必填 | 說明 | 預設值 |
|---------|------|------|------|-------|
| page | Integer | 否 | 頁碼（從 0 開始） | 0 |
| size | Integer | 否 | 每頁筆數 | 50 |
| search | String | 否 | 搜尋關鍵字（帳號） | - |

### 請求範例

```bash
curl -X GET "http://localhost:8080/api/v1/friends?page=0&size=50&search=alice" \
  -H "Authorization: Bearer {your-token}"
```

### 成功回應 (200 OK)

```json
{
  "success": true,
  "data": {
    "friends": [
      {
        "friendshipId": "friend-550e8400-e29b-41d4-a716-446655440000",
        "friend": {
          "userId": "550e8400-e29b-41d4-a716-446655440001",
          "username": "alice_wang"
        },
        "isOnline": true,
        "lastSeenAt": "2025-09-30T10:30:00Z",
        "createdAt": "2025-09-25T14:20:00Z"
      },
      {
        "friendshipId": "friend-550e8400-e29b-41d4-a716-446655440002",
        "friend": {
          "userId": "550e8400-e29b-41d4-a716-446655440003",
          "username": "bob_chen"
        },
        "isOnline": false,
        "lastSeenAt": "2025-09-30T08:15:00Z",
        "createdAt": "2025-09-20T09:30:00Z"
      }
    ],
    "pagination": {
      "currentPage": 0,
      "totalPages": 1,
      "totalItems": 2,
      "pageSize": 50
    }
  }
}
```

### 成功回應欄位說明

| 欄位路徑 | 類型 | 說明 |
|---------|------|------|
| success | Boolean | 請求是否成功 |
| data.friends[] | Array | 好友列表 |
| data.friends[].friendshipId | String (UUID) | 好友關係 ID |
| data.friends[].friend.userId | String (UUID) | 好友 ID |
| data.friends[].friend.username | String | 好友帳號 |
| data.friends[].isOnline | Boolean | 是否線上 |
| data.friends[].lastSeenAt | String (ISO 8601) | 最後上線時間 |
| data.friends[].createdAt | String (ISO 8601) | 成為好友時間 |
| data.pagination | Object | 分頁資訊 |

### HTTP 狀態碼

| 狀態碼 | 說明 |
|-------|------|
| 200 | 查詢成功 |
| 401 | 未授權 |
| 500 | 伺服器內部錯誤 |

---

## 5. 刪除好友

### 基本資訊

- **端點：** `DELETE /api/v1/friends/{friendshipId}`
- **描述：** 刪除好友關係
- **需要認證：** 是

### 請求標頭

```
Authorization: Bearer {access-token}
```

### 路徑參數

| 參數名稱 | 類型 | 說明 |
|---------|------|------|
| friendshipId | UUID | 好友關係 ID |

### 請求範例

```bash
curl -X DELETE http://localhost:8080/api/v1/friends/{friendshipId} \
  -H "Authorization: Bearer {your-token}"
```

### 成功回應 (200 OK)

```json
{
  "success": true,
  "message": "已刪除好友"
}
```

### 成功回應欄位說明

| 欄位名稱 | 類型 | 說明 |
|---------|------|------|
| success | Boolean | 請求是否成功 |
| message | String | 回應訊息 |

### 錯誤回應

#### 404 Not Found - 好友關係不存在

```json
{
  "success": false,
  "message": "好友關係不存在"
}
```

#### 403 Forbidden - 無權限

```json
{
  "success": false,
  "message": "無權限刪除此好友關係"
}
```

### HTTP 狀態碼

| 狀態碼 | 說明 |
|-------|------|
| 200 | 刪除成功 |
| 401 | 未授權 |
| 403 | 無權限刪除 |
| 404 | 好友關係不存在 |
| 500 | 伺服器內部錯誤 |

---

## 6. 批次查詢線上狀態

### 基本資訊

- **端點：** `POST /api/v1/friends/online-status`
- **描述：** 批次查詢多個好友的線上狀態
- **需要認證：** 是

### 請求標頭

```
Authorization: Bearer {access-token}
Content-Type: application/json
```

### 請求主體 (Request Body)

```json
{
  "userIds": [
    "550e8400-e29b-41d4-a716-446655440001",
    "550e8400-e29b-41d4-a716-446655440002",
    "550e8400-e29b-41d4-a716-446655440003"
  ]
}
```

### 請求欄位說明

| 欄位名稱 | 類型 | 必填 | 說明 |
|---------|------|------|------|
| userIds | Array<String> | 是 | 使用者 ID 列表（UUID 陣列） |

### 請求範例

```bash
curl -X POST http://localhost:8080/api/v1/friends/online-status \
  -H "Authorization: Bearer {your-token}" \
  -H "Content-Type: application/json" \
  -d '{
    "userIds": [
      "550e8400-e29b-41d4-a716-446655440001",
      "550e8400-e29b-41d4-a716-446655440002"
    ]
  }'
```

### 成功回應 (200 OK)

```json
{
  "success": true,
  "data": {
    "550e8400-e29b-41d4-a716-446655440001": {
      "isOnline": true,
      "lastSeenAt": "2025-09-30T10:30:00Z"
    },
    "550e8400-e29b-41d4-a716-446655440002": {
      "isOnline": false,
      "lastSeenAt": "2025-09-30T08:15:00Z"
    },
    "550e8400-e29b-41d4-a716-446655440003": {
      "isOnline": true,
      "lastSeenAt": "2025-09-30T10:28:00Z"
    }
  }
}
```

### 成功回應欄位說明

| 欄位路徑 | 類型 | 說明 |
|---------|------|------|
| success | Boolean | 請求是否成功 |
| data.{userId}.isOnline | Boolean | 該使用者是否線上 |
| data.{userId}.lastSeenAt | String (ISO 8601) | 最後上線時間 |

### HTTP 狀態碼

| 狀態碼 | 說明 |
|-------|------|
| 200 | 查詢成功 |
| 400 | 參數錯誤 |
| 401 | 未授權 |
| 500 | 伺服器內部錯誤 |

---

## 業務規則

### 1. 好友請求規則
- 不能對自己發送請求
- 同一對使用者只能有一個待處理請求
- 已是好友不能再發送請求
- 請求狀態：PENDING（待處理）、ACCEPTED（已接受）、REJECTED（已拒絕）

### 2. 好友關係規則
- 雙向關係：A 接受 B 的請求後，建立兩筆記錄（A→B 和 B→A）
- 不能與自己成為好友
- 唯一性：防止重複的好友關係

### 3. 線上狀態
- 資料來源：Redis（即時）
- 更新頻率：心跳機制（每 5 分鐘）
- TTL：1 小時

---

## 錯誤碼對照表

| 錯誤碼 | HTTP 狀態 | 說明 | 解決方式 |
|-------|----------|------|---------|
| CANNOT_ADD_SELF | 400 | 不能加自己為好友 | 輸入其他使用者帳號 |
| USER_NOT_FOUND | 404 | 使用者不存在 | 確認帳號是否正確 |
| REQUEST_ALREADY_EXISTS | 409 | 請求已存在 | 等待對方回應 |
| ALREADY_FRIENDS | 409 | 已是好友關係 | 無需重複新增 |
| REQUEST_NOT_FOUND | 404 | 請求不存在 | 確認請求 ID |
| NO_PERMISSION | 403 | 無權限 | 只能處理發給自己的請求 |
| FRIENDSHIP_NOT_FOUND | 404 | 好友關係不存在 | 確認關係 ID |

---

## 整合範例

### JavaScript (完整好友系統)

```javascript
class FriendService {
  constructor(baseURL = 'http://localhost:8080/api/v1/friends') {
    this.baseURL = baseURL;
  }

  // 取得 Token
  getToken() {
    return localStorage.getItem('accessToken') || 
           sessionStorage.getItem('accessToken');
  }

  // 發送好友請求
  async sendFriendRequest(targetUsername) {
    try {
      const response = await fetch(`${this.baseURL}/requests`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${this.getToken()}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ targetUsername })
      });

      const data = await response.json();
      
      if (response.ok) {
        return { success: true, data: data.data };
      } else {
        return { success: false, message: data.message };
      }
    } catch (error) {
      console.error('發送好友請求錯誤:', error);
      return { success: false, message: '網路連線失敗' };
    }
  }

  // 查看收到的請求
  async getReceivedRequests(page = 0, size = 20) {
    try {
      const response = await fetch(
        `${this.baseURL}/requests/received?page=${page}&size=${size}&status=PENDING`,
        {
          headers: {
            'Authorization': `Bearer ${this.getToken()}`
          }
        }
      );

      const data = await response.json();
      return response.ok ? data.data : null;
    } catch (error) {
      console.error('查詢好友請求錯誤:', error);
      return null;
    }
  }

  // 處理好友請求
  async handleRequest(requestId, action) {
    try {
      const response = await fetch(`${this.baseURL}/requests/${requestId}`, {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${this.getToken()}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ action })
      });

      const data = await response.json();
      return { success: response.ok, data: data.data, message: data.message };
    } catch (error) {
      console.error('處理請求錯誤:', error);
      return { success: false, message: '網路連線失敗' };
    }
  }

  // 查看好友列表
  async getFriends(page = 0, size = 50, search = '') {
    try {
      const params = new URLSearchParams({ page, size });
      if (search) params.append('search', search);
      
      const response = await fetch(`${this.baseURL}?${params}`, {
        headers: {
          'Authorization': `Bearer ${this.getToken()}`
        }
      });

      const data = await response.json();
      return response.ok ? data.data : null;
    } catch (error) {
      console.error('查詢好友列表錯誤:', error);
      return null;
    }
  }

  // 刪除好友
  async deleteFriend(friendshipId) {
    try {
      const response = await fetch(`${this.baseURL}/${friendshipId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${this.getToken()}`
        }
      });

      return response.ok;
    } catch (error) {
      console.error('刪除好友錯誤:', error);
      return false;
    }
  }

  // 批次查詢線上狀態
  async getOnlineStatus(userIds) {
    try {
      const response = await fetch(`${this.baseURL}/online-status`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${this.getToken()}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ userIds })
      });

      const data = await response.json();
      return response.ok ? data.data : null;
    } catch (error) {
      console.error('查詢線上狀態錯誤:', error);
      return null;
    }
  }
}

// 使用範例
const friendService = new FriendService();

// 發送好友請求
await friendService.sendFriendRequest('alice_wang');

// 接受好友請求
await friendService.handleRequest('request-id', 'ACCEPT');

// 查看好友列表
const friends = await friendService.getFriends();

// 刪除好友
await friendService.deleteFriend('friendship-id');
```

---

## 測試案例

### 成功案例

```json
// 測試案例 1: 發送好友請求
{
  "targetUsername": "alice_wang"
}
// 預期: 201 Created

// 測試案例 2: 接受好友請求
{
  "action": "ACCEPT"
}
// 預期: 200 OK，建立好友關係

// 測試案例 3: 拒絕好友請求
{
  "action": "REJECT"
}
// 預期: 200 OK

// 測試案例 4: 查詢好友列表（有搜尋）
GET /api/v1/friends?search=alice
// 預期: 200 OK，返回包含 "alice" 的好友
```

### 失敗案例

```json
// 測試案例 5: 加自己為好友
{
  "targetUsername": "john_doe"
}
// 預期: 400 Bad Request（假設當前使用者是 john_doe）

// 測試案例 6: 重複發送請求
// 第一次: 201 Created
// 第二次: 409 Conflict

// 測試案例 7: 處理不存在的請求
PUT /api/v1/friends/requests/invalid-id
{
  "action": "ACCEPT"
}
// 預期: 404 Not Found

// 測試案例 8: 處理他人的請求
// User A 試圖處理 User B 收到的請求
// 預期: 403 Forbidden
```

---

## 版本歷史

| 版本 | 日期 | 變更內容 |
|-----|------|---------|
| 1.0 | 2025-09-30 | 初版發布 |