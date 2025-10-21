# 聊天系統專案總覽文件

**專案名稱：** ChatHub - 簡易聊天系統  
**文件版本：** 1.0  
**最後更新：** 2025-10-06  
**文件狀態：** 完整版

---

## 📋 目錄

1. [專案簡介](#1-專案簡介)
2. [技術棧總覽](#2-技術棧總覽)
3. [系統架構](#3-系統架構)
4. [資料庫設計](#4-資料庫設計)
5. [API 端點總覽](#5-api-端點總覽)
6. [核心功能模組](#6-核心功能模組)
7. [開發路線圖](#7-開發路線圖)
8. [快速參考](#8-快速參考)

---

## 1. 專案簡介

### 1.1 專案概述

**ChatHub** 是一個現代化的即時通訊系統，提供使用者註冊、好友管理、即時聊天等核心功能。系統採用前後端分離架構，使用 React 作為前端框架，Spring Boot 作為後端服務，支援水平擴展與高併發場景。

### 1.2 核心特性

✅ **使用者認證系統**
- 帳號註冊與登入
- JWT Token 雙令牌機制
- 登入失敗次數限制（防暴力破解）
- 記住我功能（7天免登入）

✅ **好友系統**
- 搜尋並發送好友請求
- 接受/拒絕好友請求
- 好友列表管理
- 即時線上狀態顯示

✅ **即時聊天功能**
- WebSocket 即時通訊
- 文字訊息發送/接收
- 歷史訊息查詢（分頁載入）
- 訊息已讀回執
- 正在輸入指示器
- 未讀訊息提醒

✅ **效能優化**
- Redis 多層快取策略
- MongoDB 訊息持久化
- WebSocket 跨 Pod 通訊（Redis Pub/Sub）
- 分頁載入機制

### 1.3 系統特色

🚀 **微服務就緒**
- Kubernetes 部署架構
- 多 Pod 水平擴展
- Redis Pub/Sub 跨服務通訊

🔒 **安全性優先**
- BCrypt 密碼加密
- JWT Token 黑名單機制
- XSS 防護
- 頻率限制（Rate Limiting）

📱 **響應式設計**
- 支援桌面版與手機版
- 現代化 UI 設計
- 流暢的使用者體驗

---

## 2. 技術棧總覽

### 2.1 前端技術

| 技術 | 版本 | 用途 |
|-----|------|------|
| **React** | 18.2.0 | 前端框架 |
| **React Router** | 6.16.0 | 路由管理 |
| **Axios** | 1.5.0 | HTTP 客戶端 |
| **Vite** | 4.4.0 | 建置工具 |

**UI 設計風格：**
- 主色：#1a1a1a（深黑）
- 背景：#fafafa（淺灰）
- 圓角設計（8px-16px）
- 現代化黑白主題

### 2.2 後端技術

| 技術 | 版本 | 用途 |
|-----|------|------|
| **Spring Boot** | 3.2.x | 應用框架 |
| **Spring Security** | - | 安全框架 |
| **Spring Data JPA** | - | ORM 框架 |
| **Spring WebSocket** | - | WebSocket 支援 |
| **JWT (jjwt)** | 0.11.x | Token 認證 |

### 2.3 資料庫

| 資料庫 | 用途 | 資料類型 |
|-------|------|---------|
| **PostgreSQL** | 關聯式資料 | 使用者、好友關係、Token |
| **MongoDB** | NoSQL 文件庫 | 訊息、對話 |
| **Redis** | 快取與 Pub/Sub | 線上狀態、訊息快取、跨 Pod 通訊 |

### 2.4 部署與基礎設施

| 技術 | 用途 |
|-----|------|
| **Kubernetes** | 容器編排 |
| **Docker** | 容器化 |
| **Nginx Ingress** | 負載均衡與路由 |
| **Prometheus + Grafana** | 監控與可視化 |

---

## 3. 系統架構

### 3.1 整體架構圖

```
┌─────────────────────────────────────────────────────────────────┐
│                      Kubernetes Cluster                          │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    Nginx Ingress                          │   │
│  │              (負載均衡 + SSL 終止)                        │   │
│  └───────────────────┬──────────────────────────────────────┘   │
│                      │                                            │
│         ┬────────────┴────────────┬                              │
│         │                         │                               │
│         ▼                         ▼                               │
│  ┌──────────────┐        ┌──────────────┐                       │
│  │   Frontend   │        │   Backend    │                       │
│  │  React App   │        │ Spring Boot  │                       │
│  │  (Pod 1-3)   │        │  (Pod 1-5)   │                       │
│  └──────────────┘        └──────┬───────┘                       │
│                                  │                                │
│                    ┌─────────────┼─────────────┐                │
│                    │             │             │                 │
│                    ▼             ▼             ▼                 │
│            ┌────────────┐ ┌──────────┐ ┌──────────┐            │
│            │PostgreSQL  │ │  Redis   │ │ MongoDB  │            │
│            │   (RDS)    │ │(ElastiCache)│(Atlas) │            │
│            └────────────┘ └──────────┘ └──────────┘            │
│                                                                   │
└───────────────────────────────────────────────────────────────────┘
```

### 3.2 應用層架構

```
前端層 (React)
    ├── 認證模組 (JWT Token 管理)
    ├── 好友模組 (好友列表、請求管理)
    └── 聊天模組 (WebSocket 連線、訊息收發)
           │
           ↓
       HTTPS/WSS
           │
           ↓
後端層 (Spring Boot)
    ├── API Gateway (REST API)
    ├── WebSocket Handler (即時通訊)
    ├── 認證服務 (JWT 驗證)
    ├── 好友服務 (好友管理)
    └── 聊天服務 (訊息處理)
           │
           ↓
    ┌──────┴───────┬──────────┐
    │              │          │
    ▼              ▼          ▼
PostgreSQL      Redis     MongoDB
(使用者資料)   (快取)    (訊息資料)
```

### 3.3 WebSocket 跨 Pod 通訊架構

```
User A → Backend Pod 1 → Redis Pub/Sub → Backend Pod 3 → User B
                            (廣播訊息)
```

**關鍵設計：**
- 每個 Pod 訂閱 Redis 頻道
- 訊息透過 Pub/Sub 廣播
- 接收 Pod 推送給對應使用者

---

## 4. 資料庫設計

### 4.1 PostgreSQL（關聯式資料）

#### **users 表** - 使用者基本資料
```sql
CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(20) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_online BOOLEAN NOT NULL DEFAULT FALSE
);
```

#### **refresh_tokens 表** - Refresh Token 管理
```sql
CREATE TABLE refresh_tokens (
    token_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP,
    device_info VARCHAR(255),
    ip_address VARCHAR(45)
);
```

#### **friendships 表** - 好友關係（雙向）
```sql
CREATE TABLE friendships (
    friendship_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    friend_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT no_self_friendship CHECK (user_id != friend_id),
    UNIQUE(user_id, friend_id)
);
```

#### **friend_requests 表** - 好友請求
```sql
CREATE TABLE friend_requests (
    request_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    from_user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    to_user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP,
    CONSTRAINT valid_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    CONSTRAINT no_self_request CHECK (from_user_id != to_user_id)
);
```

### 4.2 MongoDB（文件資料庫）

#### **messages Collection** - 訊息資料
```javascript
{
  "_id": ObjectId("..."),
  "messageId": "uuid-string",
  "conversationId": "uuid-string",
  "senderId": "uuid-string",
  "senderUsername": "alice_wang",
  "recipientId": "uuid-string",
  "content": "訊息內容",
  "timestamp": ISODate("2025-09-30T10:30:00Z"),
  "isRead": false,
  "readAt": null,
  "clientMessageId": "client-uuid",
  "createdAt": ISODate("2025-09-30T10:30:00Z")
}
```

**索引：**
- `{ conversationId: 1, timestamp: -1 }` - 查詢對話訊息
- `{ recipientId: 1, isRead: 1 }` - 未讀訊息查詢
- `{ clientMessageId: 1 }` - 去重索引（UNIQUE）

#### **conversations Collection** - 對話資料
```javascript
{
  "_id": ObjectId("..."),
  "conversationId": "uuid-string",
  "participants": ["uuid-user-1", "uuid-user-2"],
  "lastMessage": {
    "messageId": "uuid-string",
    "content": "訊息內容",
    "senderId": "uuid-string",
    "timestamp": ISODate("2025-09-30T10:30:00Z")
  },
  "unreadCounts": {
    "uuid-user-1": 0,
    "uuid-user-2": 2
  },
  "createdAt": ISODate("2025-09-25T14:20:00Z"),
  "updatedAt": ISODate("2025-09-30T10:30:00Z")
}
```

**索引：**
- `{ participants: 1 }` - 查詢使用者對話（UNIQUE）
- `{ participants: 1, updatedAt: -1 }` - 對話列表排序

### 4.3 Redis（快取與 Pub/Sub）

#### **資料結構總覽**

| Key 格式 | 類型 | TTL | 用途 |
|---------|------|-----|------|
| `user:online:{userId}` | String (JSON) | 1 小時 | 線上狀態 |
| `login:attempt:{username}` | String (Integer) | 15 分鐘 | 登入失敗計數 |
| `token:blacklist:{jti}` | String | Token 有效期 | Token 黑名單 |
| `conversation:messages:{id}` | Sorted Set | 7 天 | 訊息快取（最近100則） |
| `user:unread:{userId}` | Hash | 永久 | 未讀訊息計數 |
| `typing:{conversationId}:{userId}` | String | 5 秒 | 正在輸入狀態 |

#### **Pub/Sub 頻道**
- `user:{userId}` - 使用者專屬頻道（接收訊息）
- `conversation:{conversationId}` - 對話頻道

---

## 5. API 端點總覽

### 5.1 認證 API (`/api/v1/auth`)

| 方法 | 端點 | 說明 | 認證 |
|-----|------|------|------|
| POST | `/register` | 註冊新使用者 | ❌ |
| GET | `/check-username` | 檢查帳號可用性 | ❌ |
| POST | `/login` | 使用者登入 | ❌ |
| POST | `/refresh` | 刷新 Access Token | ❌ |
| POST | `/logout` | 使用者登出 | ✅ |
| GET | `/verify` | 驗證 Token 有效性 | ✅ |

### 5.2 好友 API (`/api/v1/friends`)

| 方法 | 端點 | 說明 | 認證 |
|-----|------|------|------|
| POST | `/requests` | 發送好友請求 | ✅ |
| GET | `/requests/received` | 查看收到的請求 | ✅ |
| PUT | `/requests/{id}` | 處理好友請求 | ✅ |
| GET | `/` | 查看好友列表 | ✅ |
| DELETE | `/{id}` | 刪除好友 | ✅ |
| POST | `/online-status` | 批次查詢線上狀態 | ✅ |

### 5.3 聊天 API (`/api/v1/conversations`)

| 方法 | 端點 | 說明 | 認證 |
|-----|------|------|------|
| GET | `/` | 查詢對話列表 | ✅ |
| GET | `/{id}/messages` | 查詢歷史訊息 | ✅ |
| PUT | `/{id}/read` | 標記訊息為已讀 | ✅ |
| GET | `/unread-count` | 查詢未讀訊息總數 | ✅ |

### 5.4 WebSocket API

**連線 URL：** `ws://localhost:8080/ws/chat?token={access-token}`

#### **訊息類型（CLIENT → SERVER）**

| 類型 | 說明 | Payload |
|-----|------|---------|
| `SEND_MESSAGE` | 發送訊息 | `{recipientId, content, clientMessageId}` |
| `TYPING_START` | 開始輸入 | `{recipientId}` |
| `TYPING_STOP` | 停止輸入 | `{recipientId}` |
| `MESSAGE_READ` | 標記已讀 | `{conversationId, messageId}` |
| `PING` | 心跳 | `{}` |

#### **訊息類型（SERVER → CLIENT）**

| 類型 | 說明 | Payload |
|-----|------|---------|
| `CONNECTION_ESTABLISHED` | 連線成功 | `{userId, timestamp}` |
| `NEW_MESSAGE` | 新訊息 | `{messageId, conversationId, senderId, content, timestamp}` |
| `MESSAGE_DELIVERED` | 訊息送達 | `{messageId, clientMessageId, timestamp}` |
| `USER_TYPING` | 對方正在輸入 | `{userId, username, isTyping}` |
| `MESSAGE_READ_RECEIPT` | 已讀回執 | `{conversationId, messageId, readBy, readAt}` |
| `PONG` | 心跳回應 | `{timestamp}` |
| `ERROR` | 錯誤訊息 | `{code, message}` |

---

## 6. 核心功能模組

### 6.1 註冊功能

**流程：**
1. 前端驗證表單（帳號4-20字元、密碼強度）
2. 後端檢查帳號唯一性
3. BCrypt 加密密碼（強度10）
4. 建立使用者記錄於 PostgreSQL
5. 返回註冊成功（不自動登入）

**密碼規則：**
- ✅ 至少 8 位數
- ✅ 至少一個大寫字母（A-Z）
- ✅ 至少一個小寫字母（a-z）
- ✅ 至少一個數字（0-9）
- ✅ 至少一個符號（@$!%*?&）

### 6.2 登入功能

**流程：**
1. 驗證帳號密碼（BCrypt 比對）
2. 檢查登入失敗次數（5次/15分鐘限制）
3. 生成 JWT Access Token（1小時）
4. 生成 Refresh Token（7天或24小時）
5. 更新 PostgreSQL `last_login_at`
6. 設定 Redis 線上狀態（TTL: 1小時）
7. 返回 Tokens

**Token 機制：**
- **Access Token**：用於 API 認證，有效期 1 小時
- **Refresh Token**：用於刷新 Access Token
    - 記住我 = true：7 天
    - 記住我 = false：24 小時

### 6.3 好友系統

**好友請求流程：**
```
User A 輸入 User B 帳號
    ↓
檢查：是否為自己？已是好友？已有請求？
    ↓
建立請求記錄（status: PENDING）
    ↓
User B 查看請求列表
    ↓
User B 點擊「接受」
    ↓
更新請求狀態（status: ACCEPTED）
    ↓
建立雙向好友關係：
  - INSERT (User A, User B)
  - INSERT (User B, User A)
```

**線上狀態查詢：**
- 優先從 Redis 查詢（快速）
- Redis 無資料時從 PostgreSQL 查詢
- 批次查詢優化（避免 N+1 問題）

### 6.4 即時聊天功能

**訊息發送流程：**
```
User A 輸入訊息 → WebSocket SEND_MESSAGE
    ↓
後端驗證（是否為好友、內容長度）
    ↓
儲存至 MongoDB (messages)
    ↓
更新對話資訊 (conversations)
    ↓
快取至 Redis（最近100則）
    ↓
增加未讀數（Redis Hash）
    ↓
推送給 User B（WebSocket NEW_MESSAGE）
    ↓
User B 收到訊息、顯示、播放提示音
```

**跨 Pod 訊息推送：**
```
User A (Pod 1) 發送訊息
    ↓
儲存 MongoDB + Redis
    ↓
發布至 Redis Pub/Sub: user:{User B ID}
    ↓
所有 Pod 訂閱該頻道
    ↓
User B 連線的 Pod (Pod 3) 收到訊息
    ↓
推送給 User B 的 WebSocket 連線
```

**訊息快取策略：**
- **前端記憶體**：當前對話訊息
- **Redis Sorted Set**：最近 100 則訊息（7天 TTL）
- **MongoDB**：完整歷史訊息（永久）

---

## 7. 開發路線圖

### 7.1 已完成項目 ✅

- ✅ **文件撰寫完成**
    - 註冊功能文件
    - 登入功能文件
    - 好友系統文件
    - 聊天功能文件
    - API 規格文件（4份）
    - UI 原型設計（3份）

### 7.2 開發階段規劃

#### **Phase 1：基礎建設（2週）**
- [ ] 專案初始化
    - Spring Boot 專案建立
    - React 專案建立
    - Docker Compose 環境設置
- [ ] 資料庫建立
    - PostgreSQL Schema 初始化
    - MongoDB 設置
    - Redis 配置
- [ ] CI/CD Pipeline 設置

#### **Phase 2：認證模組（1週）**
- [ ] 後端實作
    - User Entity 與 Repository
    - 註冊 API
    - 登入 API（JWT）
    - Token 刷新機制
- [ ] 前端實作
    - 註冊頁面 UI
    - 登入頁面 UI
    - Token 管理（httpClient）
- [ ] 測試
    - 單元測試
    - 整合測試

#### **Phase 3：好友系統（1.5週）**
- [ ] 後端實作
    - 好友請求 API
    - 好友列表 API
    - 線上狀態查詢
- [ ] 前端實作
    - 好友列表頁面
    - 新增好友 Modal
    - 線上狀態顯示
- [ ] 測試

#### **Phase 4：聊天功能（2週）**
- [ ] 後端實作
    - WebSocket Handler
    - 訊息儲存（MongoDB）
    - Redis 快取
    - Pub/Sub 跨 Pod 通訊
- [ ] 前端實作
    - 聊天頁面 UI
    - WebSocket 連線管理
    - 訊息收發
    - 歷史訊息載入
- [ ] 測試

#### **Phase 5：優化與部署（1週）**
- [ ] 效能優化
    - Redis 快取優化
    - 資料庫索引優化
    - 前端代碼分割
- [ ] Kubernetes 部署
    - Dockerfile 撰寫
    - K8s YAML 配置
    - Helm Chart（選配）
- [ ] 監控與日誌
    - Prometheus Metrics
    - Grafana Dashboard
    - ELK Stack（選配）

### 7.3 未來擴展（Optional）

- [ ] 群組聊天功能
- [ ] 檔案傳輸（圖片、文件）
- [ ] 語音/視訊通話
- [ ] 端到端加密
- [ ] 推送通知（PWA）
- [ ] 國際化（i18n）

---

## 8. 快速參考

### 8.1 資料庫速查

#### **PostgreSQL 表**
- `users` - 使用者資料
- `refresh_tokens` - Refresh Token
- `friendships` - 好友關係（雙向）
- `friend_requests` - 好友請求

#### **MongoDB Collections**
- `messages` - 訊息資料
- `conversations` - 對話資料

#### **Redis Keys**
- `user:online:{userId}` - 線上狀態
- `login:attempt:{username}` - 登入失敗計數
- `token:blacklist:{jti}` - Token 黑名單
- `conversation:messages:{id}` - 訊息快取
- `user:unread:{userId}` - 未讀計數
- `typing:{conversationId}:{userId}` - 正在輸入

### 8.2 API 端點速查

```
認證 API
POST   /api/v1/auth/register       註冊
POST   /api/v1/auth/login          登入
POST   /api/v1/auth/refresh        刷新 Token
POST   /api/v1/auth/logout         登出
GET    /api/v1/auth/verify         驗證 Token

好友 API
POST   /api/v1/friends/requests              發送請求
GET    /api/v1/friends/requests/received     查看請求
PUT    /api/v1/friends/requests/{id}         處理請求
GET    /api/v1/friends                       好友列表
DELETE /api/v1/friends/{id}                  刪除好友

聊天 API
GET    /api/v1/conversations                 對話列表
GET    /api/v1/conversations/{id}/messages   歷史訊息
PUT    /api/v1/conversations/{id}/read       標記已讀

WebSocket
ws://localhost:8080/ws/chat?token={token}
```

### 8.3 技術棧版本速查

```
前端：React 18.2.0 + Vite 4.4.0
後端：Spring Boot 3.2.x + Java 17+
資料庫：PostgreSQL 15+ / MongoDB 7.0+ / Redis 7.0+
部署：Kubernetes 1.28+ / Docker 24.0+
```

### 8.4 開發環境端口

| 服務 | 端口 | 說明 |
|-----|------|------|
| React Dev Server | 5173 | 前端開發伺服器 |
| Spring Boot | 8080 | 後端 API 伺服器 |
| PostgreSQL | 5432 | 關聯式資料庫 |
| MongoDB | 27017 | 文件資料庫 |
| Redis | 6379 | 快取與 Pub/Sub |

---

## 📌 重要提醒

### Token 使用規則
- **Access Token**：放在 `Authorization: Bearer {token}` 標頭
- **有效期**：1 小時
- **刷新時機**：剩餘 5 分鐘時自動刷新
- **儲存位置**：
    - 記住我 = true → localStorage
    - 記住我 = false → sessionStorage

### 密碼安全
- ✅ 傳輸使用 HTTPS
- ✅ 儲存使用 BCrypt（強度 10）
- ✅ 永不儲存明文密碼
- ✅ 登入失敗限制（5次/15分鐘）

### WebSocket 注意事項
- ✅ 連線需帶 Token：`?token={access-token}`
- ✅ 心跳間隔：30 秒
- ✅ 自動重連：指數退避（最多 5 次）
- ✅ 訊息去重：使用 `clientMessageId`

---

**文件完成！** 🎉

此專案總覽提供了完整的技術架構、資料庫設計、API 規格與開發路線圖。建議將此文件保存為 `00-project-overview.md`，作為專案開發的核心參考文件。


📋 開發進度更新
✅ 已完成

專案架構規劃
資料庫設計文件（4份）
API 規格文件（4份）
PostgreSQL 環境建置（Docker, port 5433）
Spring Boot 專案初始化（3.4.10）
註冊 API 完整實作

Entity（User）
Repository（UserRepository）
Service（UserService + Impl）
Controller（AuthController）
Exception Handler（GlobalExceptionHandler）
Security Config（BCrypt + permitAll）
測試通過 ✅



🔄 進行中

登入 API（下一步）

JWT Token 機制
Redis 線上狀態
Refresh Token



⏳ 待辦事項（API 優先）

登入 API
Token 刷新 API
登出 API
好友請求 API（5個端點）
聊天 WebSocket
訊息歷史 API

📝 後續規劃（UI）

React 前端專案初始化
註冊頁面 UI
登入頁面 UI
好友列表 UI
聊天介面 UI


🎯 下一步：登入 API
準備好時回報，我會提供登入 API 的實作指南（包含 JWT + Redis + Refresh Token）。

🎉 登入 API 完成！
✅ 已完成功能總覽
1️⃣ 環境配置

✅ Redis Docker 環境（port 6379）
✅ JWT + Redis 依賴
✅ application.yml 配置

2️⃣ 核心組件

✅ JwtProperties（JWT 配置讀取）
✅ RedisConfig（Redis 配置）
✅ JwtTokenProvider（Token 生成與驗證）
✅ RedisService（線上狀態、登入失敗計數）

3️⃣ 資料模型

✅ RefreshToken Entity
✅ RefreshTokenRepository
✅ LoginRequest/LoginResponse DTO

4️⃣ 登入功能

✅ 帳號密碼驗證（BCrypt）
✅ 登入失敗限制（5次/15分鐘）
✅ JWT Access Token 生成（1小時）
✅ Refresh Token 生成（記住我：7天 / 不記住：24小時）
✅ Redis 線上狀態設定
✅ 錯誤訊息處理


📋 下一步開發選項
根據專案路線圖，接下來可以：
A. Token 刷新 API（推薦）

使用 Refresh Token 換取新的 Access Token

B. 登出 API

Token 黑名單機制
清除線上狀態

C. Token 驗證 API

檢查 Token 是否有效

D. JWT 認證過濾器

自動驗證 API 請求的 Token

🎉 Token 刷新 API 測試通過！

✅ 已完成功能總覽
1️⃣ 註冊 API

✅ 帳號唯一性檢查
✅ 密碼強度驗證
✅ BCrypt 加密

2️⃣ 登入 API

✅ 帳號密碼驗證
✅ 登入失敗限制（5次/15分鐘）
✅ JWT Access Token（1小時）
✅ Refresh Token（記住我：7天 / 不記住：24小時）
✅ Redis 線上狀態
✅ Token Hash 儲存

3️⃣ Token 刷新 API ✅

✅ Refresh Token 驗證
✅ 過期與撤銷檢查
✅ Token 輪換機制（提高安全性）
✅ 生成新 Access Token
✅ 更新線上狀態


📋 下一步開發選項
根據專案路線圖，接下來可以：
B. 登出 API

Token 黑名單機制（Redis）
撤銷 Refresh Token
清除線上狀態
單一裝置登出 vs 所有裝置登出

C. Token 驗證 API

/api/v1/auth/verify 端點
檢查 Token 是否有效
返回使用者資訊

D. JWT 認證過濾器

Spring Security Filter Chain
自動驗證所有受保護的 API
從 JWT 提取使用者資訊
設定 SecurityContext


🎯 建議順序
推薦先做 D（JWT 過濾器） → 因為後續的好友、聊天 API 都需要它來驗證身份！
完成過濾器後，其他 API 就能自動受到保護。