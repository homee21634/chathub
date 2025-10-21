#沒有任何呼叫doFilterInternal的地方，是如何被呼叫的，什麼時候呼叫

# 🔍 doFilterInternal 呼叫機制解析

很好的問題！這是 Spring 的**過濾器鏈（Filter Chain）**機制。

---

## **1️⃣ 呼叫流程**

```
HTTP 請求
    ↓
Tomcat/Servlet 容器
    ↓
Spring Security Filter Chain
    ↓
【JwtAuthenticationFilter】← doFilterInternal() 被呼叫
    ↓
【其他過濾器...】
    ↓
Controller（你的 API）
```

---

## **2️⃣ 誰呼叫 doFilterInternal()？**

### **繼承關係：**
```java
JwtAuthenticationFilter 
    extends OncePerRequestFilter      // ← Spring 提供的抽象類別
        extends GenericFilterBean
            implements Filter          // ← Servlet 標準介面
```

### **實際呼叫者：**

1. **Servlet 容器**呼叫 `Filter.doFilter()`
2. **OncePerRequestFilter** 實作了 `doFilter()`
3. `OncePerRequestFilter.doFilter()` 內部呼叫 **`doFilterInternal()`**

---

## **3️⃣ OncePerRequestFilter 原始碼（簡化版）**

```java
public abstract class OncePerRequestFilter extends GenericFilterBean {
    
    @Override
    public final void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // 檢查是否應該跳過此過濾器
        if (shouldNotFilter(httpRequest)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 確保每個請求只執行一次
        if (alreadyFiltered(httpRequest)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // ✅ 呼叫我們實作的方法
        doFilterInternal(httpRequest, httpResponse, filterChain);
    }
    
    // 我們必須實作的抽象方法
    protected abstract void doFilterInternal(
        HttpServletRequest request, 
        HttpServletResponse response, 
        FilterChain filterChain
    ) throws ServletException, IOException;
}
```

---

## **4️⃣ 什麼時候被呼叫？**

### **每個 HTTP 請求都會觸發：**

```
GET /api/v1/test/protected  → doFilterInternal() 執行
POST /api/v1/auth/login     → doFilterInternal() 執行
PUT /api/v1/friends/123     → doFilterInternal() 執行
```

### **但是有例外（shouldNotFilter）：**

```java
@Override
protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    
    // ✅ 這些路徑會跳過過濾器
    return path.startsWith("/api/v1/auth/register") ||
           path.startsWith("/api/v1/auth/login") ||
           path.startsWith("/api/v1/auth/refresh");
}
```

---

## **5️⃣ 過濾器執行順序**

Spring Security 有多個內建過濾器，執行順序：

```
1. SecurityContextPersistenceFilter
2. LogoutFilter
3. UsernamePasswordAuthenticationFilter
4. 【JwtAuthenticationFilter】← 我們加在這裡之前
5. FilterSecurityInterceptor（檢查權限）
6. Controller
```

### **我們的配置：**
```java
.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```

這表示 JWT 過濾器在**第 3 步之前**執行。

---

## **6️⃣ filterChain.doFilter() 的作用**

```java
filterChain.doFilter(request, response);
```

**這行很重要！** 它的意思是：

- ✅ **繼續執行下一個過濾器**
- ✅ 最終到達 Controller

### **如果忘記呼叫會怎樣？**

```java
@Override
protected void doFilterInternal(...) {
    // 驗證 Token
    // ...
    
    // ❌ 忘記呼叫這行
    // filterChain.doFilter(request, response);
}
```

**結果：請求卡住，永遠到不了 Controller！**

---

## **7️⃣ 實際執行範例**

### **請求：**
```http
GET /api/v1/test/protected
Authorization: Bearer eyJhbGci...
```

### **執行流程：**

```java
// Step 1: Servlet 容器呼叫 doFilter()
OncePerRequestFilter.doFilter() {
    
    // Step 2: 檢查是否跳過
    if (shouldNotFilter()) {  // false，不跳過
        // 不執行
    }
    
    // Step 3: 呼叫我們的方法
    doFilterInternal(request, response, filterChain) {
        
        // Step 4: 提取 Token
        String token = extractTokenFromRequest(request);
        
        // Step 5: 驗證 Token
        if (jwtTokenProvider.validateToken(token)) {
            // Step 6: 載入使用者資訊
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        
        // Step 7: 繼續下一個過濾器
        filterChain.doFilter(request, response);  // ← 關鍵！
    }
}

// Step 8: 到達 Controller
TestController.testProtectedEndpoint() {
    // 處理請求
}
```

---

## **8️⃣ OncePerRequestFilter 的優點**

### **為什麼用 OncePerRequestFilter？**

1. **保證每個請求只執行一次**
    - 防止 forward/include 導致重複執行

2. **自動轉型為 HttpServletRequest**
    - 不用手動 cast

3. **提供 shouldNotFilter() 鉤子**
    - 可以輕鬆跳過特定路徑

---

## **9️⃣ 對比：不使用 OncePerRequestFilter**

如果直接實作 `Filter` 介面：

```java
@Component
public class JwtAuthenticationFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        // ❌ 需要手動轉型
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        // ❌ 需要手動處理重複執行問題
        
        // ❌ 需要手動判斷路徑
        
        // 驗證邏輯...
        
        chain.doFilter(request, response);
    }
}
```

**使用 OncePerRequestFilter 更簡潔！**

---

## **📌 總結**

1. **誰呼叫？** Servlet 容器 → OncePerRequestFilter.doFilter() → 我們的 doFilterInternal()
2. **何時呼叫？** 每個 HTTP 請求（除了 shouldNotFilter 排除的）
3. **為什麼看不到？** Spring 框架自動管理過濾器鏈
4. **關鍵：** 必須呼叫 `filterChain.doFilter()` 讓請求繼續

---

**理解了嗎？還有其他問題或繼續下一步？** 🚀