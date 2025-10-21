# 🐳 使用 Docker 登入 Redis CLI

---

## **方法 1: 進入容器執行 redis-cli（推薦）**

### **步驟：**

```bash
# 1. 查看 Redis 容器名稱
docker ps | grep redis

# 2. 進入容器並執行 redis-cli
docker exec -it <容器名稱> redis-cli

# 範例
docker exec -it redis redis-cli
```

---

## **進入後的常用指令**

### **查看 Token 黑名單：**

```bash
# 1. 先列出所有 token:blacklist 開頭的 key
KEYS token:blacklist:*

# 2. 查看特定黑名單 Token（複製上面的 key）
GET token:blacklist:a5b6c7d8-e9f0-1234-5678-90abcdef1234

# 3. 查看 TTL（剩餘秒數）
TTL token:blacklist:a5b6c7d8-e9f0-1234-5678-90abcdef1234
```

---

### **查看線上狀態：**

```bash
# 列出所有線上使用者
KEYS user:online:*

# 查看特定使用者（替換成你的 userId）
GET user:online:1c248f01-ab6d-4814-9e0c-5a45cfb7ceb3

# 查看 TTL
TTL user:online:1c248f01-ab6d-4814-9e0c-5a45cfb7ceb3
```

---

### **查看登入失敗記錄：**

```bash
# 列出所有登入失敗記錄
KEYS login:attempt:*

# 查看特定使用者的失敗次數
GET login:attempt:testuser
```

---

### **其他常用指令：**

```bash
# 查看所有 key
KEYS *

# 查看 Redis 資訊
INFO

# 清空當前資料庫（小心使用！）
FLUSHDB

# 離開
exit
```

---

## **方法 2: 不進入容器，直接執行指令**

```bash
# 單次執行指令
docker exec -it redis redis-cli KEYS "token:blacklist:*"
docker exec -it redis redis-cli GET "user:online:1c248f01-ab6d-4814-9e0c-5a45cfb7ceb3"
```

---

## **🧪 完整驗證流程**

### **1. 登入取得 Token**
```http
POST http://localhost:8080/api/v1/auth/login
```

**記下 `accessToken` 和 `userId`**

---

### **2. 查看線上狀態（登入後）**

```bash
docker exec -it redis redis-cli

# 進入後執行
GET user:online:1c248f01-ab6d-4814-9e0c-5a45cfb7ceb3
# 應該顯示: "true"

TTL user:online:1c248f01-ab6d-4814-9e0c-5a45cfb7ceb3
# 應該顯示: (integer) 3599 左右（1小時 = 3600秒）
```

---

### **3. 登出**

```http
POST http://localhost:8080/api/v1/auth/logout
Authorization: Bearer {剛才的 accessToken}
```

---

### **4. 查看線上狀態（登出後）**

```bash
GET user:online:1c248f01-ab6d-4814-9e0c-5a45cfb7ceb3
# 應該顯示: (nil) - 表示已被刪除
```

---

### **5. 查看 Token 黑名單**

```bash
# 列出所有黑名單 Token
KEYS token:blacklist:*
# 應該顯示: 1) "token:blacklist:{jti}"

# 查看內容
GET token:blacklist:{剛才列出的 jti}
# 應該顯示: "true"

# 查看剩餘時間
TTL token:blacklist:{jti}
# 應該顯示: (integer) 3598 左右
```

---

### **6. 驗證 Token 已失效**

```http
GET http://localhost:8080/api/v1/test/protected
Authorization: Bearer {剛才登出的 accessToken}
```

**應該回傳 403 Forbidden**

---

## **📌 如何取得 JTI？**

JTI 在 Token 中，需要解碼才能看到。

### **方法 1: 使用 jwt.io**
1. 前往 https://jwt.io
2. 貼上你的 `accessToken`
3. 在 Payload 中看到 `jti` 欄位

### **方法 2: 用程式解碼（測試用）**

在 `AuthController` 加個測試端點：

```java
@GetMapping("/decode-token")
public ResponseEntity<?> decodeToken(@RequestParam String token) {
    Claims claims = jwtTokenProvider.getClaimsFromToken(token);
    return ResponseEntity.ok(claims);
}
```

然後呼叫：
```http
GET http://localhost:8080/api/v1/auth/decode-token?token={accessToken}
```

---

## **容器名稱不確定？**

```bash
# 列出所有容器
docker ps

# 只看 Redis
docker ps | grep redis

# 輸出範例：
# CONTAINER ID   IMAGE         PORTS                    NAMES
# abc123def456   redis:7.0     0.0.0.0:6379->6379/tcp   redis
```

使用 **NAMES** 欄位的值（例如 `redis`）

---

**現在去驗證 Redis 吧！完成後回報結果！** 🚀