# 📖 Claims 物件詳解

`Claims` 是 JWT Token 的**有效載荷（Payload）**，包含 Token 的所有資訊。

---

## **1️⃣ Claims 是什麼？**

JWT Token 結構分三部分：

```
Header.Payload.Signature
```

**Claims 就是 Payload 的內容**，以 JSON 格式儲存資料。

---

## **2️⃣ JWT Token 範例**

### **完整 Token：**
```
eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxYzI0OGYwMS1hYjZkLTQ4MTQtOWUwYy01YTQ1Y2ZiN2NlYjMiLCJ1c2VybmFtZSI6InRlc3R1c2VyIiwidHlwZSI6ImFjY2VzcyIsImp0aSI6ImE1YjZjN2Q4LWU5ZjAtMTIzNC01Njc4LTkwYWJjZGVmMTIzNCIsImlhdCI6MTcyOTc0MjQwMCwiZXhwIjoxNzI5NzQ2MDAwfQ.signature...
```

### **解碼後的 Payload（Claims）：**
```json
{
  "sub": "1c248f01-ab6d-4814-9e0c-5a45cfb7ceb3",
  "username": "testuser",
  "type": "access",
  "jti": "a5b6c7d8-e9f0-1234-5678-90abcdef1234",
  "iat": 1729742400,
  "exp": 1729746000
}
```

---

## **3️⃣ 標準 Claims（JWT 規範定義）**

| Claim | 方法 | 說明 | 範例 |
|-------|------|------|------|
| **sub** | `getSubject()` | 主體（通常是使用者 ID） | `"1c248f01..."` |
| **iat** | `getIssuedAt()` | 簽發時間 | `1729742400` |
| **exp** | `getExpiration()` | 過期時間 | `1729746000` |
| **jti** | `getId()` | JWT ID（唯一識別碼） | `"a5b6c7d8..."` |
| **iss** | `getIssuer()` | 簽發者 | `"ChatHub"` |
| **aud** | `getAudience()` | 接收者 | `"web-app"` |

---

## **4️⃣ 自訂 Claims（我們專案中的）**

| Claim | 取得方法 | 說明 |
|-------|----------|------|
| **username** | `get("username", String.class)` | 使用者名稱 |
| **type** | `get("type", String.class)` | Token 類型（access/refresh） |

---

## **5️⃣ 如何使用 Claims**

### **在過濾器中：**
```java
Claims claims = jwtTokenProvider.getClaimsFromToken(token);

// 標準 Claims
String userId = claims.getSubject();           // sub
Date issuedAt = claims.getIssuedAt();         // iat
Date expiration = claims.getExpiration();     // exp
String jti = claims.getId();                  // jti

// 自訂 Claims
String username = claims.get("username", String.class);
String type = claims.get("type", String.class);
```

### **檢查 Token 類型：**
```java
if (!"access".equals(tokenType)) {
    // 拒絕 Refresh Token 訪問 API
}
```

### **計算剩餘時間：**
```java
long remainingMillis = claims.getExpiration().getTime() - System.currentTimeMillis();
boolean expiringSoon = remainingMillis < 5 * 60 * 1000; // 少於 5 分鐘
```

---

## **6️⃣ 為什麼需要 Claims？**

### **✅ 無狀態認證**
- 伺服器不需要儲存 Session
- 所有資訊都在 Token 中

### **✅ 攜帶使用者資訊**
- `sub`: 使用者 ID
- `username`: 使用者名稱
- 可加入角色、權限等

### **✅ 安全控制**
- `exp`: 過期時間（防止 Token 永久有效）
- `jti`: 唯一 ID（黑名單功能）
- `type`: 區分 Access Token 與 Refresh Token

---

## **7️⃣ Claims 的安全性**

### **⚠️ 注意事項：**
1. **Claims 是 Base64 編碼，不是加密**
    - 任何人都能解碼查看內容
    - **絕不放敏感資訊**（密碼、信用卡號等）

2. **簽章保證完整性**
    - 修改 Claims 會導致簽章驗證失敗
    - 防止 Token 被竄改

3. **適合放入的資訊：**
    - ✅ 使用者 ID
    - ✅ 使用者名稱
    - ✅ 角色/權限
    - ❌ 密碼
    - ❌ 敏感個資

---

## **8️⃣ 實際範例：在 Controller 中使用**

```java
@GetMapping("/profile")
public ResponseEntity<?> getProfile() {
    // 從 SecurityContext 取得 Authentication
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    
    // Principal 就是 Claims 的 subject（使用者 ID）
    String userId = (String) auth.getPrincipal();
    
    // Details 是我們設定的 username
    String username = (String) auth.getDetails();
    
    // 使用這些資訊查詢資料庫
    User user = userService.findById(UUID.fromString(userId));
    
    return ResponseEntity.ok(user);
}
```

---

## **📌 總結**

**Claims 就是 JWT Token 中的資料包**，包含：
- 標準欄位（sub, exp, iat, jti）
- 自訂欄位（username, type, roles）
- 用於無狀態認證與授權

**記住：**
- ✅ Claims 可被讀取（Base64 編碼）
- ✅ Claims 不可被竄改（有簽章保護）
- ❌ 不要放敏感資訊

---

**還有其他問題嗎？或繼續下一步？** 🚀