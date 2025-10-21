# 簡易聊天系統開發文件 - 註冊功能

**文件版本：** 1.0  
**最後更新：** 2025-09-30  
**狀態：** 待審核

---

## 目錄

1. [UI/UX 設計 - 註冊頁面](#1-uiux-設計---註冊頁面)
2. [後端 API 文件 - 註冊功能](#2-後端-api-文件---註冊功能)
3. [系統架構圖 - 註冊功能](#3-系統架構圖---註冊功能)
4. [資料庫架構 - 使用者資料](#4-資料庫架構---使用者資料-postgresql)
5. [後端實作架構 (Spring Boot)](#5-後端實作架構-spring-boot)
6. [前端實作架構 (React)](#6-前端實作架構-react)
7. [開發優先順序與後續規劃](#7-開發優先順序與後續規劃)
8. [文件版本記錄](#8-文件版本記錄)

---

## 1. UI/UX 設計 - 註冊頁面

### 1.1 頁面佈局設計

**頁面名稱：** 使用者註冊頁面 (User Registration)

**設計規格：**

- **版面配置：** 居中卡片式設計，簡潔明瞭
- **響應式設計：** 支援桌面版 (1200px+) 和行動版 (375px+)
- **色彩方案：**
  - 主色：#4A90E2 (藍色)
  - 次要色：#F5F7FA (淺灰背景)
  - 錯誤色：#E74C3C (紅色)
  - 成功色：#27AE60 (綠色)

### 1.2 頁面元素

#### 1. Logo / 標題區域
- 應用程式名稱或 Logo
- 副標題：「創建您的帳號」

#### 2. 註冊表單區域

**帳號輸入欄位**
- Label: "帳號"
- Placeholder: "請輸入帳號 (4-20個字元)"
- 即時驗證提示

**密碼輸入欄位**
- Label: "密碼"
- Placeholder: "請輸入密碼"
- 類型：password (可切換顯示/隱藏)
- 密碼強度指示器
- 即時驗證規則顯示

**確認密碼欄位**
- Label: "確認密碼"
- Placeholder: "請再次輸入密碼"
- 即時比對提示

#### 3. 密碼規則提示區域
- ✓ 至少 8 位數
- ✓ 包含至少一個大寫字母 (A-Z)
- ✓ 包含至少一個小寫字母 (a-z)
- ✓ 包含至少一個數字 (0-9)
- ✓ 包含至少一個符號 (!@#$%^&*...)

#### 4. 操作按鈕區域
- 註冊按鈕 (主要按鈕，全寬)
- 返回登入連結

#### 5. 額外資訊區域
- 「已有帳號？」+ 登入連結

### 1.3 互動設計

- 欄位獲得焦點時顯示邊框高亮
- 即時驗證，輸入時顯示錯誤/成功狀態
- 密碼規則項目依符合狀態變色 (灰色→綠色)
- 提交按鈕在表單驗證通過前為禁用狀態
- 載入中狀態：顯示 spinner，禁用表單
- 成功註冊後顯示成功訊息，3秒後跳轉至登入頁面

---

## 2. 後端 API 文件 - 註冊功能

### 2.1 需求說明

**功能需求：**
1. 接收使用者註冊資訊（帳號、密碼）
2. 驗證帳號唯一性（不可重複）
3. 驗證密碼複雜度規則
4. 密碼加密儲存（使用 BCrypt）
5. 建立新使用者記錄於 PostgreSQL
6. 返回註冊結果

**安全需求：**
- 密碼必須加密儲存，不可明文
- 防止 SQL Injection
- 限制請求頻率（防止暴力註冊攻擊）
- HTTPS 傳輸加密

### 2.2 API 端點列表

#### 2.2.1 註冊新使用者

**端點：** `POST /api/v1/auth/register`

**請求標頭：**
```
Content-Type: application/json
```

**請求主體 (Request Body)：**
```json
{
  "username": "string",
  "password": "string",
  "confirmPassword": "string"
}
```

**欄位說明：**

| 欄位名稱 | 類型 | 必填 | 說明 | 驗證規則 |
|---------|------|------|------|---------|
| username | String | 是 | 使用者帳號 | 4-20字元，只允許英文字母、數字、底線 |
| password | String | 是 | 使用者密碼 | 至少8位數，包含大小寫字母、數字、符號各一 |
| confirmPassword | String | 是 | 確認密碼 | 必須與 password 相同 |

**密碼驗證正規表達式：**
```regex
^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$
```

**成功回應 (201 Created)：**
```json
{
  "success": true,
  "message": "註冊成功",
  "data": {
    "userId": "uuid-string",
    "username": "string",
    "createdAt": "2025-09-30T10:30:00Z"
  }
}
```

**錯誤回應：**

**400 Bad Request - 驗證失敗**
```json
{
  "success": false,
  "message": "驗證失敗",
  "errors": [
    {
      "field": "username",
      "message": "帳號長度必須在 4-20 字元之間"
    },
    {
      "field": "password",
      "message": "密碼必須包含大小寫字母、數字和符號各至少一個"
    }
  ]
}
```

**409 Conflict - 帳號已存在**
```json
{
  "success": false,
  "message": "此帳號已被註冊",
  "errors": [
    {
      "field": "username",
      "message": "此帳號已存在，請使用其他帳號"
    }
  ]
}
```

**500 Internal Server Error - 伺服器錯誤**
```json
{
  "success": false,
  "message": "伺服器錯誤，請稍後再試"
}
```

#### 2.2.2 檢查帳號可用性

**端點：** `GET /api/v1/auth/check-username?username={username}`

**用途：** 讓前端在使用者輸入時即時檢查帳號是否已被使用

**查詢參數：**

| 參數名稱 | 類型 | 必填 | 說明 |
|---------|------|------|------|
| username | String | 是 | 要檢查的帳號 |

**成功回應 (200 OK)：**
```json
{
  "success": true,
  "available": true,
  "message": "此帳號可以使用"
}
```

或

```json
{
  "success": true,
  "available": false,
  "message": "此帳號已被使用"
}
```

---

## 3. 系統架構圖 - 註冊功能

### 3.1 整體服務架構

```
┌─────────────────────────────────────────────────────────────────┐
│                          Kubernetes Cluster                      │
│                                                                   │
│  ┌──────────────┐         ┌──────────────┐                      │
│  │   Ingress    │────────>│ Load Balancer│                      │
│  │  Controller  │         └──────┬───────┘                      │
│  └──────────────┘                │                               │
│                                   │                               │
│         ┌─────────────────────────┴──────────────────┐          │
│         │                                              │          │
│         ▼                                              ▼          │
│  ┌──────────────┐                            ┌──────────────┐   │
│  │   Frontend   │                            │   Backend    │   │
│  │  React App   │                            │  Spring Boot │   │
│  │   (Pod 1-3)  │◄───────────────────────────│   (Pod 1-5)  │   │
│  └──────────────┘                            └──────┬───────┘   │
│                                                      │           │
│                                    ┌─────────────────┼─────┐    │
│                                    │                 │     │    │
│                                    ▼                 ▼     ▼    │
│                            ┌────────────┐   ┌──────────┐  │    │
│                            │ PostgreSQL │   │  Redis   │  │    │
│                            │  (User DB) │   │ (Cache)  │  │    │
│                            └────────────┘   └──────────┘  │    │
│                                                            │    │
│                                                     ┌──────▼───┐│
│                                                     │ MongoDB  ││
│                                                     │(Messages)││
│                                                     └──────────┘│
└───────────────────────────────────────────────────────────────-─┘
```

### 3.2 註冊流程架構圖

```
┌─────────┐         ┌─────────┐         ┌──────────────┐
│ Browser │         │  React  │         │ Spring Boot  │
│  (User) │         │Frontend │         │   Backend    │
└────┬────┘         └────┬────┘         └──────┬───────┘
     │                   │                      │
     │ 1. 輸入註冊資訊   │                      │
     ├──────────────────>│                      │
     │                   │                      │
     │                   │ 2. 前端驗證          │
     │                   │   (即時檢查)         │
     │                   │                      │
     │                   │ 3. POST /api/v1/auth/register
     │                   ├─────────────────────>│
     │                   │                      │
     │                   │                      │ 4. 驗證資料格式
     │                   │                      │
     │                   │                      │ 5. 檢查帳號唯一性
     │                   │                      │    ↓
     │                   │                  ┌───┴────────┐
     │                   │                  │ PostgreSQL │
     │                   │                  │  查詢帳號  │
     │                   │                  └───┬────────┘
     │                   │                      │
     │                   │                      │ 6. 密碼加密 (BCrypt)
     │                   │                      │
     │                   │                      │ 7. 建立使用者記錄
     │                   │                      │    ↓
     │                   │                  ┌───┴────────┐
     │                   │                  │ PostgreSQL │
     │                   │                  │ INSERT User│
     │                   │                  └───┬────────┘
     │                   │                      │
     │                   │ 8. 回傳註冊成功     │
     │                   │<─────────────────────┤
     │                   │                      │
     │ 9. 顯示成功訊息   │                      │
     │   並跳轉登入頁    │                      │
     │<──────────────────┤                      │
     │                   │                      │
```

### 3.3 Kubernetes 部署架構 (註冊服務相關)

```yaml
# 簡化的 K8s 資源結構

- Namespace: chat-system
  
  - Deployments:
    - frontend-deployment (React)
      - Replicas: 3
      - Container Port: 80
    
    - backend-deployment (Spring Boot)
      - Replicas: 5
      - Container Port: 8080
      - Environment Variables:
        - DB_HOST, DB_PORT, DB_NAME
        - REDIS_HOST, REDIS_PORT
        - MONGODB_HOST, MONGODB_PORT
  
  - Services:
    - frontend-service (ClusterIP)
    - backend-service (ClusterIP)
    - postgres-service (ClusterIP)
    - redis-service (ClusterIP)
    - mongodb-service (ClusterIP)
  
  - Ingress:
    - chat-system-ingress
      - Paths:
        - /api/* → backend-service
        - /* → frontend-service
  
  - ConfigMaps:
    - app-config
  
  - Secrets:
    - db-credentials
    - jwt-secret
```

---

## 4. 資料庫架構 - 使用者資料 (PostgreSQL)

### 4.1 資料表結構

#### 4.1.1 users 表（使用者基本資料）

**表格名稱：** `users`

**說明：** 儲存使用者帳號、密碼及基本資訊

| 欄位名稱 | 資料類型 | 約束條件 | 預設值 | 說明 |
|---------|---------|---------|--------|------|
| user_id | UUID | PRIMARY KEY | uuid_generate_v4() | 使用者唯一識別碼 |
| username | VARCHAR(20) | NOT NULL, UNIQUE | - | 使用者帳號 |
| password_hash | VARCHAR(255) | NOT NULL | - | 加密後的密碼 (BCrypt) |
| created_at | TIMESTAMP | NOT NULL | CURRENT_TIMESTAMP | 帳號建立時間 |
| updated_at | TIMESTAMP | NOT NULL | CURRENT_TIMESTAMP | 最後更新時間 |
| last_login_at | TIMESTAMP | NULL | NULL | 最後登入時間 |
| is_active | BOOLEAN | NOT NULL | TRUE | 帳號是否啟用 |
| is_online | BOOLEAN | NOT NULL | FALSE | 是否在線（同步至 Redis） |

**索引：**
- PRIMARY KEY on `user_id`
- UNIQUE INDEX on `username`
- INDEX on `is_active`
- INDEX on `created_at`

**SQL 建立語句：**
```sql
-- 啟用 UUID 擴展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 建立 users 表
CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(20) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_online BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- 約束條件
    CONSTRAINT username_length CHECK (char_length(username) >= 4)
);

-- 建立索引
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_is_active ON users(is_active);
CREATE INDEX idx_users_created_at ON users(created_at);

-- 自動更新 updated_at 的觸發器
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at 
    BEFORE UPDATE ON users 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();
```

### 4.2 資料庫關聯架構圖 (註冊功能階段)

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
│  └───────────────────────────────┘ │
│                                     │
└─────────────────────────────────────┘

註：後續階段將新增以下表格
- friendships (好友關係表)
- friend_requests (好友請求表)
```

### 4.3 註冊功能資料流

**資料寫入流程：**

1. **接收註冊請求**
  - Backend 接收 username, password, confirmPassword

2. **資料驗證**
  - 驗證 username 格式（4-20字元）
  - 驗證 password 複雜度
  - 驗證 password 與 confirmPassword 是否一致

3. **檢查帳號唯一性**
   ```sql
   SELECT COUNT(*) FROM users WHERE username = ?;
   ```
  - 若帳號已存在，返回 409 錯誤

4. **密碼加密**
  - 使用 BCrypt 演算法加密密碼
  - 加密強度：10 rounds

5. **建立使用者記錄**
   ```sql
   INSERT INTO users (username, password_hash) 
   VALUES (?, ?) 
   RETURNING user_id, username, created_at;
   ```

6. **返回成功回應**
  - 返回新建立的使用者資訊（不包含密碼）

---

## 5. 後端實作架構 (Spring Boot)

### 5.1 專案結構

```
src/main/java/com/example/chatsystem/
├── config/
│   ├── SecurityConfig.java          # Spring Security 配置
│   └── JpaConfig.java                # JPA 配置
├── controller/
│   └── AuthController.java           # 認證相關 API
├── service/
│   ├── UserService.java              # 使用者服務接口
│   └── impl/
│       └── UserServiceImpl.java      # 使用者服務實作
├── repository/
│   └── UserRepository.java           # JPA Repository
├── entity/
│   └── User.java                     # 使用者實體
├── dto/
│   ├── RegisterRequest.java          # 註冊請求 DTO
│   ├── RegisterResponse.java         # 註冊回應 DTO
│   └── ApiResponse.java              # 統一 API 回應格式
├── exception/
│   ├── GlobalExceptionHandler.java   # 全局異常處理
│   ├── UsernameAlreadyExistsException.java
│   └── ValidationException.java
└── validator/
    └── PasswordValidator.java        # 密碼驗證器
```

### 5.2 依賴項 (pom.xml 片段)

```xml
<dependencies>
    <!-- Spring Boot Starter Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Spring Boot Starter Data JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <!-- PostgreSQL Driver -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <!-- Spring Security (for BCrypt) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    
    <!-- Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    
    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

---

## 6. 前端實作架構 (React)

### 6.1 專案結構

```
src/
├── components/
│   ├── auth/
│   │   ├── RegisterForm.jsx          # 註冊表單組件
│   │   ├── PasswordStrengthMeter.jsx # 密碼強度指示器
│   │   └── ValidationMessage.jsx     # 驗證訊息組件
│   └── common/
│       ├── Button.jsx                # 按鈕組件
│       ├── Input.jsx                 # 輸入框組件
│       └── LoadingSpinner.jsx        # 載入動畫
├── pages/
│   ├── RegisterPage.jsx              # 註冊頁面
│   └── LoginPage.jsx                 # 登入頁面（後續實作）
├── services/
│   └── authService.js                # 認證相關 API 服務
├── utils/
│   ├── validation.js                 # 驗證工具函數
│   └── constants.js                  # 常數定義
├── hooks/
│   └── useForm.js                    # 表單處理 Hook
└── App.jsx                           # 應用程式主組件
```

### 6.2 套件依賴 (package.json 片段)

```json
{
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.16.0",
    "axios": "^1.5.0"
  },
  "devDependencies": {
    "@vitejs/plugin-react": "^4.0.0",
    "vite": "^4.4.0"
  }
}
```

---

## 7. 開發優先順序與後續規劃

### 7.1 階段一：註冊功能（當前文件範圍）

**完成項目：**
- ✅ UI/UX 設計文件
- ✅ API 規格文件
- ✅ 系統架構設計
- ✅ 資料庫結構設計

**待開發項目：**
1. 後端實作
  - Entity、Repository、Service、Controller
  - 密碼加密邏輯
  - 資料驗證

2. 前端實作
  - 註冊頁面 UI
  - 表單驗證
  - API 串接

3. 測試
  - 單元測試
  - 整合測試
  - UI 測試

### 7.2 後續階段規劃

**階段二：登入功能**
- JWT Token 機制
- 登入 API
- Token 刷新機制
- Redis 儲存線上狀態

**階段三：好友系統**
- 好友列表
- 新增好友（請求/確認/拒絕）
- 刪除好友
- 資料表：friendships, friend_requests

**階段四：聊天功能**
- WebSocket 連線
- 訊息傳送/接收
- 聊天室介面
- MongoDB 訊息儲存

**階段五：Kubernetes 部署**
- Dockerfile 撰寫
- K8s YAML 配置
- CI/CD Pipeline

---

## 8. 文件版本記錄

| 版本 | 日期 | 作者 | 變更說明 |
|-----|------|------|---------|
| 1.0 | 2025-09-30 | Development Team | 初版：註冊功能完整文件 |

---

## 附錄

### A. 相關文件連結

- [登入功能文件](./02-login.md) - 待建立
- [好友系統文件](./03-friend-system.md) - 待建立
- [聊天功能文件](./04-chat-function.md) - 待建立

### B. 問題與討論

如有任何問題或建議，請在此記錄：

- [ ] 問題項目 1
- [ ] 問題項目 2

---

**下一步行動：**
1. 審核並確認此文件規格
2. 根據需求調整文件內容
3. 開始後端程式碼實作
4. 並行開發前端註冊頁面 UI