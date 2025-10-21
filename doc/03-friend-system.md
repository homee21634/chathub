# 簡易聊天系統開發文件 - 好友系統

**文件版本：** 1.0  
**最後更新：** 2025-09-30  
**狀態：** 待審核

---

## 目錄

1. [UI/UX 設計 - 好友系統頁面](#1-uiux-設計---好友系統頁面)
2. [後端 API 文件 - 好友功能](#2-後端-api-文件---好友功能)
3. [系統架構圖 - 好友系統](#3-系統架構圖---好友系統)
4. [資料庫架構 (PostgreSQL)](#4-資料庫架構-postgresql)
5. [業務邏輯規則](#5-業務邏輯規則)
6. [後端實作架構 (Spring Boot)](#6-後端實作架構-spring-boot)
7. [前端實作架構 (React)](#7-前端實作架構-react)
8. [錯誤處理與邊界情況](#8-錯誤處理與邊界情況)
9. [文件版本記錄](#9-文件版本記錄)

---

## 1. UI/UX 設計 - 好友系統頁面

### 1.1 頁面佈局設計

**頁面名稱：** 好友列表與管理頁面 (Friend List & Management)

**設計規格：**

- **版面配置：** 雙欄式佈局（左側列表 + 右側詳情）
- **響應式設計：**
  - 桌面版 (1200px+)：雙欄並排
  - 手機版 (< 768px)：單欄切換
- **色彩方案：**
  - 主色：#1a1a1a (深黑)
  - 背景：#ffffff (白色)
  - 次要背景：#f9fafb (淺灰)
  - 邊框：#e5e7eb (灰色)
  - 線上狀態：#10b981 (綠色)
  - 離線狀態：#6b7280 (灰色)

### 1.2 左側欄 - 好友列表區域

#### 1. 使用者資訊卡片（頂部，深色背景）
**元素：**
- 使用者頭像（圓形，48px）
  - 顯示縮寫字母
  - 線上狀態指示器（綠點/灰點，12px）
- 使用者帳號名稱
- 線上狀態文字
- 操作按鈕組：
  - ➕ 新增好友（主要按鈕，黑底白字）
  - ⚙️ 設定
  - 🚪 登出

#### 2. 搜尋欄
**元素：**
- 搜尋圖示（🔍）
- 輸入框：「搜尋好友...」
- 即時過濾功能

#### 3. 標籤切換
**兩個標籤：**
- 「好友」標籤（預設選中）
- 「請求」標籤（顯示未讀數量徽章，紅色）

**互動：**
- 點擊切換顯示內容
- 選中標籤底部顯示黑色底線

#### 4. 好友列表項目
**每個項目包含：**
- 頭像（40px，縮寫 + 線上狀態點）
- 好友資訊：
  - 帳號名稱（15px，粗體）
  - 狀態文字（13px，灰色）
    - 線上：「線上 - 最後上線時間」
    - 離線：「離線 - 最後上線時間」
- Hover 效果：淺灰背景
- 選中效果：灰色背景

#### 5. 好友請求項目
**每個項目包含：**
- 頭像（40px）
- 請求資訊：
  - 帳號名稱
  - 「想要加你為好友」文字
- 操作按鈕：
  - ✓ 接受（綠色，hover 顯示綠色背景）
  - ✕ 拒絕（紅色，hover 顯示紅色背景）

### 1.3 右側欄 - 好友詳情與操作

#### 1. 頁面標題列
**元素：**
- ⬅️ 返回按鈕（僅手機版）
- 頁面標題：「好友列表」或好友帳號

#### 2. 空白狀態（未選中好友時）
**元素：**
- 大圖示：👥（64px）
- 標題：「選擇一個好友開始聊天」
- 說明：「或點擊「➕」按鈕新增好友」

#### 3. 好友詳情頁（選中好友後）
**元素：**
- 大頭像（100px，置中）
  - 縮寫字母
  - 線上狀態指示器
- 好友帳號（24px，粗體，置中）
- 狀態文字（14px，灰色，置中）
  - 「線上 - 最後上線：剛剛」
  - 「離線 - 最後上線：2小時前」

**操作按鈕組（水平排列，置中）：**
1. 💬 開始聊天（主要按鈕，黑底白字）
2. 👤 查看資料（次要按鈕，白底黑字黑框）
3. 🗑️ 刪除好友（危險按鈕，白底紅字紅框）

### 1.4 新增好友 Modal

**觸發方式：** 點擊「➕」按鈕

**Modal 設計：**
- **背景遮罩：** 半透明黑色（rgba(0,0,0,0.5)）
- **卡片：** 白色，圓角 16px，最大寬度 400px
- **內容：**
  - 標題：「新增好友」（20px，粗體）
  - 說明：「輸入對方的帳號以發送好友請求」（14px，灰色）
  - 成功/錯誤訊息區域（動態顯示）
  - 表單欄位：
    - Label: "好友帳號"
    - Input: "請輸入帳號"
  - 按鈕組：
    - 「取消」（次要按鈕，白底黑字黑框）
    - 「發送請求」（主要按鈕，黑底白字）

**互動流程：**
1. 使用者輸入帳號
2. 點擊「發送請求」
3. 顯示載入狀態
4. 成功：顯示綠色成功訊息，1.5秒後關閉 Modal
5. 失敗：顯示紅色錯誤訊息

---

## 2. 後端 API 文件 - 好友功能

### 2.1 需求說明

**功能需求：**
1. 搜尋使用者並發送好友請求
2. 查看收到的好友請求列表
3. 接受或拒絕好友請求
4. 查看已接受的好友列表
5. 刪除好友關係
6. 批次查詢好友線上狀態

**安全需求：**
- 必須經過 JWT Token 驗證
- 不能對自己發送好友請求
- 不能重複發送好友請求
- 好友關係為雙向確認

### 2.2 API 端點列表

#### 2.2.1 發送好友請求

**端點：** `POST /api/v1/friends/requests`

**請求標頭：**
```
Authorization: Bearer {access-token}
Content-Type: application/json
```

**請求主體：**
```json
{
  "targetUsername": "string"
}
```

**欄位說明：**

| 欄位名稱 | 類型 | 必填 | 說明 |
|---------|------|------|------|
| targetUsername | String | 是 | 目標使用者的帳號 |

**成功回應 (201 Created)：**
```json
{
  "success": true,
  "message": "好友請求已發送",
  "data": {
    "requestId": "uuid-string",
    "targetUser": {
      "userId": "uuid-string",
      "username": "alice_wang"
    },
    "status": "PENDING",
    "createdAt": "2025-09-30T10:30:00Z"
  }
}
```

**錯誤回應：**

**400 Bad Request - 驗證失敗**
```json
{
  "success": false,
  "message": "不能加自己為好友"
}
```

**404 Not Found - 使用者不存在**
```json
{
  "success": false,
  "message": "使用者不存在"
}
```

**409 Conflict - 請求已存在**
```json
{
  "success": false,
  "message": "已發送過好友請求，請等待對方回應"
}
```

**409 Conflict - 已是好友**
```json
{
  "success": false,
  "message": "已經是好友關係"
}
```

#### 2.2.2 查看收到的好友請求列表

**端點：** `GET /api/v1/friends/requests/received`

**請求標頭：**
```
Authorization: Bearer {access-token}
```

**查詢參數：**

| 參數名稱 | 類型 | 必填 | 說明 | 預設值 |
|---------|------|------|------|-------|
| page | Integer | 否 | 頁碼（從 0 開始） | 0 |
| size | Integer | 否 | 每頁筆數 | 20 |
| status | String | 否 | 請求狀態（PENDING/ACCEPTED/REJECTED） | PENDING |

**成功回應 (200 OK)：**
```json
{
  "success": true,
  "data": {
    "requests": [
      {
        "requestId": "uuid-string",
        "fromUser": {
          "userId": "uuid-string",
          "username": "bob_chen"
        },
        "status": "PENDING",
        "createdAt": "2025-09-30T09:00:00Z"
      },
      {
        "requestId": "uuid-string-2",
        "fromUser": {
          "userId": "uuid-string-2",
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

#### 2.2.3 處理好友請求（接受/拒絕）

**端點：** `PUT /api/v1/friends/requests/{requestId}`

**請求標頭：**
```
Authorization: Bearer {access-token}
Content-Type: application/json
```

**路徑參數：**

| 參數名稱 | 類型 | 說明 |
|---------|------|------|
| requestId | UUID | 好友請求 ID |

**請求主體：**
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

**欄位說明：**

| 欄位名稱 | 類型 | 必填 | 說明 | 可選值 |
|---------|------|------|------|-------|
| action | String | 是 | 處理動作 | ACCEPT, REJECT |

**成功回應 - 接受 (200 OK)：**
```json
{
  "success": true,
  "message": "已接受好友請求",
  "data": {
    "friendshipId": "uuid-string",
    "friend": {
      "userId": "uuid-string",
      "username": "bob_chen"
    },
    "createdAt": "2025-09-30T10:35:00Z"
  }
}
```

**成功回應 - 拒絕 (200 OK)：**
```json
{
  "success": true,
  "message": "已拒絕好友請求"
}
```

**錯誤回應：**

**404 Not Found - 請求不存在**
```json
{
  "success": false,
  "message": "好友請求不存在或已處理"
}
```

**403 Forbidden - 無權限**
```json
{
  "success": false,
  "message": "無權限處理此好友請求"
}
```

#### 2.2.4 查看好友列表

**端點：** `GET /api/v1/friends`

**請求標頭：**
```
Authorization: Bearer {access-token}
```

**查詢參數：**

| 參數名稱 | 類型 | 必填 | 說明 | 預設值 |
|---------|------|------|------|-------|
| page | Integer | 否 | 頁碼（從 0 開始） | 0 |
| size | Integer | 否 | 每頁筆數 | 50 |
| search | String | 否 | 搜尋關鍵字（帳號） | - |

**成功回應 (200 OK)：**
```json
{
  "success": true,
  "data": {
    "friends": [
      {
        "friendshipId": "uuid-string",
        "friend": {
          "userId": "uuid-string",
          "username": "alice_wang"
        },
        "isOnline": true,
        "lastSeenAt": "2025-09-30T10:30:00Z",
        "createdAt": "2025-09-25T14:20:00Z"
      },
      {
        "friendshipId": "uuid-string-2",
        "friend": {
          "userId": "uuid-string-2",
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

#### 2.2.5 刪除好友

**端點：** `DELETE /api/v1/friends/{friendshipId}`

**請求標頭：**
```
Authorization: Bearer {access-token}
```

**路徑參數：**

| 參數名稱 | 類型 | 說明 |
|---------|------|------|
| friendshipId | UUID | 好友關係 ID |

**成功回應 (200 OK)：**
```json
{
  "success": true,
  "message": "已刪除好友"
}
```

**錯誤回應：**

**404 Not Found - 好友關係不存在**
```json
{
  "success": false,
  "message": "好友關係不存在"
}
```

**403 Forbidden - 無權限**
```json
{
  "success": false,
  "message": "無權限刪除此好友關係"
}
```

#### 2.2.6 批次查詢好友線上狀態

**端點：** `POST /api/v1/friends/online-status`

**請求標頭：**
```
Authorization: Bearer {access-token}
Content-Type: application/json
```

**請求主體：**
```json
{
  "userIds": [
    "uuid-1",
    "uuid-2",
    "uuid-3"
  ]
}
```

**成功回應 (200 OK)：**
```json
{
  "success": true,
  "data": {
    "uuid-1": {
      "isOnline": true,
      "lastSeenAt": "2025-09-30T10:30:00Z"
    },
    "uuid-2": {
      "isOnline": false,
      "lastSeenAt": "2025-09-30T08:15:00Z"
    },
    "uuid-3": {
      "isOnline": true,
      "lastSeenAt": "2025-09-30T10:28:00Z"
    }
  }
}
```

---

## 3. 系統架構圖 - 好友系統

### 3.1 好友請求流程

```
┌─────────┐          ┌─────────┐          ┌──────────────┐          ┌─────────────┐
│ User A  │          │  React  │          │ Spring Boot  │          │ PostgreSQL  │
│(發送者) │          │Frontend │          │   Backend    │          │             │
└────┬────┘          └────┬────┘          └──────┬───────┘          └──────┬──────┘
     │                    │                       │                         │
     │ 1. 輸入對方帳號    │                       │                         │
     ├───────────────────>│                       │                         │
     │                    │                       │                         │
     │                    │ 2. POST /api/v1/friends/requests                │
     │                    ├──────────────────────>│                         │
     │                    │  {targetUsername}     │                         │
     │                    │                       │                         │
     │                    │                       │ 3. 驗證 Token          │
     │                    │                       │    取得 User A ID      │
     │                    │                       │                         │
     │                    │                       │ 4. 查詢目標使用者      │
     │                    │                       ├────────────────────────>│
     │                    │                       │ SELECT * FROM users    │
     │                    │                       │ WHERE username = ?     │
     │                    │                       │                         │
     │                    │                       │ 5. 返回 User B 資料    │
     │                    │                       │<────────────────────────┤
     │                    │                       │                         │
     │                    │                       │ 6. 檢查是否已是好友    │
     │                    │                       ├────────────────────────>│
     │                    │                       │                         │
     │                    │                       │ 7. 檢查是否已有請求    │
     │                    │                       ├────────────────────────>│
     │                    │                       │                         │
     │                    │                       │ 8. 建立好友請求記錄    │
     │                    │                       ├────────────────────────>│
     │                    │                       │ INSERT friend_requests │
     │                    │                       │                         │
     │                    │ 9. 返回成功            │                         │
     │                    │<──────────────────────┤                         │
     │                    │                       │                         │
     │ 10. 顯示成功訊息   │                       │                         │
     │<───────────────────┤                       │                         │
     │                    │                       │                         │
     
     
┌─────────┐          ┌─────────┐          ┌──────────────┐          ┌─────────────┐
│ User B  │          │  React  │          │ Spring Boot  │          │ PostgreSQL  │
│(接收者) │          │Frontend │          │   Backend    │          │             │
└────┬────┘          └────┬────┘          └──────┬───────┘          └──────┬──────┘
     │                    │                       │                         │
     │ 11. 進入好友頁面   │                       │                         │
     ├───────────────────>│                       │                         │
     │                    │                       │                         │
     │                    │ 12. GET /api/v1/friends/requests/received       │
     │                    ├──────────────────────>│                         │
     │                    │                       │                         │
     │                    │                       │ 13. 查詢待處理請求     │
     │                    │                       ├────────────────────────>│
     │                    │                       │                         │
     │                    │ 14. 返回請求列表      │                         │
     │                    │     (含 User A)       │                         │
     │                    │<──────────────────────┤                         │
     │                    │                       │                         │
     │ 15. 顯示請求       │                       │                         │
     │    (User A 想加你) │                       │                         │
     │<───────────────────┤                       │                         │
     │                    │                       │                         │
     │ 16. 點擊「接受」   │                       │                         │
     ├───────────────────>│                       │                         │
     │                    │                       │                         │
     │                    │ 17. PUT /api/v1/friends/requests/{id}           │
     │                    ├──────────────────────>│ {action: "ACCEPT"}      │
     │                    │                       │                         │
     │                    │                       │ 18. 更新請求狀態       │
     │                    │                       ├────────────────────────>│
     │                    │                       │ UPDATE friend_requests │
     │                    │                       │ SET status = 'ACCEPTED'│
     │                    │                       │                         │
     │                    │                       │ 19. 建立雙向好友關係   │
     │                    │                       ├────────────────────────>│
     │                    │                       │ INSERT friendships     │
     │                    │                       │ (user1, user2)         │
     │                    │                       │ (user2, user1)         │
     │                    │                       │                         │
     │                    │ 20. 返回成功          │                         │
     │                    │<──────────────────────┤                         │
     │                    │                       │                         │
     │ 21. 顯示成功       │                       │                         │
     │    User A 加入     │                       │                         │
     │    好友列表        │                       │                         │
     │<───────────────────┤                       │                         │
     │                    │                       │                         │
```

### 3.2 查看好友列表與線上狀態流程

```
┌─────────┐          ┌─────────┐          ┌──────────────┐          ┌─────────────┐          ┌─────────┐
│  User   │          │  React  │          │ Spring Boot  │          │ PostgreSQL  │          │  Redis  │
│         │          │Frontend │          │   Backend    │          │             │          │         │
└────┬────┘          └────┬────┘          └──────┬───────┘          └──────┬──────┘          └────┬────┘
     │                    │                       │                         │                      │
     │ 1. 進入好友列表    │                       │                         │                      │
     ├───────────────────>│                       │                         │                      │
     │                    │                       │                         │                      │
     │                    │ 2. GET /api/v1/friends                          │                      │
     │                    ├──────────────────────>│                         │                      │
     │                    │                       │                         │                      │
     │                    │                       │ 3. 查詢好友關係         │                      │
     │                    │                       ├────────────────────────>│                      │
     │                    │                       │ SELECT * FROM           │                      │
     │                    │                       │ friendships             │                      │
     │                    │                       │ JOIN users              │                      │
     │                    │                       │                         │                      │
     │                    │                       │ 4. 返回好友資料         │                      │
     │                    │                       │<────────────────────────┤                      │
     │                    │                       │                         │                      │
     │                    │                       │ 5. 批次查詢線上狀態     │                      │
     │                    │                       ├─────────────────────────────────────────────>│
     │                    │                       │ MGET user:online:{id1}                        │
     │                    │                       │      user:online:{id2}                        │
     │                    │                       │                         │                      │
     │                    │                       │ 6. 返回線上狀態資料     │                      │
     │                    │                       │<─────────────────────────────────────────────┤
     │                    │                       │                         │                      │
     │                    │ 7. 返回好友列表        │                         │                      │
     │                    │    + 線上狀態          │                         │                      │
     │                    │<──────────────────────┤                         │                      │
     │                    │                       │                         │                      │
     │ 8. 顯示好友列表    │                       │                         │                      │
     │    (含線上/離線)   │                       │                         │                      │
     │<───────────────────┤                       │                         │                      │
     │                    │                       │                         │                      │
```

---

## 4. 資料庫架構 (PostgreSQL)

### 4.1 資料表結構

#### 4.1.1 friend_requests 表（好友請求）

**表格名稱：** `friend_requests`

**說明：** 儲存好友請求記錄

| 欄位名稱 | 資料類型 | 約束條件 | 預設值 | 說明 |
|---------|---------|---------|--------|------|
| request_id | UUID | PRIMARY KEY | uuid_generate_v4() | 請求唯一識別碼 |
| from_user_id | UUID | NOT NULL, FOREIGN KEY | - | 發送請求的使用者 ID |
| to_user_id | UUID | NOT NULL, FOREIGN KEY | - | 接收請求的使用者 ID |
| status | VARCHAR(20) | NOT NULL | 'PENDING' | 請求狀態 |
| created_at | TIMESTAMP | NOT NULL | CURRENT_TIMESTAMP | 請求建立時間 |
| updated_at | TIMESTAMP | NOT NULL | CURRENT_TIMESTAMP | 最後更新時間 |
| responded_at | TIMESTAMP | NULL | NULL | 回應時間 |

**狀態枚舉值：**
- `PENDING`: 待處理
- `ACCEPTED`: 已接受
- `REJECTED`: 已拒絕

**索引：**
- PRIMARY KEY on `request_id`
- INDEX on `from_user_id`
- INDEX on `to_user_id`
- INDEX on `status`
- UNIQUE INDEX on `(from_user_id, to_user_id, status)` WHERE `status = 'PENDING'`

**外鍵約束：**
- `from_user_id` REFERENCES `users(user_id)` ON DELETE CASCADE
- `to_user_id` REFERENCES `users(user_id)` ON DELETE CASCADE

**SQL 建立語句：**
```sql
-- 建立 friend_requests 表
CREATE TABLE friend_requests (
    request_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    from_user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    to_user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP,
    
    -- 約束條件
    CONSTRAINT valid_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    CONSTRAINT no_self_request CHECK (from_user_id != to_user_id)
);

-- 建立索引
CREATE INDEX idx_friend_requests_from_user ON friend_requests(from_user_id);
CREATE INDEX idx_friend_requests_to_user ON friend_requests(to_user_id);
CREATE INDEX idx_friend_requests_status ON friend_requests(status);

-- 唯一索引：同一對使用者只能有一個待處理請求
CREATE UNIQUE INDEX idx_friend_requests_unique_pending 
ON friend_requests(from_user_id, to_user_id) 
WHERE status = 'PENDING';

-- 自動更新 updated_at 的觸發器
CREATE TRIGGER update_friend_requests_updated_at 
    BEFORE UPDATE ON friend_requests 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();
```

#### 4.1.2 friendships 表（好友關係）

**表格名稱：** `friendships`

**說明：** 儲存已建立的好友關係（雙向）

| 欄位名稱 | 資料類型 | 約束條件 | 預設值 | 說明 |
|---------|---------|---------|--------|------|
| friendship_id | UUID | PRIMARY KEY | uuid_generate_v4() | 好友關係唯一識別碼 |
| user_id | UUID | NOT NULL, FOREIGN KEY | - | 使用者 ID |
| friend_id | UUID | NOT NULL, FOREIGN KEY | - | 好友 ID |
| created_at | TIMESTAMP | NOT NULL | CURRENT_TIMESTAMP | 好友關係建立時間 |

**索引：**
- PRIMARY KEY on `friendship_id`
- INDEX on `user_id`
- INDEX on `friend_id`
- UNIQUE INDEX on `(user_id, friend_id)`

**外鍵約束：**
- `user_id` REFERENCES `users(user_id)` ON DELETE CASCADE
- `friend_id` REFERENCES `users(user_id)` ON DELETE CASCADE

**SQL 建立語句：**
```sql
-- 建立 friendships 表
CREATE TABLE friendships (
    friendship_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    friend_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 約束條件
    CONSTRAINT no_self_friendship CHECK (user_id != friend_id)
);

-- 建立索引
CREATE INDEX idx_friendships_user_id ON friendships(user_id);
CREATE INDEX idx_friendships_friend_id ON friendships(friend_id);

-- 唯一索引：防止重複的好友關係
CREATE UNIQUE INDEX idx_friendships_unique 
ON friendships(user_id, friend_id);
```

### 4.2 資料庫關聯架構圖

```
┌─────────────────────────────────────┐
│            PostgreSQL               │
│                                     │
│  ┌───────────────────────────────┐ │
│  │          users 表              │ │
│  ├───────────────────────────────┤ │
│  │ PK  user_id (UUID)            │ │
│  │ UQ  username (VARCHAR)        │ │
│  │     password_hash (VARCHAR)   │ │
│  │     created_at (TIMESTAMP)    │ │
│  │     updated_at (TIMESTAMP)    │ │
│  │     last_login_at (TIMESTAMP) │ │
│  │     is_active (BOOLEAN)       │ │
│  │     is_online (BOOLEAN)       │ │
│  └───────┬───────────┬───────────┘ │
│          │           │              │
│          │           │              │
│    ┌─────▼──────┐ ┌─▼─────────┐   │
│    │            │ │           │    │
│  ┌─┴────────────┴─┴──────────┐│   │
│  │   friend_requests 表      ││   │
│  ├───────────────────────────┤│   │
│  │ PK  request_id (UUID)     ││   │
│  │ FK  from_user_id (UUID)   ││   │
│  │ FK  to_user_id (UUID)     ││   │
│  │     status (VARCHAR)      ││   │
│  │     created_at (TIMESTAMP)││   │
│  │     updated_at (TIMESTAMP)││   │
│  │     responded_at (TIMESTAMP)│  │
│  └───────────────────────────┘│   │
│                                │   │
│  ┌───────────────────────────┐│   │
│  │     friendships 表        ││   │
│  ├───────────────────────────┤│   │
│  │ PK  friendship_id (UUID)  ││   │
│  │ FK  user_id (UUID) ───────┼┘   │
│  │ FK  friend_id (UUID) ─────┘    │
│  │     created_at (TIMESTAMP)     │
│  └───────────────────────────┘    │
│                                     │
└─────────────────────────────────────┘

關聯說明：
1. users ←→ friend_requests: 一對多（發送的請求）
2. users ←→ friend_requests: 一對多（收到的請求）
3. users ←→ friendships: 一對多（雙向關係）
```

### 4.3 資料完整性規則

1. **好友請求規則**
  - 不能對自己發送請求：`from_user_id != to_user_id`
  - 同一對使用者只能有一個待處理請求
  - 請求狀態只能是：PENDING, ACCEPTED, REJECTED

2. **好友關係規則**
  - 雙向關係：當 A 接受 B 的請求時，建立兩筆記錄
    - (A, B)
    - (B, A)
  - 不能與自己成為好友：`user_id != friend_id`
  - 唯一性：防止重複的好友關係

3. **級聯刪除**
  - 刪除使用者時，自動刪除相關的好友請求和好友關係

---

## 5. 業務邏輯規則

### 5.1 好友請求規則

#### 發送請求前的檢查順序：
1. ✅ **驗證目標使用者存在**
  - 查詢 `users` 表確認帳號存在
  - 不存在 → 返回 404

2. ✅ **檢查是否為自己**
  - `targetUserId == currentUserId`
  - 是 → 返回 400 "不能加自己為好友"

3. ✅ **檢查是否已是好友**
  - 查詢 `friendships` 表
  - 已是好友 → 返回 409 "已經是好友關係"

4. ✅ **檢查是否已有待處理請求**
  - 查詢 `friend_requests` 表
  - 條件：`status = 'PENDING'`
  - 方向 A：`from = current, to = target`
  - 方向 B：`from = target, to = current`
  - 存在 → 返回 409 "已發送過好友請求"

5. ✅ **建立好友請求**
  - INSERT 到 `friend_requests` 表
  - 返回 201 成功

### 5.2 處理請求規則

#### 接受請求 (ACCEPT)：
1. ✅ **驗證請求存在且為 PENDING**
  - 請求不存在或已處理 → 返回 404

2. ✅ **驗證權限**
  - 只有 `to_user` 可以處理
  - 非接收者 → 返回 403

3. ✅ **更新請求狀態**
  - `status = 'ACCEPTED'`
  - `responded_at = CURRENT_TIMESTAMP`

4. ✅ **建立雙向好友關係**
  - INSERT `(from_user_id, to_user_id)` 到 friendships
  - INSERT `(to_user_id, from_user_id)` 到 friendships

5. ✅ **返回成功**
  - 包含新建立的好友關係資訊

#### 拒絕請求 (REJECT)：
1. ✅ **驗證請求存在且為 PENDING**
2. ✅ **驗證權限**
3. ✅ **更新請求狀態**
  - `status = 'REJECTED'`
  - `responded_at = CURRENT_TIMESTAMP`
4. ✅ **返回成功**
  - 不建立好友關係

### 5.3 刪除好友規則

1. ✅ **驗證好友關係存在**
  - 查詢 `friendships` 表
  - 不存在 → 返回 404

2. ✅ **驗證權限**
  - 只有關係中的任一方可以刪除
  - 非當事人 → 返回 403

3. ✅ **刪除雙向關係**
  - DELETE `(user_id, friend_id)`
  - DELETE `(friend_id, user_id)`

4. ✅ **返回成功**

### 5.4 線上狀態查詢規則

1. **資料來源優先級**
  - 優先從 Redis 查詢（快速）
  - Redis 無資料時，從 PostgreSQL 查詢
  - 結合 `users.is_online` 和 `users.last_login_at`

2. **線上狀態定義**
  - 線上：Redis 中存在 `user:online:{userId}` 且 TTL > 0
  - 離線：Redis 中無資料或 TTL 已過期

3. **最後上線時間顯示**
  - 剛剛：< 1 分鐘
  - X分鐘前：< 60 分鐘
  - X小時前：< 24 小時
  - 昨天：< 48 小時
  - X天前：< 7 天
  - 週X：< 30 天
  - 日期：> 30 天

---

## 6. 後端實作架構 (Spring Boot)

### 6.1 專案結構

```
src/main/java/com/example/chatsystem/
├── config/
│   ├── SecurityConfig.java
│   └── RedisConfig.java
├── controller/
│   └── FriendController.java           # 好友相關 API
├── service/
│   ├── FriendService.java              # 好友服務接口
│   └── impl/
│       └── FriendServiceImpl.java      # 好友服務實作
├── repository/
│   ├── FriendRequestRepository.java    # 好友請求 Repository
│   └── FriendshipRepository.java       # 好友關係 Repository
├── entity/
│   ├── User.java
│   ├── FriendRequest.java              # 好友請求實體
│   └── Friendship.java                 # 好友關係實體
├── dto/
│   ├── FriendRequestDTO.java           # 好友請求 DTO
│   ├── FriendshipDTO.java              # 好友關係 DTO
│   ├── SendFriendRequestDTO.java       # 發送請求 DTO
│   └── HandleRequestDTO.java           # 處理請求 DTO
├── exception/
│   ├── FriendRequestAlreadyExistsException.java
│   ├── AlreadyFriendsException.java
│   └── FriendRequestNotFoundException.java
├── enums/
│   └── FriendRequestStatus.java        # 請求狀態枚舉
└── util/
    └── TimeUtils.java                  # 時間格式化工具
```

### 6.2 Entity 定義

#### FriendRequest.java
```java
@Entity
@Table(name = "friend_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequest {
    
    @Id
    @GeneratedValue
    private UUID requestId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id", nullable = false)
    private User fromUser;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id", nullable = false)
    private User toUser;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendRequestStatus status = FriendRequestStatus.PENDING;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Timestamp createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private Timestamp updatedAt;
    
    private Timestamp respondedAt;
}
```

#### Friendship.java
```java
@Entity
@Table(name = "friendships")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Friendship {
    
    @Id
    @GeneratedValue
    private UUID friendshipId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "friend_id", nullable = false)
    private User friend;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Timestamp createdAt;
}
```

#### FriendRequestStatus.java (枚舉)
```java
public enum FriendRequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}
```

### 6.3 Repository 接口

```java
@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, UUID> {
    
    // 查詢待處理的好友請求
    Page<FriendRequest> findByToUserAndStatus(
        User toUser, 
        FriendRequestStatus status, 
        Pageable pageable
    );
    
    // 檢查是否存在待處理請求
    boolean existsByFromUserAndToUserAndStatus(
        User fromUser, 
        User toUser, 
        FriendRequestStatus status
    );
    
    // 查詢雙向請求
    Optional<FriendRequest> findByFromUserAndToUserOrToUserAndFromUser(
        User fromUser1, 
        User toUser1, 
        User fromUser2, 
        User toUser2
    );
}

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {
    
    // 查詢好友列表
    Page<Friendship> findByUser(User user, Pageable pageable);
    
    // 檢查是否已是好友
    boolean existsByUserAndFriend(User user, User friend);
    
    // 查詢雙向好友關係
    List<Friendship> findByUserAndFriendOrFriendAndUser(
        User user1, 
        User friend1, 
        User user2, 
        User friend2
    );
    
    // 搜尋好友
    @Query("SELECT f FROM Friendship f WHERE f.user = :user " +
           "AND LOWER(f.friend.username) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Friendship> searchFriends(
        @Param("user") User user, 
        @Param("search") String search, 
        Pageable pageable
    );
}
```

### 6.4 Service 核心方法

```java
public interface FriendService {
    
    // 發送好友請求
    FriendRequestDTO sendFriendRequest(UUID currentUserId, String targetUsername);
    
    // 查看收到的請求
    Page<FriendRequestDTO> getReceivedRequests(UUID userId, Pageable pageable);
    
    // 處理好友請求
    ApiResponse handleFriendRequest(UUID requestId, UUID currentUserId, String action);
    
    // 查看好友列表
    Page<FriendshipDTO> getFriends(UUID userId, String search, Pageable pageable);
    
    // 刪除好友
    void deleteFriend(UUID friendshipId, UUID currentUserId);
    
    // 批次查詢線上狀態
    Map<UUID, OnlineStatusDTO> getOnlineStatus(List<UUID> userIds);
}
```

---

## 7. 前端實作架構 (React)

### 7.1 專案結構

```
src/
├── components/
│   ├── friend/
│   │   ├── FriendList.jsx              # 好友列表組件
│   │   ├── FriendItem.jsx              # 好友項目組件
│   │   ├── FriendRequestItem.jsx       # 好友請求項目組件
│   │   ├── FriendDetail.jsx            # 好友詳情組件
│   │   └── AddFriendModal.jsx          # 新增好友 Modal
│   └── common/
│       ├── Avatar.jsx                  # 頭像組件
│       ├── OnlineIndicator.jsx         # 線上狀態組件
│       └── SearchBox.jsx               # 搜尋框組件
├── pages/
│   └── FriendListPage.jsx              # 好友列表頁面
├── services/
│   └── friendService.js                # 好友相關 API 服務
├── hooks/
│   ├── useFriends.js                   # 好友列表 Hook
│   ├── useFriendRequests.js            # 好友請求 Hook
│   └── useOnlineStatus.js              # 線上狀態 Hook
├── utils/
│   └── timeFormatter.js                # 時間格式化工具
└── context/
    └── FriendContext.jsx               # 好友狀態管理
```

### 7.2 API Service 範例

```javascript
// friendService.js
import axios from 'axios';

const API_BASE_URL = '/api/v1/friends';

export const friendService = {
  // 發送好友請求
  sendFriendRequest: async (targetUsername) => {
    const response = await axios.post(`${API_BASE_URL}/requests`, {
      targetUsername
    });
    return response.data;
  },

  // 查看收到的請求
  getReceivedRequests: async (page = 0, size = 20) => {
    const response = await axios.get(`${API_BASE_URL}/requests/received`, {
      params: { page, size, status: 'PENDING' }
    });
    return response.data;
  },

  // 處理好友請求
  handleFriendRequest: async (requestId, action) => {
    const response = await axios.put(
      `${API_BASE_URL}/requests/${requestId}`,
      { action }
    );
    return response.data;
  },

  // 查看好友列表
  getFriends: async (page = 0, size = 50, search = '') => {
    const response = await axios.get(API_BASE_URL, {
      params: { page, size, search }
    });
    return response.data;
  },

  // 刪除好友
  deleteFriend: async (friendshipId) => {
    const response = await axios.delete(`${API_BASE_URL}/${friendshipId}`);
    return response.data;
  },

  // 批次查詢線上狀態
  getOnlineStatus: async (userIds) => {
    const response = await axios.post(`${API_BASE_URL}/online-status`, {
      userIds
    });
    return response.data;
  }
};
```

### 7.3 Custom Hook 範例

```javascript
// useFriends.js
import { useState, useEffect } from 'react';
import { friendService } from '../services/friendService';

export const useFriends = () => {
  const [friends, setFriends] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadFriends = async (search = '') => {
    try {
      setLoading(true);
      const response = await friendService.getFriends(0, 50, search);
      setFriends(response.data.friends);
      
      // 載入線上狀態
      const userIds = response.data.friends.map(f => f.friend.userId);
      const statusResponse = await friendService.getOnlineStatus(userIds);
      
      // 合併線上狀態
      const friendsWithStatus = response.data.friends.map(friend => ({
        ...friend,
        isOnline: statusResponse.data[friend.friend.userId]?.isOnline || false,
        lastSeenAt: statusResponse.data[friend.friend.userId]?.lastSeenAt
      }));
      
      setFriends(friendsWithStatus);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadFriends();
  }, []);

  return { friends, loading, error, refreshFriends: loadFriends };
};
```

---

## 8. 錯誤處理與邊界情況

### 8.1 常見錯誤情況

| 錯誤情況 | HTTP 狀態碼 | 錯誤訊息 | 處理方式 |
|---------|-----------|---------|---------|
| 使用者不存在 | 404 | "使用者不存在" | 前端提示使用者重新輸入 |
| 加自己為好友 | 400 | "不能加自己為好友" | 前端提示錯誤 |
| 已是好友 | 409 | "已經是好友關係" | 前端顯示已是好友狀態 |
| 請求已存在 | 409 | "已發送過好友請求" | 前端提示等待對方回應 |
| 請求不存在 | 404 | "好友請求不存在或已處理" | 前端重新載入列表 |
| 無權限處理 | 403 | "無權限處理此好友請求" | 前端提示權限不足 |
| Token 無效 | 401 | "未授權，請重新登入" | 跳轉至登入頁面 |

### 8.2 邊界情況處理

#### 1. 同時發送請求
**情況：** A 向 B 發送請求，B 也同時向 A 發送請求

**處理方式：**
- 資料庫層面：UNIQUE 索引防止重複
- 應用層面：檢查雙向請求
- 建議：自動接受其中一個請求，刪除另一個

#### 2. 請求過期
**情況：** 長時間未處理的請求

**處理方式：**
- 設定過期時間（例如：30 天）
- 定期清理過期請求
- 前端顯示時過濾過期請求

#### 3. 刪除使用者
**情況：** 使用者帳號被刪除

**處理方式：**
- 資料庫：CASCADE DELETE 自動清理
- 相關好友請求自動刪除
- 相關好友關係自動刪除

#### 4. 網路錯誤
**情況：** API 請求失敗

**處理方式：**
- 前端顯示友善錯誤訊息
- 提供重試按鈕
- 使用樂觀更新 + 回滾機制

### 8.3 效能優化考量

1. **分頁載入**
  - 好友列表分頁（每頁 50 筆）
  - 請求列表分頁（每頁 20 筆）

2. **快取策略**
  - Redis 快取線上狀態（TTL: 1 小時）
  - 前端快取好友列表（定期刷新）

3. **批次查詢**
  - 批次查詢線上狀態（避免 N+1 問題）
  - 使用 JOIN 查詢減少資料庫請求

4. **索引優化**
  - 在常用查詢欄位建立索引
  - 複合索引優化查詢效能

---

## 9. 文件版本記錄

| 版本 | 日期 | 作者 | 變更說明 |
|-----|------|------|---------|
| 1.0 | 2025-09-30 | Development Team | 初版：好友系統完整文件 |

---

## 附錄

### A. 相關文件連結

- [註冊功能文件](./01-registration.md)
- [登入功能文件](./02-login.md)
- [聊天功能文件](./04-chat-function.md) - 待建立

### B. API 測試範例

#### 使用 cURL 測試發送好友請求

```bash
curl -X POST http://localhost:8080/api/v1/friends/requests \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"targetUsername": "alice_wang"}'
```

#### 使用 cURL 測試接受好友請求

```bash
curl -X PUT http://localhost:8080/api/v1/friends/requests/REQUEST_ID \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"action": "ACCEPT"}'
```

### C. 資料庫查詢範例

#### 查詢某使用者的所有好友

```sql
SELECT u.user_id, u.username, u.is_online, u.last_login_at
FROM friendships f
JOIN users u ON f.friend_id = u.user_id
WHERE f.user_id = 'YOUR_USER_ID'
ORDER BY u.username;
```

#### 查詢待處理的好友請求

```sql
SELECT fr.request_id, u.username as from_user, fr.created_at
FROM friend_requests fr
JOIN users u ON fr.from_user_id = u.user_id
WHERE fr.to_user_id = 'YOUR_USER_ID' 
  AND fr.status = 'PENDING'
ORDER BY fr.created_at DESC;
```

---

**下一步行動：**
1. 審核並確認此文件規格
2. 根據需求調整文件內容
3. 繼續撰寫「聊天功能文件」
4. 或開始好友系統的程式碼實作