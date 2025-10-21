# Spring Data 統一抽象層

## 核心理念

```
┌──────────────────────────────────────────────────┐
│       你寫的程式碼（統一的 Repository 介面）        │
│                                                  │
│   UserRepository extends CrudRepository<User>    │
│       ├── findById(Long id)                      │
│       ├── findAll()                              │
│       ├── save(User user)                        │
│       └── delete(User user)                      │
└───────────────────┬──────────────────────────────┘
                    │
        ┌───────────┴───────────┐
        │                       │
        ▼                       ▼
┌──────────────────┐    ┌──────────────────┐
│ Spring Data      │    │ 你的專案只需      │
│ Commons          │    │ 換一個依賴，      │
│ (核心抽象層)      │    │ 就能切換資料庫！  │
└────┬─────────────┘    └──────────────────┘
     │
     ├─────┬─────┬─────┬─────┐
     ▼     ▼     ▼     ▼     ▼
   JPA  MongoDB Redis  ES  Cassandra
    │     │      │     │     │
    ▼     ▼      ▼     ▼     ▼
PostgreSQL Mongo Redis ES  Cassandra
  MySQL  Atlas Cache Search Database
```

---

## 🎯 實際範例

### 相同的程式碼，不同的資料庫

#### 範例：查詢使用者

```java
// ✅ 統一的介面定義（不管用什麼資料庫）
public interface UserRepository extends CrudRepository<User, Long> {
    User findByUsername(String username);
    List<User> findByAgeGreaterThan(int age);
}
```

---

### Spring Data JPA（SQL 資料庫）

```java
// pom.xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

// application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
```

**自動轉換為 SQL：**
```sql
SELECT * FROM users WHERE username = ?
SELECT * FROM users WHERE age > ?
```

**支援：** PostgreSQL, MySQL, Oracle, SQL Server, H2...

---

### Spring Data MongoDB

```java
// pom.xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>

// application.yml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/mydb
```

**自動轉換為 MongoDB 查詢：**
```javascript
db.users.find({ username: "alice" })
db.users.find({ age: { $gt: 25 } })
```

---

### Spring Data Redis

```java
// pom.xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

// application.yml
spring:
  redis:
    host: localhost
    port: 6379
```

**自動轉換為 Redis 命令：**
```
GET user:alice
HGETALL user:alice
```

---

### Spring Data Elasticsearch

```java
// pom.xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
```

**自動轉換為 ES 查詢：**
```json
{
  "query": {
    "match": { "username": "alice" }
  }
}
```

---

## 🔑 核心優勢

### 1️⃣ 統一的學習曲線

```java
// 你只需要學一次這些方法：
- findById()
- findAll()
- save()
- delete()
- findByXxx()

// 就能操作：PostgreSQL, MongoDB, Redis, ES...
```

### 2️⃣ 輕鬆切換資料庫

```java
// 從 PostgreSQL 切換到 MongoDB
// 只需要：
// 1. 改 pom.xml 依賴
// 2. 改 application.yml 連線
// 3. 程式碼不用改！✨
```

### 3️⃣ 減少重複程式碼

```java
// ❌ 沒有 Spring Data 時
public class UserDao {
    public User findById(Long id) {
        Connection conn = getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE id = ?");
        ps.setLong(1, id);
        ResultSet rs = ps.executeQuery();
        // ... 50 行程式碼解析結果
    }
}

// ✅ 有 Spring Data 時
public interface UserRepository extends CrudRepository<User, Long> {
    // 自動實作！不用寫程式碼！
}
```

---

## 📊 各模組用途對照表

| 模組 | 用途 | 底層資料庫 | 適用場景 |
|-----|------|-----------|---------|
| **Spring Data Commons** | 核心抽象層 | - | 定義統一介面 |
| **Spring Data JPA** | 關聯式資料庫 | PostgreSQL, MySQL, Oracle | 傳統業務系統、交易處理 |
| **Spring Data MongoDB** | 文件資料庫 | MongoDB | 彈性 Schema、大量寫入 |
| **Spring Data Redis** | 鍵值資料庫 | Redis | 快取、Session、計數器 |
| **Spring Data Elasticsearch** | 搜尋引擎 | Elasticsearch | 全文搜尋、日誌分析 |

---

## 💡 聊天系統為什麼用多種？

```
你的 ChatHub 專案：

PostgreSQL (JPA)        → 使用者帳號、好友關係（強一致性）
    ├── users
    ├── friendships
    └── friend_requests

MongoDB                 → 聊天訊息、對話（高寫入、彈性）
    ├── messages
    └── conversations

Redis                   → 線上狀態、快取（極快讀寫）
    ├── user:online:{id}
    └── conversation:messages:{id}
```

**原因：** 不同資料特性，選擇最適合的資料庫！

---

## ✅ 總結一句話

**Spring Data = 統一的資料存取介面，讓你用相同程式碼操作不同資料庫！**

就像「萬用遙控器」，一個遙控器控制電視、冷氣、音響...
你只需要學一次按鈕，就能控制所有家電！🎮