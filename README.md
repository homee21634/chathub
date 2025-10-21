# 💬 ChatHub - Real-time Chat System

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.10-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MongoDB](https://img.shields.io/badge/MongoDB-7.0+-green.svg)](https://www.mongodb.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-blue.svg)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7.0+-red.svg)](https://redis.io/)

A modern, scalable real-time chat system built with Spring Boot, WebSocket, MongoDB, PostgreSQL, and Redis.

[English](#english) | [繁體中文](#繁體中文)

---

## English

### 🌟 Features

#### ✅ Implemented
- **User Authentication**
    - User registration with validation
    - JWT-based login (Access Token + Refresh Token)
    - Token refresh mechanism
    - Logout with token blacklist
    - Login attempt rate limiting (5 attempts / 15 minutes)

- **Friend System**
    - Send/receive friend requests
    - Accept/reject friend requests
    - Friend list management
    - Delete friends
    - Real-time online status (Redis)

- **Real-time Chat**
    - WebSocket-based instant messaging
    - Message persistence (MongoDB)
    - Message delivery confirmation
    - Message deduplication (clientMessageId)
    - Typing indicators
    - Read receipts
    - Chat history with pagination
    - Unread message count

#### 🚧 In Progress
- Redis Pub/Sub for multi-pod communication
- Message caching optimization
- Frontend application (React)

#### 📋 Planned
- Group chat
- File sharing
- Voice/Video calls
- Push notifications

---

### 🏗️ Architecture

```
┌─────────────────────────────────────────────────┐
│                   Client                         │
│          (React - To be implemented)             │
└──────────────────┬──────────────────────────────┘
                   │ HTTPS / WSS
                   ▼
┌─────────────────────────────────────────────────┐
│              Spring Boot Backend                │
│  ┌──────────────────────────────────────────┐  │
│  │  REST API + WebSocket                    │  │
│  └──────────────────────────────────────────┘  │
└───────┬────────────┬────────────┬──────────────┘
        │            │            │
        ▼            ▼            ▼
┌──────────┐  ┌──────────┐  ┌──────────┐
│PostgreSQL│  │  Redis   │  │ MongoDB  │
│  (Users) │  │ (Cache)  │  │(Messages)│
└──────────┘  └──────────┘  └──────────┘
```

---

### 🚀 Quick Start

#### Prerequisites

- Java 17+
- Maven 3.8+
- Docker & Docker Compose

#### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/chathub.git
   cd chathub
   ```

2. **Start databases with Docker Compose**
   ```bash
   docker-compose up -d
   ```

3. **Configure application**

   Edit `src/main/resources/application.yml`:
   ```yaml
   jwt:
     secret: your-secret-key-here-minimum-512-bits
   ```

4. **Build and run**
   ```bash
   # Build
   mvn clean install
   
   # Run
   mvn spring-boot:run
   ```

5. **Verify**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

---

### 📚 API Documentation

#### Authentication

**Register**
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "username": "alice",
  "password": "Password123!"
}
```

**Login**
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "alice",
  "password": "Password123!",
  "rememberMe": true
}
```

**Response**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "userId": "uuid",
    "username": "alice"
  }
}
```

#### Friends

**Send Friend Request**
```http
POST /api/v1/friends/requests
Authorization: Bearer {token}
Content-Type: application/json

{
  "toUserId": "target-user-uuid"
}
```

**Get Friend List**
```http
GET /api/v1/friends
Authorization: Bearer {token}
```

#### Chat

**Get Conversations**
```http
GET /api/v1/conversations?page=0&size=20
Authorization: Bearer {token}
```

**Get Message History**
```http
GET /api/v1/conversations/{conversationId}/messages?page=0&size=20
Authorization: Bearer {token}
```

#### WebSocket

**Connection URL**
```
ws://localhost:8080/ws/chat?token={access-token}
```

**Send Message**
```json
{
  "type": "SEND_MESSAGE",
  "payload": {
    "recipientId": "uuid",
    "content": "Hello!",
    "clientMessageId": "client-uuid"
  },
  "timestamp": "2025-10-21T10:00:00Z"
}
```

**Receive Message**
```json
{
  "type": "NEW_MESSAGE",
  "payload": {
    "messageId": "uuid",
    "conversationId": "uuid1_uuid2",
    "senderId": "uuid",
    "senderUsername": "alice",
    "content": "Hello!",
    "timestamp": "2025-10-21T10:00:00Z"
  },
  "timestamp": "2025-10-21T10:00:01Z"
}
```

---

### 🗄️ Database Schema

#### PostgreSQL

- **users** - User accounts and profiles
- **friendships** - Friend relationships (bidirectional)
- **friend_requests** - Pending friend requests
- **refresh_tokens** - JWT refresh token storage

#### MongoDB

- **messages** - Chat message history
- **conversations** - Conversation metadata and unread counts

#### Redis

- `user:online:{userId}` - Online status (1 hour TTL)
- `login:attempt:{username}` - Login failure count (15 min TTL)
- `token:blacklist:{jti}` - Revoked token blacklist

---

### 🧪 Testing

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=MessageServiceTest

# View test report
open target/surefire-reports/index.html
```

**Test Coverage:**
- MongoDB Connection: ✅ 5/5 passed
- Message Service: ✅ 5/5 passed
- Conversation Service: ✅ 5/5 passed
- REST API: ✅ 6/6 passed

---

### 🛠️ Technology Stack

| Category | Technology |
|----------|-----------|
| **Backend** | Spring Boot 3.4.10, Java 17 |
| **Security** | JWT (jjwt 0.11.x), BCrypt |
| **Database** | PostgreSQL 15+, MongoDB 7.0+ |
| **Cache** | Redis 7.0+ |
| **Real-time** | WebSocket (STOMP) |
| **Build** | Maven 3.8+ |
| **Container** | Docker, Docker Compose |

---

### 📁 Project Structure

```
chatsystem/
├── src/
│   ├── main/
│   │   ├── java/com/chathub/
│   │   │   ├── config/          # Configuration classes
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── dto/             # Data transfer objects
│   │   │   ├── entity/          # JPA/MongoDB entities
│   │   │   ├── handler/         # WebSocket handlers
│   │   │   ├── repository/      # Data access layer
│   │   │   ├── security/        # Security components
│   │   │   └── service/         # Business logic
│   │   └── resources/
│   │       └── application.yml
│   └── test/                    # Test classes
├── docker-compose.yml
├── pom.xml
└── README.md
```

---

### 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

### 👤 Author

**Your Name**
- GitHub: [@homee21634](https://github.com/yourusername)

---

### 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- MongoDB and PostgreSQL communities
- All contributors who helped with this project

---

## 繁體中文

### 🌟 功能特色

#### ✅ 已完成
- **使用者認證系統**
    - 使用者註冊（帳號驗證）
    - JWT 登入機制（Access Token + Refresh Token）
    - Token 刷新機制
    - 登出功能（Token 黑名單）
    - 登入失敗限制（5次/15分鐘）

- **好友系統**
    - 發送/接收好友請求
    - 接受/拒絕好友請求
    - 好友列表管理
    - 刪除好友
    - 即時線上狀態（Redis）

- **即時聊天**
    - WebSocket 即時通訊
    - 訊息持久化（MongoDB）
    - 訊息送達確認
    - 訊息去重機制（clientMessageId）
    - 正在輸入提示
    - 已讀回執
    - 歷史訊息查詢（分頁）
    - 未讀訊息統計

#### 🚧 開發中
- Redis Pub/Sub 跨 Pod 通訊
- 訊息快取優化
- 前端應用（React）

#### 📋 規劃中
- 群組聊天
- 檔案分享
- 語音/視訊通話
- 推送通知

---

### 🚀 快速開始

#### 環境需求

- Java 17+
- Maven 3.8+
- Docker & Docker Compose

#### 安裝步驟

1. **克隆專案**
   ```bash
   git clone https://github.com/yourusername/chathub.git
   cd chathub
   ```

2. **啟動資料庫（Docker Compose）**
   ```bash
   docker-compose up -d
   ```

3. **配置應用程式**

   編輯 `src/main/resources/application.yml`：
   ```yaml
   jwt:
     secret: 你的密鑰-至少512位元
   ```

4. **建置並執行**
   ```bash
   # 建置
   mvn clean install
   
   # 執行
   mvn spring-boot:run
   ```

5. **驗證服務**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

---

### 🗄️ 資料庫結構

#### PostgreSQL

- **users** - 使用者帳號與個人資料
- **friendships** - 好友關係（雙向）
- **friend_requests** - 待處理的好友請求
- **refresh_tokens** - JWT 刷新令牌儲存

#### MongoDB

- **messages** - 聊天訊息歷史記錄
- **conversations** - 對話元資料與未讀數

#### Redis

- `user:online:{userId}` - 線上狀態（1小時 TTL）
- `login:attempt:{username}` - 登入失敗計數（15分鐘 TTL）
- `token:blacklist:{jti}` - 已撤銷的令牌黑名單

---

### 🧪 測試

```bash
# 執行所有測試
mvn test

# 執行特定測試
mvn test -Dtest=MessageServiceTest

# 查看測試報告
open target/surefire-reports/index.html
```

**測試涵蓋率：**
- MongoDB 連線測試：✅ 5/5 通過
- 訊息服務測試：✅ 5/5 通過
- 對話服務測試：✅ 5/5 通過
- REST API 測試：✅ 6/6 通過

---

### 🛠️ 技術棧

| 類別 | 技術 |
|------|------|
| **後端框架** | Spring Boot 3.4.10, Java 17 |
| **安全機制** | JWT (jjwt 0.11.x), BCrypt |
| **資料庫** | PostgreSQL 15+, MongoDB 7.0+ |
| **快取** | Redis 7.0+ |
| **即時通訊** | WebSocket (STOMP) |
| **建置工具** | Maven 3.8+ |
| **容器化** | Docker, Docker Compose |

---

### 🤝 貢獻指南

歡迎提交 Pull Request！

1. Fork 本專案
2. 建立功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交變更 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 開啟 Pull Request

---

### 📞 聯絡方式

**專案作者**
- GitHub: [@homee21634](https://github.com/yourusername)

---

### ⭐ Star History

如果這個專案對你有幫助，請給個 Star ⭐

[![Star History Chart](https://api.star-history.com/svg?repos=yourusername/chathub&type=Date)](https://star-history.com/#yourusername/chathub&Date)