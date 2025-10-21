# 簡易聊天系統開發文件 - 登入功能

**文件版本：** 1.0  
**最後更新：** 2025-09-30  
**狀態：** 已完成

---

## 目錄

1. [UI/UX 設計 - 登入頁面](#1-uiux-設計---登入頁面)
2. [後端 API 文件 - 登入功能](#2-後端-api-文件---登入功能)
3. [JWT Token 機制設計](#3-jwt-token-機制設計)
4. [Redis 線上狀態管理](#4-redis-線上狀態管理)
5. [系統架構圖 - 登入功能](#5-系統架構圖---登入功能)
6. [資料庫更新 (PostgreSQL)](#6-資料庫更新-postgresql)
7. [後端實作架構 (Spring Boot)](#7-後端實作架構-spring-boot)
8. [前端實作架構 (React)](#8-前端實作架構-react)
9. [安全性考量](#9-安全性考量)
10. [錯誤處理與測試](#10-錯誤處理與測試)
11. [效能優化](#11-效能優化)
12. [監控與日誌](#12-監控與日誌)
13. [文件版本記錄](#13-文件版本記錄)

---

## 1. UI/UX 設計 - 登入頁面

### 1.1 頁面佈局設計

**頁面名稱：** 使用者登入頁面 (User Login)

**設計規格：**

- **版面配置：** 居中卡片式設計，與註冊頁面風格一致
- **響應式設計：** 支援桌面版 (1200px+) 和行動版 (375px+)
- **色彩方案：**
  - 主色：#1a1a1a (深黑)
  - 背景：黑灰漸層
  - 卡片背景：#ffffff (白色)
  - 錯誤色：#ef4444 (紅色)
  - 成功色：#10b981 (綠色)

### 1.2 頁面元素

#### 1. Logo / 標題區域
- 應用程式名稱或 Logo
- 副標題：「歡迎回來」

#### 2. 登入表單區域

**帳號輸入欄位**
- Label: "帳號"
- Placeholder: "請輸入您的帳號"
- 自動完成：username
- 即時驗證：非空白檢查

**密碼輸入欄位**
- Label: "密碼"
- Placeholder: "請輸入您的密碼"
- 類型：password (可切換顯示/隱藏)
- 自動完成：current-password

**記住我選項**
- Checkbox: "記住我" (7天內免登入)
- 說明文字：使用此裝置時保持登入狀態

#### 3. 錯誤訊息區域
- 登入失敗訊息顯示區
- 顯示方式：卡片上方紅色警告框
- 常見錯誤訊息：
  - "帳號或密碼錯誤"
  - "此帳號已被停用"
  - "登入失敗次數過多，請稍後再試"

#### 4. 操作按鈕區域
- 登入按鈕 (主要按鈕，全寬)
- 忘記密碼連結 (次要，靠右對齊)

#### 5. 額外資訊區域
- 分隔線：「或」
- 「還沒有帳號？」+ 註冊連結

### 1.3 互動設計

- 欄位獲得焦點時顯示邊框高亮
- Enter 鍵可提交表單
- 即時驗證：必填欄位檢查
- 提交按鈕在表單未填寫完整前為禁用狀態
- 載入中狀態：顯示 spinner，禁用表單
- 登入成功後：
  - 顯示成功訊息
  - 儲存 Token 至 localStorage/sessionStorage
  - 1秒後跳轉至主頁面（好友列表）
- 登入失敗：顯示錯誤訊息，密碼欄位清空

### 1.4 頁面流程圖

```
開始
  ↓
進入登入頁面
  ↓
輸入帳號密碼
  ↓
點擊登入按鈕
  ↓
前端驗證 ─────→ 失敗 ─→ 顯示錯誤訊息
  ↓ 通過
發送登入請求
  ↓
後端驗證
  ↓
  ├─→ 成功 ─→ 儲存 Token ─→ 更新線上狀態 ─→ 跳轉主頁
  │
  └─→ 失敗 ─→ 返回錯誤 ─→ 顯示錯誤訊息 ─→ 清空密碼欄位
```

---

## 2. 後端 API 文件 - 登入功能

### 2.1 需求說明

**功能需求：**
1. 接收使用者登入資訊（帳號、密碼）
2. 驗證帳號是否存在
3. 驗證密碼是否正確（BCrypt 比對）
4. 檢查帳號狀態（是否被停用）
5. 生成 JWT Access Token 和 Refresh Token
6. 更新最後登入時間至 PostgreSQL
7. 設定使用者線上狀態至 Redis
8. 返回登入結果及 Token

**安全需求：**
- 防止暴力破解：限制登入嘗試次數（5次/15分鐘）
- 使用 HTTPS 傳輸
- Token 安全儲存
- 密碼錯誤時不透露帳號是否存在
- IP 白名單/黑名單（選配）

### 2.2 API 端點列表

#### 2.2.1 使用者登入

**端點：** `POST /api/v1/auth/login`

**請求標頭：**
```
Content-Type: application/json
```

**請求主體 (Request Body)：**
```json
{
  "username": "string",
  "password": "string",
  "rememberMe": boolean
}
```

**欄位說明：**

| 欄位名稱 | 類型 | 必填 | 說明 | 預設值 |
|---------|------|------|------|-------|
| username | String | 是 | 使用者帳號 | - |
| password | String | 是 | 使用者密碼 | - |
| rememberMe | Boolean | 否 | 是否記住登入狀態 | false |

**成功回應 (200 OK)：**
```json
{
  "success": true,
  "message": "登入成功",
  "data": {
    "user": {
      "userId": "uuid-string",
      "username": "string",
      "lastLoginAt": "2025-09-30T10:30:00Z"
    },
    "tokens": {
      "accessToken": "jwt-access-token-string",
      "refreshToken": "jwt-refresh-token-string",
      "tokenType": "Bearer",
      "expiresIn": 3600
    }
  }
}
```

**回應欄位說明：**

| 欄位路徑 | 類型 | 說明 |
|---------|------|------|
| data.user.userId | String (UUID) | 使用者唯一識別碼 |
| data.user.username | String | 使用者帳號 |
| data.user.lastLoginAt | String (ISO 8601) | 最後登入時間 |
| data.tokens.accessToken | String (JWT) | 存取權杖，有效期 1 小時 |
| data.tokens.refreshToken | String (JWT) | 刷新權杖，有效期 7 天（rememberMe=true）或 24 小時 |
| data.tokens.tokenType | String | Token 類型，固定為 "Bearer" |
| data.tokens.expiresIn | Integer | Access Token 過期時間（秒） |

**錯誤回應：**

**400 Bad Request - 驗證失敗**
```json
{
  "success": false,
  "message": "驗證失敗",
  "errors": [
    {
      "field": "username",
      "message": "帳號不可為空"
    },
    {
      "field": "password",
      "message": "密碼不可為空"
    }
  ]
}
```

**401 Unauthorized - 帳號或密碼錯誤**
```json
{
  "success": false,
  "message": "帳號或密碼錯誤"
}
```

**403 Forbidden - 帳號已被停用**
```json
{
  "success": false,
  "message": "此帳號已被停用，請聯絡管理員"
}
```

**429 Too Many Requests - 登入嘗試次數過多**
```json
{
  "success": false,
  "message": "登入失敗次數過多，請在 15 分鐘後再試",
  "retryAfter": 900
}
```

**500 Internal Server Error - 伺服器錯誤**
```json
{
  "success": false,
  "message": "伺服器錯誤，請稍後再試"
}
```

#### 2.2.2 刷新 Token

**端點：** `POST /api/v1/auth/refresh`

**用途：** 當 Access Token 過期時，使用 Refresh Token 取得新的 Access Token

**請求標頭：**
```
Content-Type: application/json
```

**請求主體：**
```json
{
  "refreshToken": "jwt-refresh-token-string"
}
```

**成功回應 (200 OK)：**
```json
{
  "success": true,
  "message": "Token 刷新成功",
  "data": {
    "accessToken": "new-jwt-access-token-string",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

**錯誤回應：**

**401 Unauthorized - Token 無效或過期**
```json
{
  "success": false,
  "message": "Refresh Token 無效或已過期，請重新登入"
}
```

#### 2.2.3 使用者登出

**端點：** `POST /api/v1/auth/logout`

**用途：** 登出使用者，清除線上狀態，使 Token 失效

**請求標頭：**
```
Authorization: Bearer {access-token}
Content-Type: application/json
```

**請求主體：** 無需 body（從 Token 中取得使用者資訊）

**成功回應 (200 OK)：**
```json
{
  "success": true,
  "message": "登出成功"
}
```

**錯誤回應：**

**401 Unauthorized - Token 無效**
```json
{
  "success": false,
  "message": "未授權，請重新登入"
}
```

#### 2.2.4 驗證 Token

**端點：** `GET /api/v1/auth/verify`

**用途：** 驗證當前 Token 是否有效（用於前端檢查登入狀態）

**請求標頭：**
```
Authorization: Bearer {access-token}
```

**成功回應 (200 OK)：**
```json
{
  "success": true,
  "valid": true,
  "data": {
    "userId": "uuid-string",
    "username": "string"
  }
}
```

**錯誤回應：**

**401 Unauthorized**
```json
{
  "success": false,
  "valid": false,
  "message": "Token 無效或已過期"
}
```

---

## 3. JWT Token 機制設計

### 3.1 Token 類型說明

#### Access Token（存取權杖）
- **用途：** 用於存取受保護的 API 資源
- **有效期：** 1 小時（3600 秒）
- **儲存位置：**
  - 記住我 = true：localStorage
  - 記住我 = false：sessionStorage（僅瀏覽器關閉前有效）
- **刷新機制：** 透過 Refresh Token 刷新

#### Refresh Token（刷新權杖）
- **用途：** 用於取得新的 Access Token
- **有效期：**
  - 記住我 = true：7 天
  - 記住我 = false：24 小時
- **儲存位置：** localStorage（HttpOnly Cookie 為更安全選擇）
- **安全性：** 單次使用後失效，需重新取得

### 3.2 JWT Payload 結構

#### Access Token Payload
```json
{
  "sub": "user-uuid",              // Subject: 使用者 ID
  "username": "john_doe",          // 使用者帳號
  "iat": 1696067400,               // Issued At: 發行時間
  "exp": 1696071000,               // Expiration: 過期時間
  "type": "access"                 // Token 類型
}
```

#### Refresh Token Payload
```json
{
  "sub": "user-uuid",              // Subject: 使用者 ID
  "iat": 1696067400,               // Issued At: 發行時間
  "exp": 1696671000,               // Expiration: 過期時間（7天後）
  "type": "refresh",               // Token 類型
  "jti": "token-uuid"              // JWT ID: Token 唯一識別碼
}
```

### 3.3 JWT 密鑰配置

```yaml
# application.yml
jwt:
  secret: ${JWT_SECRET}                    # 從環境變數讀取，至少 256 位元
  access-token-validity: 3600              # Access Token 有效期（秒）
  refresh-token-validity: 604800           # Refresh Token 有效期（秒，7天）
  refresh-token-validity-short: 86400      # 未勾選記住我（秒，24小時）
```

**安全建議：**
- JWT Secret 必須儲存在 Kubernetes Secret 中
- Secret 長度至少 256 位元（32 字元）
- 定期輪換密鑰（建議每季）
- 生產環境使用 RS256 (非對稱加密) 更安全

### 3.4 Token 刷新流程

```
前端偵測 Access Token 即將過期（剩餘 5 分鐘）
  ↓
使用 Refresh Token 呼叫 /api/v1/auth/refresh
  ↓
後端驗證 Refresh Token
  ├─→ 有效
  │     ↓
  │   生成新的 Access Token
  │     ↓
  │   （選配）生成新的 Refresh Token（輪換機制）
  │     ↓
  │   返回新 Token
  │     ↓
  │   前端更新儲存
  │
  └─→ 無效/過期
        ↓
      返回 401 錯誤
        ↓
      前端清除所有 Token
        ↓
      跳轉至登入頁面
```

---

## 4. Redis 線上狀態管理

### 4.1 資料結構設計

#### 使用者線上狀態 (String)

**Key 格式：** `user:online:{userId}`  
**Value：** JSON 格式的狀態資訊  
**TTL：** 1 小時（隨心跳更新）

**Value 結構：**
```json
{
  "userId": "uuid-string",
  "username": "john_doe",
  "lastActiveAt": "2025-09-30T10:30:00Z",
  "deviceInfo": "Chrome/120.0 (Windows)",
  "ipAddress": "192.168.1.100"
}
```

**Redis 指令範例：**
```redis
# 設定使用者上線
SET user:online:550e8400-e29b-41d4-a716-446655440000 '{"userId":"550e8400-e29b-41d4-a716-446655440000","username":"john_doe","lastActiveAt":"2025-09-30T10:30:00Z"}' EX 3600

# 檢查使用者是否在線
EXISTS user:online:550e8400-e29b-41d4-a716-446655440000

# 取得使用者狀態
GET user:online:550e8400-e29b-41d4-a716-446655440000

# 刪除線上狀態（登出）
DEL user:online:550e8400-e29b-41d4-a716-446655440000
```

#### 登入失敗計數器 (String)

**Key 格式：** `login:attempt:{username}`  
**Value：** 失敗次數（整數）  
**TTL：** 15 分鐘

**Redis 指令範例：**
```redis
# 增加失敗次數
INCR login:attempt:john_doe
EXPIRE login:attempt:john_doe 900

# 檢查失敗次數
GET login:attempt:john_doe

# 登入成功後清除
DEL login:attempt:john_doe
```

#### Token 黑名單 (String)

**Key 格式：** `token:blacklist:{jti}`  
**Value：** Token 資訊  
**TTL：** Token 的剩餘有效期

**用途：** 登出時將 Token 加入黑名單，防止被重複使用

```redis
# 加入黑名單
SETEX token:blacklist:abc123-token-id "revoked" 3600

# 檢查是否在黑名單
EXISTS token:blacklist:abc123-token-id
```

### 4.2 心跳機制

**目的：** 保持使用者線上狀態更新

**實作方式：**
- 前端每 5 分鐘發送一次心跳請求
- 後端更新 Redis 中的 `lastActiveAt` 時間
- 重設 TTL 為 1 小時

**API 端點：** `POST /api/v1/users/heartbeat`

**請求標頭：**
```
Authorization: Bearer {access-token}
```

**成功回應：**
```json
{
  "success": true,
  "message": "心跳更新成功"
}
```

### 4.3 線上狀態查詢

**批次查詢好友線上狀態**

**端點：** `POST /api/v1/users/online-status`

**請求主體：**
```json
{
  "userIds": ["uuid-1", "uuid-2", "uuid-3"]
}
```

**成功回應：**
```json
{
  "success": true,
  "data": {
    "uuid-1": {
      "isOnline": true,
      "lastActiveAt": "2025-09-30T10:30:00Z"
    },
    "uuid-2": {
      "isOnline": false,
      "lastActiveAt": "2025-09-30T08:15:00Z"
    },
    "uuid-3": {
      "isOnline": true,
      "lastActiveAt": "2025-09-30T10:28:00Z"
    }
  }
}
```

---

## 5. 系統架構圖 - 登入功能

### 5.1 登入流程架構圖

```
┌─────────┐         ┌─────────┐         ┌──────────────┐         ┌─────────────┐
│ Browser │         │  React  │         │ Spring Boot  │         │ PostgreSQL  │
│  (User) │         │Frontend │         │   Backend    │         │             │
└────┬────┘         └────┬────┘         └──────┬───────┘         └──────┬──────┘
     │                   │                      │                        │
     │ 1. 輸入帳號密碼   │                      │                        │
     ├──────────────────>│                      │                        │
     │                   │                      │                        │
     │                   │ 2. 前端驗證          │                        │
     │                   │   (必填欄位)         │                        │
     │                   │                      │                        │
     │                   │ 3. POST /api/v1/auth/login                    │
     │                   ├─────────────────────>│                        │
     │                   │                      │                        │
     │                   │                      │ 4. 檢查登入嘗試次數   │
     │                   │                      │    ↓                   │
     │                   │                  ┌───┴────────┐               │
     │                   │                  │   Redis    │               │
     │                   │                  │ 查詢計數器 │               │
     │                   │                  └───┬────────┘               │
     │                   │                      │                        │
     │                   │                      │ 5. 查詢使用者資料     │
     │                   │                      ├───────────────────────>│
     │                   │                      │                        │
     │                   │                      │ 6. 返回使用者資料     │
     │                   │                      │<───────────────────────┤
     │                   │                      │                        │
     │                   │                      │ 7. 驗證密碼 (BCrypt)  │
     │                   │                      │                        │
     │                   │                      │ 8. 更新 last_login_at │
     │                   │                      ├───────────────────────>│
     │                   │                      │                        │
     │                   │                      │ 9. 生成 JWT Tokens    │
     │                   │                      │                        │
     │                   │                      │ 10. 設定線上狀態      │
     │                   │                      │    ↓                   │
     │                   │                  ┌───┴────────┐               │
     │                   │                  │   Redis    │               │
     │                   │                  │ 設定 online│               │
     │                   │                  └───┬────────┘               │
     │                   │                      │                        │
     │                   │ 11. 返回登入成功    │                        │
     │                   │     + Tokens         │                        │
     │                   │<─────────────────────┤                        │
     │                   │                      │                        │
     │                   │ 12. 儲存 Tokens     │                        │
     │                   │     到 Storage       │                        │
     │                   │                      │                        │
     │ 13. 跳轉主頁面    │                      │                        │
     │<──────────────────┤                      │                        │
     │                   │                      │                        │
```

### 5.2 Token 驗證流程

```
前端發送 API 請求（帶 Access Token）
  ↓
Spring Security Filter 攔截
  ↓
提取 Authorization Header 中的 Token
  ↓
驗證 Token 簽章
  ├─→ 簽章無效 ─→ 返回 401 Unauthorized
  │
  └─→ 簽章有效
        ↓
      解析 Token Payload
        ↓
      檢查過期時間
        ├─→ 已過期 ─→ 返回 401 Unauthorized（提示刷新 Token）
        │
        └─→ 未過期
              ↓
            檢查 Token 黑名單（Redis）
              ├─→ 在黑名單 ─→ 返回 401 Unauthorized
              │
              └─→ 不在黑名單
                    ↓
                  從 Token 取得 userId
                    ↓
                  查詢使用者資訊（PostgreSQL）
                    ↓
                  設定 SecurityContext
                    ↓
                  繼續處理請求
```

---

## 6. 資料庫更新 (PostgreSQL)

### 6.1 users 表更新

登入功能需要更新 `last_login_at` 欄位，此欄位已在註冊功能文件中定義。

**更新 SQL 範例：**
```sql
-- 登入成功時更新最後登入時間
UPDATE users 
SET last_login_at = CURRENT_TIMESTAMP,
    is_online = TRUE
WHERE user_id = ?;
```

### 6.2 新增資料表：refresh_tokens

**用途：** 儲存已發行的 Refresh Token，用於驗證和撤銷

**表格名稱：** `refresh_tokens`

| 欄位名稱 | 資料類型 | 約束條件 | 預設值 | 說明 |
|---------|---------|---------|--------|------|
| token_id | UUID | PRIMARY KEY | uuid_generate_v4() | Token 唯一識別碼 |
| user_id | UUID | NOT NULL, FOREIGN KEY | - | 使用者 ID，外鍵關聯 users.user_id |
| token_hash | VARCHAR(255) | NOT NULL | - | Token 的 Hash 值（不儲存原始 Token） |
| expires_at | TIMESTAMP | NOT NULL | - | Token 過期時間 |
| created_at | TIMESTAMP | NOT NULL | CURRENT_TIMESTAMP | Token 建立時間 |
| revoked_at | TIMESTAMP | NULL | NULL | Token 撤銷時間（登出或刷新時設定） |
| device_info | VARCHAR(255) | NULL | NULL | 裝置資訊 |
| ip_address | VARCHAR(45) | NULL | NULL | IP 位址 |

**SQL 建立語句：**
```sql
-- 建立 refresh_tokens 表
CREATE TABLE refresh_tokens (
    token_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP,
    device_info VARCHAR(255),
    ip_address VARCHAR(45),
    
    -- 約束條件
    CONSTRAINT valid_expiry CHECK (expires_at > created_at)
);

-- 建立索引
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- 定期清理過期 Token 的函數（可選）
CREATE OR REPLACE FUNCTION cleanup_expired_tokens()
RETURNS void AS $$
BEGIN
    DELETE FROM refresh_tokens 
    WHERE expires_at < CURRENT_TIMESTAMP 
       OR revoked_at IS NOT NULL;
END;
$$ LANGUAGE plpgsql;

-- 建立定時任務（需要 pg_cron 擴展）
-- SELECT cron.schedule('cleanup-tokens', '0 2 * * *', 'SELECT cleanup_expired_tokens()');
```

### 6.3 更新後的 ER 圖

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
│  └───────────┬───────────────────┘ │
│              │                      │
│              │ 1                    │
│              │                      │
│              │ N                    │
│              ▼                      │
│  ┌───────────────────────────────┐ │
│  │    refresh_tokens 表          │ │
│  ├───────────────────────────────┤ │
│  │ PK  token_id (UUID)           │ │
│  │ FK  user_id (UUID)            │ │
│  │ UQ  token_hash (VARCHAR)      │ │
│  │     expires_at (TIMESTAMP)    │ │
│  │     created_at (TIMESTAMP)    │ │
│  │     revoked_at (TIMESTAMP)    │ │
│  │     device_info (VARCHAR)     │ │
│  │     ip_address (VARCHAR)      │ │
│  └───────────────────────────────┘ │
│                                     │
└─────────────────────────────────────┘

關聯說明：
1. users ←→ refresh_tokens: 一對多
2. 刪除使用者時，自動刪除相關的 Refresh Tokens (CASCADE)
```

---

## 7. 後端實作架構 (Spring Boot)

### 7.1 專案結構

```
src/main/java/com/example/chatsystem/
├── config/
│   ├── SecurityConfig.java             # Spring Security 配置
│   ├── JwtConfig.java                  # JWT 配置
│   └── RedisConfig.java                # Redis 配置
├── controller/
│   └── AuthController.java             # 認證相關 API
├── service/
│   ├── AuthService.java                # 認證服務接口
│   ├── TokenService.java               # Token 服務接口
│   └── impl/
│       ├── AuthServiceImpl.java        # 認證服務實作
│       └── TokenServiceImpl.java       # Token 服務實作
├── repository/
│   ├── UserRepository.java             # User Repository
│   └── RefreshTokenRepository.java     # Refresh Token Repository
├── entity/
│   ├── User.java                       # 使用者實體
│   └── RefreshToken.java               # Refresh Token 實體
├── dto/
│   ├── LoginRequest.java               # 登入請求 DTO
│   ├── LoginResponse.java              # 登入回應 DTO
│   ├── RefreshTokenRequest.java        # 刷新 Token 請求 DTO
│   └── TokenResponse.java              # Token 回應 DTO
├── security/
│   ├── JwtTokenProvider.java           # JWT Token 生成與驗證
│   ├── JwtAuthenticationFilter.java    # JWT 認證過濾器
│   └── CustomUserDetailsService.java   # 使用者詳情服務
├── exception/
│   ├── InvalidCredentialsException.java
│   ├── TokenExpiredException.java
│   └── AccountDisabledException.java
└── util/
    ├── IpAddressUtil.java              # IP 位址工具
    └── DeviceInfoUtil.java             # 裝置資訊工具
```

### 7.2 核心配置

#### SecurityConfig.java
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/login", "/api/v1/auth/register", 
                                "/api/v1/auth/refresh").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
    
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
```

#### JwtConfig.java
```java
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtConfig {
    
    private String secret;
    private long accessTokenValidity;        // 3600 秒（1小時）
    private long refreshTokenValidity;       // 604800 秒（7天）
    private long refreshTokenValidityShort;  // 86400 秒（24小時）
    
    @PostConstruct
    public void init() {
        // 驗證配置
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 characters");
        }
    }
}
```

### 7.3 JWT Token Provider

```java
@Component
public class JwtTokenProvider {
    
    @Autowired
    private JwtConfig jwtConfig;
    
    private Key getSigningKey() {
        byte[] keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    // 生成 Access Token
    public String generateAccessToken(UUID userId, String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getAccessTokenValidity() * 1000);
        
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("username", username)
                .claim("type", "access")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    // 生成 Refresh Token
    public String generateRefreshToken(UUID userId, boolean rememberMe) {
        Date now = new Date();
        long validity = rememberMe ? 
            jwtConfig.getRefreshTokenValidity() : 
            jwtConfig.getRefreshTokenValidityShort();
        Date expiryDate = new Date(now.getTime() + validity * 1000);
        
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("type", "refresh")
                .setId(UUID.randomUUID().toString())  // jti
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    // 驗證 Token
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    // 從 Token 取得 User ID
    public UUID getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        return UUID.fromString(claims.getSubject());
    }
    
    // 從 Token 取得 JTI（Refresh Token 用）
    public String getJtiFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        return claims.getId();
    }
    
    // 檢查 Token 是否即將過期（剩餘 5 分鐘）
    public boolean isTokenExpiringSoon(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        Date expiration = claims.getExpiration();
        long timeUntilExpiry = expiration.getTime() - System.currentTimeMillis();
        
        return timeUntilExpiry < 5 * 60 * 1000; // 5 分鐘
    }
}
```

### 7.4 認證服務實作（部分代碼）

```java
@Service
public class AuthServiceImpl implements AuthService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Value("${login.max-attempts:5}")
    private int maxLoginAttempts;
    
    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        String username = request.getUsername();
        
        // 1. 檢查登入失敗次數
        if (isLoginBlocked(username)) {
            throw new TooManyAttemptsException("登入失敗次數過多，請在 15 分鐘後再試");
        }
        
        // 2. 查詢使用者
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("帳號或密碼錯誤"));
        
        // 3. 驗證密碼
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            incrementLoginAttempts(username);
            throw new InvalidCredentialsException("帳號或密碼錯誤");
        }
        
        // 4. 檢查帳號狀態
        if (!user.getIsActive()) {
            throw new AccountDisabledException("此帳號已被停用，請聯絡管理員");
        }
        
        // 5. 清除登入失敗計數
        clearLoginAttempts(username);
        
        // 6. 生成 Tokens
        String accessToken = jwtTokenProvider.generateAccessToken(
            user.getUserId(), 
            user.getUsername()
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(
            user.getUserId(), 
            request.isRememberMe()
        );
        
        // 7. 儲存 Refresh Token
        saveRefreshToken(user, refreshToken, ipAddress, userAgent);
        
        // 8. 更新最後登入時間
        user.setLastLoginAt(Timestamp.from(Instant.now()));
        user.setIsOnline(true);
        userRepository.save(user);
        
        // 9. 設定 Redis 線上狀態
        setUserOnline(user.getUserId(), ipAddress, userAgent);
        
        // 10. 返回結果
        return LoginResponse.builder()
                .user(UserDTO.from(user))
                .tokens(TokenResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(3600)
                    .build())
                .build();
    }
    
    // 私有輔助方法
    private boolean isLoginBlocked(String username) {
        String key = "login:attempt:" + username;
        String attempts = redisTemplate.opsForValue().get(key);
        return attempts != null && Integer.parseInt(attempts) >= maxLoginAttempts;
    }
    
    private void incrementLoginAttempts(String username) {
        String key = "login:attempt:" + username;
        Long count = redisTemplate.opsForValue().increment(key);
        
        if (count == 1) {
            redisTemplate.expire(key, 15, TimeUnit.MINUTES);
        }
    }
    
    private void clearLoginAttempts(String username) {
        String key = "login:attempt:" + username;
        redisTemplate.delete(key);
    }
    
    private void setUserOnline(UUID userId, String ip, String device) {
        String key = "user:online:" + userId;
        Map<String, String> data = Map.of(
            "userId", userId.toString(),
            "lastActiveAt", Instant.now().toString(),
            "ipAddress", ip,
            "deviceInfo", device
        );
        
        redisTemplate.opsForValue().set(key, toJson(data), 1, TimeUnit.HOURS);
    }
}
```

---

## 8. 前端實作架構 (React)

### 8.1 專案結構

```
src/
├── components/
│   ├── auth/
│   │   ├── LoginForm.jsx               # 登入表單組件
│   │   └── ProtectedRoute.jsx          # 受保護路由組件
│   └── common/
│       └── LoadingSpinner.jsx
├── pages/
│   ├── LoginPage.jsx                   # 登入頁面
│   └── HomePage.jsx                    # 主頁（登入後）
├── services/
│   ├── authService.js                  # 認證服務
│   └── tokenService.js                 # Token 管理服務
├── hooks/
│   ├── useAuth.js                      # 認證 Hook
│   └── useTokenRefresh.js              # Token 自動刷新 Hook
├── context/
│   └── AuthContext.jsx                 # 認證狀態管理
├── utils/
│   ├── httpClient.js                   # HTTP 客戶端（含攔截器）
│   └── storage.js                      # 儲存管理
└── App.jsx
```

### 8.2 認證服務

```javascript
// authService.js
import httpClient from '../utils/httpClient';

const API_BASE_URL = '/api/v1/auth';

export const authService = {
  // 登入
  login: async (username, password, rememberMe = false) => {
    const response = await httpClient.post(`${API_BASE_URL}/login`, {
      username,
      password,
      rememberMe
    });
    return response.data;
  },

  // 登出
  logout: async () => {
    const response = await httpClient.post(`${API_BASE_URL}/logout`);
    return response.data;
  },

  // 刷新 Token
  refreshToken: async (refreshToken) => {
    const response = await httpClient.post(`${API_BASE_URL}/refresh`, {
      refreshToken
    });
    return response.data;
  },

  // 驗證 Token
  verifyToken: async () => {
    const response = await httpClient.get(`${API_BASE_URL}/verify`);
    return response.data;
  }
};
```

### 8.3 Token 管理服務

```javascript
// tokenService.js
class TokenService {
  constructor() {
    this.storageKey = {
      accessToken: 'accessToken',
      refreshToken: 'refreshToken',
      user: 'user'
    };
  }

  // 儲存 Tokens
  saveTokens(accessToken, refreshToken, rememberMe = false) {
    const storage = rememberMe ? localStorage : sessionStorage;
    
    storage.setItem(this.storageKey.accessToken, accessToken);
    storage.setItem(this.storageKey.refreshToken, refreshToken);
  }

  // 取得 Access Token
  getAccessToken() {
    return localStorage.getItem(this.storageKey.accessToken) ||
           sessionStorage.getItem(this.storageKey.accessToken);
  }

  // 取得 Refresh Token
  getRefreshToken() {
    return localStorage.getItem(this.storageKey.refreshToken) ||
           sessionStorage.getItem(this.storageKey.refreshToken);
  }

  // 清除所有資料
  clearAll() {
    Object.values(this.storageKey).forEach(key => {
      localStorage.removeItem(key);
      sessionStorage.removeItem(key);
    });
  }

  // 檢查是否已登入
  isAuthenticated() {
    return this.getAccessToken() !== null;
  }
}

export default new TokenService();
```

### 8.4 HTTP 攔截器（自動刷新 Token）

```javascript
// httpClient.js
import axios from 'axios';
import tokenService from './tokenService';
import { authService } from '../services/authService';

const httpClient = axios.create({
  baseURL: 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json'
  }
});

// 請求攔截器：自動添加 Token
httpClient.interceptors.request.use(
  (config) => {
    const token = tokenService.getAccessToken();
    
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 回應攔截器：處理 Token 過期
let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  
  failedQueue = [];
};

httpClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    
    // Token 過期，嘗試刷新
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // 正在刷新中，將請求加入佇列
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(token => {
          originalRequest.headers.Authorization = `Bearer ${token}`;
          return httpClient(originalRequest);
        });
      }
      
      originalRequest._retry = true;
      isRefreshing = true;
      
      const refreshToken = tokenService.getRefreshToken();
      
      if (!refreshToken) {
        tokenService.clearAll();
        window.location.href = '/login';
        return Promise.reject(error);
      }
      
      try {
        const response = await authService.refreshToken(refreshToken);
        const { accessToken } = response.data;
        
        tokenService.saveTokens(accessToken, refreshToken);
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        
        processQueue(null, accessToken);
        
        return httpClient(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        tokenService.clearAll();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }
    
    return Promise.reject(error);
  }
);

export default httpClient;
```

---

## 9. 安全性考量

### 9.1 密碼安全

#### BCrypt 加密
```java
// 使用 BCrypt 加密密碼，強度為 10
PasswordEncoder encoder = new BCryptPasswordEncoder(10);
String hashedPassword = encoder.encode(plainPassword);

// 驗證密碼
boolean matches = encoder.matches(plainPassword, hashedPassword);
```

**優點：**
- 自動加鹽（Salt）
- 計算成本可調整
- 抗彩虹表攻擊

### 9.2 Token 安全

#### Token 黑名單機制
```java
private void addTokenToBlacklist(String jti, long ttlSeconds) {
    String key = "token:blacklist:" + jti;
    redisTemplate.opsForValue().set(key, "revoked", ttlSeconds, TimeUnit.SECONDS);
}
```

#### Token 輪換（Rotation）
- Refresh Token 使用後立即失效
- 生成新的 Refresh Token
- 提高安全性

### 9.3 防止暴力破解

#### 登入失敗次數限制
```java
private void incrementLoginAttempts(String username) {
    String key = "login:attempt:" + username;
    Long count = redisTemplate.opsForValue().increment(key);
    
    if (count == 1) {
        redisTemplate.expire(key, 15, TimeUnit.MINUTES);
    }
}
```

**規則：**
- 5 次失敗後鎖定 15 分鐘
- 使用 Redis 計數器
- 登入成功後清除計數

---

## 10. 錯誤處理與測試

### 10.1 全局異常處理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse> handleInvalidCredentials(
            InvalidCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(TooManyAttemptsException.class)
    public ResponseEntity<ApiResponse> handleTooManyAttempts(
            TooManyAttemptsException ex) {
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.builder()
                    .success(false)
                    .message(ex.getMessage())
                    .data(Map.of("retryAfter", 900))
                    .build());
    }
}
```

### 10.2 單元測試範例

```java
@SpringBootTest
public class AuthServiceTest {
    
    @Autowired
    private AuthService authService;
    
    @MockBean
    private UserRepository userRepository;
    
    @MockBean
    private PasswordEncoder passwordEncoder;
    
    @Test
    public void testLoginSuccess() {
        // Given
        String username = "testuser";
        String password = "Test@1234";
        
        User user = User.builder()
                .userId(UUID.randomUUID())
                .username(username)
                .passwordHash("hashed-password")
                .isActive(true)
                .build();
        
        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, user.getPasswordHash()))
                .thenReturn(true);
        
        // When
        LoginRequest request = new LoginRequest(username, password, false);
        LoginResponse response = authService.login(request, "127.0.0.1", "Test");
        
        // Then
        assertNotNull(response);
        assertNotNull(response.getTokens().getAccessToken());
        assertEquals(username, response.getUser().getUsername());
    }
}
```

---

## 11. 效能優化

### 11.1 Redis 快取優化

#### Token 驗證快取
```java
public boolean isTokenValid(String token) {
    String key = "token:valid:" + DigestUtils.sha256Hex(token);
    Boolean cached = redisTemplate.opsForValue().get(key);
    
    if (cached != null) {
        return cached;
    }
    
    boolean isValid = jwtTokenProvider.validateToken(token);
    redisTemplate.opsForValue().set(key, isValid, 5, TimeUnit.MINUTES);
    
    return isValid;
}
```

### 11.2 資料庫優化

#### 定期清理過期 Token
```java
@Scheduled(cron = "0 0 2 * * ?")
public void cleanupExpiredTokens() {
    Timestamp now = Timestamp.from(Instant.now());
    refreshTokenRepository.deleteByExpiresAtBeforeOrRevokedAtIsNotNull(now);
    log.info("Cleaned up expired refresh tokens");
}
```

---

## 12. 監控與日誌

### 12.1 登入日誌

```java
public void logLoginAttempt(
        String username, 
        boolean success, 
        String ipAddress,
        String failureReason) {
    
    if (success) {
        log.info("Login success: username={}, ip={}", username, ipAddress);
    } else {
        log.warn("Login failed: username={}, ip={}, reason={}", 
                username, ipAddress, failureReason);
    }
}
```

### 12.2 Prometheus Metrics

```java
private final Counter loginAttempts = Counter.build()
        .name("auth_login_attempts_total")
        .help("Total login attempts")
        .labelNames("status")
        .register();

public void recordLoginAttempt(boolean success) {
    loginAttempts.labels(success ? "success" : "failure").inc();
}
```

---

## 13. 文件版本記錄

| 版本 | 日期 | 作者 | 變更說明 |
|-----|------|------|---------|
| 1.0 | 2025-09-30 | Development Team | 完整版：登入功能文件 |

---

## 附錄

### A. 相關文件連結

- [註冊功能文件](./01-registration.md)
- [好友系統文件](./03-friend-system.md)
- [聊天功能文件](./04-chat-function.md)

### B. API 測試範例

```bash
# 登入
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "Test@1234",
    "rememberMe": true
  }'

# 刷新 Token
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "YOUR_REFRESH_TOKEN"
  }'

# 登出
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### C. 安全檢查清單

- [ ] 密碼使用 BCrypt 加密
- [ ] JWT Secret 長度 ≥ 32 字元
- [ ] 強制使用 HTTPS
- [ ] 啟用登入失敗次數限制
- [ ] Token 黑名單機制已實作
- [ ] Refresh Token 儲存於資料庫
- [ ] 設定 CORS 白名單
- [ ] 敏感資訊不寫入日誌
- [ ] 定期清理過期 Token
- [ ] 監控異常登入行為

---

**文件完成！** ✅