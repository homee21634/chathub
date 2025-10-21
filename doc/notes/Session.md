# 📦 Session 詳解

Session 是伺服器用來**記住使用者狀態**的機制，讓 HTTP（本身無狀態）變成有狀態。

---

## **1️⃣ Session 是什麼？**

### **問題：HTTP 是無狀態的**

```
使用者 → 請求 1：登入成功
使用者 → 請求 2：查看個人資料
```

**問題：** 伺服器怎麼知道請求 2 是剛才登入的那個使用者？

**解決方案：Session！**

---

### **Session 運作方式：**

```
1. 使用者登入成功
    ↓
2. 伺服器建立 Session（儲存使用者資訊）
   Session ID: ABC123
   Data: { userId: "1c248f01...", username: "alice" }
    ↓
3. 伺服器回傳 Cookie
   Set-Cookie: JSESSIONID=ABC123
    ↓
4. 瀏覽器儲存 Cookie
    ↓
5. 後續請求自動帶上 Cookie
   Cookie: JSESSIONID=ABC123
    ↓
6. 伺服器用 Session ID 找回使用者資訊
```

---

## **2️⃣ Session 的儲存位置**

### **伺服器端儲存：**

```java
// 簡化版的 Session 儲存
Map<String, Map<String, Object>> sessionStore = new HashMap<>();

// 建立 Session
String sessionId = UUID.randomUUID().toString(); // "ABC123"
Map<String, Object> sessionData = new HashMap<>();
sessionData.put("userId", "1c248f01...");
sessionData.put("username", "alice");
sessionStore.put(sessionId, sessionData);

// 查詢 Session
Map<String, Object> data = sessionStore.get("ABC123");
String userId = (String) data.get("userId");
```

---

### **實際儲存位置：**

| 方式 | 說明 | 優點 | 缺點 |
|-----|------|------|------|
| **記憶體** | 儲存在應用程式記憶體中 | 快速 | 重啟遺失、無法水平擴展 |
| **Redis** | 儲存在 Redis | 快速、可共享 | 需要額外服務 |
| **資料庫** | 儲存在 PostgreSQL/MySQL | 持久化 | 較慢 |
| **檔案系統** | 儲存在硬碟 | 持久化 | 很慢 |

---

## **3️⃣ Session 完整流程**

### **A. 登入時建立 Session**

```java
@PostMapping("/login")
public String login(HttpServletRequest request, 
                    @RequestParam String username,
                    @RequestParam String password) {
    
    // 驗證帳號密碼
    User user = userService.authenticate(username, password);
    
    // 建立 Session
    HttpSession session = request.getSession(true); // true = 建立新 Session
    session.setAttribute("userId", user.getUserId());
    session.setAttribute("username", user.getUsername());
    
    return "redirect:/home";
}
```

**伺服器內部：**
```java
// 1. 生成 Session ID
String sessionId = generateSessionId(); // "ABC123"

// 2. 儲存 Session 資料
sessionStore.put("ABC123", {
    "userId": "1c248f01...",
    "username": "alice",
    "createdAt": 1729742400
});

// 3. 回傳 Set-Cookie Header
response.setHeader("Set-Cookie", "JSESSIONID=ABC123; HttpOnly; Path=/");
```

---

### **B. 後續請求使用 Session**

```java
@GetMapping("/profile")
public String getProfile(HttpServletRequest request) {
    
    // 從 Session 取得使用者資訊
    HttpSession session = request.getSession(false); // false = 不建立新的
    
    if (session == null) {
        return "redirect:/login"; // 未登入
    }
    
    String userId = (String) session.getAttribute("userId");
    User user = userService.findById(userId);
    
    return "profile";
}
```

**伺服器內部：**
```java
// 1. 從 Cookie 讀取 Session ID
String sessionId = request.getCookie("JSESSIONID"); // "ABC123"

// 2. 從 Session Store 取得資料
Map<String, Object> sessionData = sessionStore.get("ABC123");

// 3. 回傳使用者資訊
String userId = sessionData.get("userId");
```

---

### **C. 登出時銷毀 Session**

```java
@PostMapping("/logout")
public String logout(HttpServletRequest request) {
    
    HttpSession session = request.getSession(false);
    if (session != null) {
        session.invalidate(); // 銷毀 Session
    }
    
    return "redirect:/login";
}
```

**伺服器內部：**
```java
// 1. 從 Session Store 移除
sessionStore.remove("ABC123");

// 2. 回傳刪除 Cookie 的 Header
response.setHeader("Set-Cookie", "JSESSIONID=; Max-Age=0");
```

---

## **4️⃣ Session 在 Spring Security 中的角色**

### **傳統方式（使用 Session）：**

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
            .formLogin(form -> form.loginPage("/login"))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                // ↑ 需要時建立 Session
            );
        
        return http.build();
    }
}
```

**登入後：**
```java
// Spring Security 自動將認證資訊存入 Session
SecurityContext context = SecurityContextHolder.getContext();
context.setAuthentication(authentication);

// Session 儲存的內容
session.setAttribute("SPRING_SECURITY_CONTEXT", context);
```

---

### **我們的方式（無 Session）：**

```java
http
    .sessionManagement(session -> session
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        // ↑ 完全不使用 Session
    );
```

**原因：**
- ✅ JWT Token 本身就包含使用者資訊
- ✅ 不需要伺服器儲存狀態
- ✅ 適合水平擴展

---

## **5️⃣ Session vs JWT 對比**

| 比較項目 | Session | JWT |
|---------|---------|-----|
| **儲存位置** | 伺服器（記憶體/Redis） | 客戶端（localStorage） |
| **狀態** | 有狀態 | 無狀態 |
| **伺服器負擔** | 需要儲存 Session | 不需要儲存 |
| **水平擴展** | 困難（需同步 Session） | 容易（任何伺服器都能驗證） |
| **安全性** | Session ID 不包含資訊 | Token 包含資訊（可解碼） |
| **登出** | 刪除 Session | Token 加入黑名單 |
| **過期** | 伺服器控制 | Token 本身有過期時間 |

---

## **6️⃣ Session 的問題**

### **A. 記憶體消耗**

```
10,000 使用者線上
每個 Session 1KB
= 10MB 記憶體

1,000,000 使用者線上
= 1GB 記憶體！
```

---

### **B. 水平擴展困難**

```
使用者登入 → Server 1 建立 Session
    ↓
使用者下次請求 → Load Balancer → Server 2
    ↓
Server 2 找不到 Session！
```

**解決方案：**
1. **Sticky Session**（綁定伺服器）- 不夠彈性
2. **Session 共享**（Redis）- 需要額外服務

---

### **C. 跨域問題**

```
網站：example.com
API：api.example.com

Cookie 預設只送給同域名
→ 跨域請求無法使用 Session
```

---

## **7️⃣ 實際 Session 範例**

### **傳統 Session 登入流程：**

```
1. 使用者輸入帳號密碼
    ↓
2. POST /login
   username=alice&password=secret
    ↓
3. 伺服器驗證
    ↓
4. 建立 Session
   Server Memory: {
     "ABC123": {
       "userId": "1c248f01...",
       "username": "alice",
       "roles": ["USER"]
     }
   }
    ↓
5. 回傳 Cookie
   HTTP/1.1 200 OK
   Set-Cookie: JSESSIONID=ABC123; HttpOnly; Secure
    ↓
6. 瀏覽器儲存 Cookie
    ↓
7. 使用者訪問 /profile
   GET /profile
   Cookie: JSESSIONID=ABC123
    ↓
8. 伺服器查詢 Session
   sessionData = sessionStore.get("ABC123")
   userId = sessionData.get("userId")
    ↓
9. 回傳使用者資料
```

---

## **8️⃣ Spring Session 與 Redis**

如果要在多台伺服器共享 Session：

```java
// 依賴
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-data-redis</artifactId>
</dependency>

// 配置
@EnableRedisHttpSession
public class SessionConfig {
}
```

**效果：**
```
Server 1 建立 Session → 儲存到 Redis
Server 2 讀取 Session → 從 Redis 讀取
```

---

## **9️⃣ Cookie 與 Session 的關係**

### **Cookie 是 Session ID 的傳輸工具：**

```
Server: 建立 Session，生成 ID = "ABC123"
    ↓
Server: 透過 Cookie 傳給客戶端
   Set-Cookie: JSESSIONID=ABC123
    ↓
Browser: 儲存 Cookie
    ↓
Browser: 後續請求自動帶上
   Cookie: JSESSIONID=ABC123
    ↓
Server: 用 Session ID 查詢使用者資訊
```

**沒有 Cookie，Session 機制無法運作！**

---

## **🔟 為什麼我們不用 Session？**

### **JWT 的優勢：**

```java
// Session 方式
POST /login → 建立 Session → 伺服器儲存
GET /profile → 查詢 Session → 從記憶體讀取

// JWT 方式
POST /login → 生成 Token → 回傳給客戶端
GET /profile → 驗證 Token → 從 Token 提取資訊（不查詢資料庫）
```

**JWT 優點：**
- ✅ 伺服器不儲存任何狀態（省記憶體）
- ✅ 任何伺服器都能驗證 Token（易擴展）
- ✅ 適合 REST API（無狀態設計）
- ✅ 適合微服務架構

---

## **📌 總結**

**Session 是：**
1. **伺服器端儲存** - 使用者資訊存在伺服器
2. **透過 Cookie 識別** - JSESSIONID 在客戶端
3. **有狀態機制** - 伺服器需要記住使用者
4. **傳統 Web 應用** - 適合單體應用

**JWT 是：**
1. **客戶端儲存** - Token 存在客戶端
2. **透過 Header 傳遞** - Authorization: Bearer {token}
3. **無狀態機制** - 伺服器不需要記住使用者
4. **現代 API** - 適合微服務、前後端分離

**我們選擇 JWT 因為：**
- 專案是 REST API
- 需要水平擴展（Kubernetes）
- 前後端分離架構
- 更符合現代 Web 開發

---

**理解了嗎？繼續下一個功能？** 🚀