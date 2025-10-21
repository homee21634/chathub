# 🖥️ Servlet 容器詳解

Servlet 容器是 Java Web 應用的**執行環境**，負責管理 Servlet 的生命週期和處理 HTTP 請求。

---

## **1️⃣ Servlet 容器是什麼？**

### **簡單比喻：**
- **Servlet 容器** = 餐廳經理
- **Servlet/Controller** = 廚師
- **HTTP 請求** = 客人點餐

**經理負責：**
- 接待客人（接收 HTTP 請求）
- 分配廚師（呼叫對應的 Servlet）
- 送餐給客人（回傳 HTTP 回應）
- 管理廚師班表（Servlet 生命週期）

---

## **2️⃣ 常見的 Servlet 容器**

| 容器 | 說明 | 使用場景 |
|-----|------|---------|
| **Tomcat** | Apache 開發，最常用 | Spring Boot 預設 |
| **Jetty** | 輕量級、快速啟動 | 嵌入式應用 |
| **Undertow** | 高效能、非阻塞 I/O | 微服務 |
| **WebLogic** | Oracle 商業產品 | 企業級應用 |
| **WebSphere** | IBM 商業產品 | 大型企業 |

**Spring Boot 預設使用 Tomcat！**

---

## **3️⃣ Servlet 容器的角色**

### **完整請求流程：**

```
使用者瀏覽器
    ↓ HTTP 請求
    ↓
┌─────────────────────────────────┐
│   Servlet 容器 (Tomcat)         │
│                                  │
│  1. 接收 Socket 連線            │
│  2. 解析 HTTP 請求              │
│  3. 建立 HttpServletRequest     │
│  4. 建立 HttpServletResponse    │
│  5. 呼叫 Filter Chain           │
│     ↓                            │
│  6. 呼叫 Servlet/Controller     │
│     ↓                            │
│  7. 取得回應結果                │
│  8. 封裝成 HTTP Response        │
│  9. 透過 Socket 傳回瀏覽器      │
└─────────────────────────────────┘
    ↓ HTTP 回應
    ↓
使用者瀏覽器
```

---

## **4️⃣ Servlet 容器的核心功能**

### **A. 生命週期管理**

```java
public class MyServlet extends HttpServlet {
    
    // 1. Servlet 啟動時呼叫（只執行一次）
    @Override
    public void init() throws ServletException {
        System.out.println("Servlet 初始化");
    }
    
    // 2. 每個請求都會呼叫
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) {
        System.out.println("處理請求");
    }
    
    // 3. Servlet 關閉時呼叫（只執行一次）
    @Override
    public void destroy() {
        System.out.println("Servlet 銷毀");
    }
}
```

**Servlet 容器負責：**
- ✅ 決定何時呼叫 `init()`
- ✅ 為每個請求呼叫 `service()`
- ✅ 應用關閉時呼叫 `destroy()`

---

### **B. 多執行緒管理**

```
使用者 A 請求 → Thread 1 → MyServlet.service()
使用者 B 請求 → Thread 2 → MyServlet.service()  ← 同時執行
使用者 C 請求 → Thread 3 → MyServlet.service()
```

**Servlet 容器負責：**
- ✅ 建立執行緒池
- ✅ 為每個請求分配執行緒
- ✅ 回收執行緒

**這就是為什麼 Servlet 是單例（Singleton）！**

---

### **C. 請求/回應物件管理**

```java
// Servlet 容器建立這些物件
HttpServletRequest request = new HttpServletRequestImpl();
request.setMethod("POST");
request.setRequestURI("/api/v1/auth/login");
request.setHeader("Content-Type", "application/json");
request.setBody("{\"username\":\"test\"}");

HttpServletResponse response = new HttpServletResponseImpl();

// 傳給 Filter 和 Servlet
filterChain.doFilter(request, response);
```

---

### **D. Session 管理**

```java
HttpSession session = request.getSession();
session.setAttribute("userId", "123");
```

**Servlet 容器負責：**
- ✅ 產生 Session ID
- ✅ 儲存 Session 資料
- ✅ Session 過期清理

---

## **5️⃣ Spring Boot 與 Servlet 容器**

### **嵌入式 Tomcat**

```java
@SpringBootApplication
public class ChatSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatSystemApplication.class, args);
        // ↑ 這行會啟動嵌入式 Tomcat
    }
}
```

**啟動過程：**
```
1. Spring Boot 啟動
2. 建立嵌入式 Tomcat 實例
3. 配置 Tomcat（port 8080）
4. 註冊所有 Servlet、Filter、Listener
5. 啟動 Tomcat（開始監聽 8080 port）
```

---

### **查看 Tomcat 資訊**

啟動應用時會看到：

```
Tomcat initialized with port 8080 (http)
Starting service [Tomcat]
Starting Servlet engine: [Apache Tomcat/10.1.x]
Tomcat started on port 8080 (http) with context path ''
```

---

## **6️⃣ Servlet 容器 vs Spring 容器**

**兩個不同的容器！**

| 比較項目 | Servlet 容器（Tomcat） | Spring 容器（IoC） |
|---------|----------------------|-------------------|
| **負責內容** | HTTP 請求處理 | Bean 管理 |
| **管理對象** | Servlet、Filter | Controller、Service |
| **生命週期** | init/service/destroy | 依賴注入、AOP |
| **啟動時機** | 應用啟動時 | 應用啟動時 |

### **關係：**
```
Servlet 容器（Tomcat）
    ↓ 接收 HTTP 請求
    ↓
Spring MVC DispatcherServlet
    ↓
Spring 容器（找到對應的 Controller Bean）
    ↓
你的 Controller 方法
```

---

## **7️⃣ 沒有 Servlet 容器會怎樣？**

### **問題：**
```java
public static void main(String[] args) {
    // 只有這行，沒有 Servlet 容器
    AuthController controller = new AuthController();
}
```

**無法：**
- ❌ 接收 HTTP 請求
- ❌ 解析 JSON
- ❌ 處理路由（/api/v1/auth/login）
- ❌ 回傳 HTTP 回應

**需要 Servlet 容器來處理這些！**

---

## **8️⃣ 過濾器在 Servlet 容器中的位置**

```
HTTP 請求到達
    ↓
Servlet 容器（Tomcat）
    ↓
【Filter 1】 ← JwtAuthenticationFilter
    ↓
【Filter 2】
    ↓
【Servlet】 ← DispatcherServlet (Spring MVC)
    ↓
Controller
```

**我們的 JWT 過濾器就是在這裡被呼叫！**

---

## **9️⃣ 實際範例：Tomcat 處理請求**

### **請求：**
```http
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{"username":"test","password":"Test1234!"}
```

### **Tomcat 的工作：**

```java
// 1. 接收 Socket 連線
Socket clientSocket = serverSocket.accept();

// 2. 讀取 HTTP 請求
BufferedReader in = new BufferedReader(
    new InputStreamReader(clientSocket.getInputStream())
);

// 3. 解析 HTTP
String line = in.readLine(); // "POST /api/v1/auth/login HTTP/1.1"
Map<String, String> headers = parseHeaders(in);
String body = readBody(in);

// 4. 建立 Servlet 物件
HttpServletRequest request = new HttpServletRequestImpl();
request.setMethod("POST");
request.setRequestURI("/api/v1/auth/login");
request.setBody(body);

HttpServletResponse response = new HttpServletResponseImpl();

// 5. 呼叫 Filter Chain
filterChain.doFilter(request, response);

// 6. 取得回應
String responseBody = response.getBody();
int statusCode = response.getStatus();

// 7. 封裝 HTTP 回應
PrintWriter out = new PrintWriter(clientSocket.getOutputStream());
out.println("HTTP/1.1 " + statusCode + " OK");
out.println("Content-Type: application/json");
out.println();
out.println(responseBody);

// 8. 關閉連線
clientSocket.close();
```

---

## **🔟 Spring Boot 如何配置 Tomcat**

### **application.yml：**
```yaml
server:
  port: 8080                    # Tomcat 監聽的 port
  tomcat:
    threads:
      max: 200                  # 最大執行緒數
      min-spare: 10             # 最小空閒執行緒
    max-connections: 8192       # 最大連線數
    accept-count: 100           # 等待佇列長度
```

---

## **📌 總結**

**Servlet 容器（Tomcat）是：**
1. **Web Server** - 處理 HTTP 請求/回應
2. **執行環境** - 運行 Servlet/Filter/Controller
3. **生命週期管理者** - 管理 Servlet 的 init/service/destroy
4. **多執行緒管理者** - 為每個請求分配執行緒
5. **Spring Boot 的底層** - 嵌入式 Tomcat 讓應用可獨立運行

**關鍵：**
- Servlet 容器負責 HTTP 層面
- Spring 容器負責業務邏輯層面
- 兩者協同工作，打造完整的 Web 應用

---

**理解了嗎？繼續下一步還是有其他問題？** 🚀