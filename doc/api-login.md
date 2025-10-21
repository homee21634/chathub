# 登入 API 文件

**版本：** 1.0  
**最後更新：** 2025-09-30

---

## API 列表

1. [使用者登入](#1-使用者登入)
2. [刷新 Token](#2-刷新-token)
3. [使用者登出](#3-使用者登出)
4. [驗證 Token](#4-驗證-token)

---

## 1. 使用者登入

### 基本資訊

- **端點：** `POST /api/v1/auth/login`
- **描述：** 使用者登入並取得 JWT Token
- **需要認證：** 否

### 請求標頭

```
Content-Type: application/json
```

### 請求主體 (Request Body)

```json
{
  "username": "string",
  "password": "string",
  "rememberMe": boolean
}
```

### 請求欄位說明

| 欄位名稱 | 類型 | 必填 | 說明 | 預設值 |
|---------|------|------|------|-------|
| username | String | 是 | 使用者帳號 | - |
| password | String | 是 | 使用者密碼 | - |
| rememberMe | Boolean | 否 | 是否記住登入狀態（7天） | false |

### 請求範例

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "Test@1234",
    "rememberMe": true
  }'
```

### 成功回應 (200 OK)

```json
{
  "success": true,
  "message": "登入成功",
  "data": {
    "user": {
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "username": "john_doe",
      "lastLoginAt": "2025-09-30T10:30:00Z"
    },
    "tokens": {
      "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "tokenType": "Bearer",
      "expiresIn": 3600
    }
  }
}
```

### 成功回應欄位說明

| 欄位路徑 | 類型 | 說明 |
|---------|------|------|
| success | Boolean | 請求是否成功 |
| message | String | 回應訊息 |
| data.user.userId | String (UUID) | 使用者唯一識別碼 |
| data.user.username | String | 使用者帳號 |
| data.user.lastLoginAt | String (ISO 8601) | 最後登入時間 |
| data.tokens.accessToken | String (JWT) | 存取權杖（有效期 1 小時） |
| data.tokens.refreshToken | String (JWT) | 刷新權杖（7天 或 24小時） |
| data.tokens.tokenType | String | Token 類型（固定為 "Bearer"） |
| data.tokens.expiresIn | Integer | Access Token 過期時間（秒） |

### Token 有效期說明

| rememberMe | Access Token | Refresh Token |
|-----------|--------------|---------------|
| true | 1 小時 | 7 天 |
| false | 1 小時 | 24 小時 |

### 錯誤回應

#### 400 Bad Request - 驗證失敗

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

#### 401 Unauthorized - 帳號或密碼錯誤

```json
{
  "success": false,
  "message": "帳號或密碼錯誤"
}
```

#### 403 Forbidden - 帳號已被停用

```json
{
  "success": false,
  "message": "此帳號已被停用，請聯絡管理員"
}
```

#### 429 Too Many Requests - 登入嘗試次數過多

```json
{
  "success": false,
  "message": "登入失敗次數過多，請在 15 分鐘後再試",
  "retryAfter": 900
}
```

### HTTP 狀態碼

| 狀態碼 | 說明 |
|-------|------|
| 200 | 登入成功 |
| 400 | 請求參數驗證失敗 |
| 401 | 帳號或密碼錯誤 |
| 403 | 帳號已被停用 |
| 429 | 登入嘗試次數過多 |
| 500 | 伺服器內部錯誤 |

---

## 2. 刷新 Token

### 基本資訊

- **端點：** `POST /api/v1/auth/refresh`
- **描述：** 使用 Refresh Token 取得新的 Access Token
- **需要認證：** 否（但需提供有效的 Refresh Token）

### 請求標頭

```
Content-Type: application/json
```

### 請求主體 (Request Body)

```json
{
  "refreshToken": "string"
}
```

### 請求欄位說明

| 欄位名稱 | 類型 | 必填 | 說明 |
|---------|------|------|------|
| refreshToken | String (JWT) | 是 | Refresh Token |

### 請求範例

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'
```

### 成功回應 (200 OK)

```json
{
  "success": true,
  "message": "Token 刷新成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

### 成功回應欄位說明

| 欄位路徑 | 類型 | 說明 |
|---------|------|------|
| success | Boolean | 請求是否成功 |
| message | String | 回應訊息 |
| data.accessToken | String (JWT) | 新的存取權杖 |
| data.tokenType | String | Token 類型 |
| data.expiresIn | Integer | Token 過期時間（秒） |

### 錯誤回應

#### 401 Unauthorized - Token 無效或過期

```json
{
  "success": false,
  "message": "Refresh Token 無效或已過期，請重新登入"
}
```

### HTTP 狀態碼

| 狀態碼 | 說明 |
|-------|------|
| 200 | 刷新成功 |
| 401 | Token 無效或已過期 |
| 500 | 伺服器內部錯誤 |

---

## 3. 使用者登出

### 基本資訊

- **端點：** `POST /api/v1/auth/logout`
- **描述：** 登出使用者，使 Token 失效
- **需要認證：** 是

### 請求標頭

```
Authorization: Bearer {access-token}
Content-Type: application/json
```

### 請求主體

無需 Request Body（從 Token 中取得使用者資訊）

### 請求範例

```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json"
```

### 成功回應 (200 OK)

```json
{
  "success": true,
  "message": "登出成功"
}
```

### 成功回應欄位說明

| 欄位名稱 | 類型 | 說明 |
|---------|------|------|
| success | Boolean | 請求是否成功 |
| message | String | 回應訊息 |

### 錯誤回應

#### 401 Unauthorized - Token 無效

```json
{
  "success": false,
  "message": "未授權，請重新登入"
}
```

### HTTP 狀態碼

| 狀態碼 | 說明 |
|-------|------|
| 200 | 登出成功 |
| 401 | Token 無效或未提供 |
| 500 | 伺服器內部錯誤 |

---

## 4. 驗證 Token

### 基本資訊

- **端點：** `GET /api/v1/auth/verify`
- **描述：** 驗證當前 Token 是否有效
- **需要認證：** 是

### 請求標頭

```
Authorization: Bearer {access-token}
```

### 請求範例

```bash
curl -X GET http://localhost:8080/api/v1/auth/verify \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 成功回應 (200 OK)

```json
{
  "success": true,
  "valid": true,
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "username": "john_doe"
  }
}
```

### 成功回應欄位說明

| 欄位路徑 | 類型 | 說明 |
|---------|------|------|
| success | Boolean | 請求是否成功 |
| valid | Boolean | Token 是否有效 |
| data.userId | String (UUID) | 使用者 ID |
| data.username | String | 使用者帳號 |

### 錯誤回應

#### 401 Unauthorized - Token 無效

```json
{
  "success": false,
  "valid": false,
  "message": "Token 無效或已過期"
}
```

### HTTP 狀態碼

| 狀態碼 | 說明 |
|-------|------|
| 200 | 驗證成功 |
| 401 | Token 無效或已過期 |
| 500 | 伺服器內部錯誤 |

---

## JWT Token 結構

### Access Token Payload

```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "username": "john_doe",
  "type": "access",
  "iat": 1696067400,
  "exp": 1696071000
}
```

| 欄位 | 說明 |
|-----|------|
| sub | 使用者 ID |
| username | 使用者帳號 |
| type | Token 類型 |
| iat | 發行時間（Unix timestamp） |
| exp | 過期時間（Unix timestamp） |

### Refresh Token Payload

```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "type": "refresh",
  "jti": "token-unique-id",
  "iat": 1696067400,
  "exp": 1696671000
}
```

| 欄位 | 說明 |
|-----|------|
| sub | 使用者 ID |
| type | Token 類型 |
| jti | Token 唯一識別碼 |
| iat | 發行時間 |
| exp | 過期時間 |

---

## 業務規則

### 1. 登入失敗次數限制
- 同一帳號 15 分鐘內失敗 5 次會被鎖定
- 鎖定期間為 15 分鐘
- 登入成功後重置失敗計數

### 2. Token 管理
- Access Token 有效期：1 小時
- Refresh Token 有效期：
  - rememberMe = true：7 天
  - rememberMe = false：24 小時
- 登出時 Token 會加入黑名單

### 3. 安全性
- 密碼使用 BCrypt 驗證
- Token 使用 HS256 算法簽名
- 傳輸必須使用 HTTPS
- Token 儲存於客戶端（localStorage 或 sessionStorage）

---

## 錯誤碼對照表

| 錯誤碼 | HTTP 狀態 | 說明 | 解決方式 |
|-------|----------|------|---------|
| INVALID_CREDENTIALS | 401 | 帳號或密碼錯誤 | 檢查帳號密碼是否正確 |
| ACCOUNT_DISABLED | 403 | 帳號已被停用 | 聯絡管理員 |
| TOO_MANY_ATTEMPTS | 429 | 登入失敗次數過多 | 等待 15 分鐘後再試 |
| TOKEN_EXPIRED | 401 | Token 已過期 | 使用 Refresh Token 刷新 |
| TOKEN_INVALID | 401 | Token 無效 | 重新登入 |
| TOKEN_REVOKED | 401 | Token 已被撤銷 | 重新登入 |

---

## 整合範例

### JavaScript (完整登入流程)

```javascript
class AuthService {
  constructor() {
    this.baseURL = 'http://localhost:8080/api/v1/auth';
  }

  // 登入
  async login(username, password, rememberMe = false) {
    try {
      const response = await fetch(`${this.baseURL}/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ username, password, rememberMe })
      });

      const data = await response.json();

      if (response.ok) {
        // 儲存 Tokens
        const storage = rememberMe ? localStorage : sessionStorage;
        storage.setItem('accessToken', data.data.tokens.accessToken);
        storage.setItem('refreshToken', data.data.tokens.refreshToken);
        storage.setItem('user', JSON.stringify(data.data.user));
        
        return { success: true, data: data.data };
      } else {
        return { success: false, message: data.message };
      }
    } catch (error) {
      console.error('登入錯誤:', error);
      return { success: false, message: '網路連線失敗' };
    }
  }

  // 登出
  async logout() {
    try {
      const token = this.getAccessToken();
      
      const response = await fetch(`${this.baseURL}/logout`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      // 清除本地資料
      localStorage.clear();
      sessionStorage.clear();

      return response.ok;
    } catch (error) {
      console.error('登出錯誤:', error);
      return false;
    }
  }

  // 刷新 Token
  async refreshToken() {
    try {
      const refreshToken = this.getRefreshToken();
      
      if (!refreshToken) {
        throw new Error('No refresh token available');
      }

      const response = await fetch(`${this.baseURL}/refresh`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ refreshToken })
      });

      const data = await response.json();

      if (response.ok) {
        // 更新 Access Token
        const storage = localStorage.getItem('refreshToken') ? localStorage : sessionStorage;
        storage.setItem('accessToken', data.data.accessToken);
        
        return { success: true, accessToken: data.data.accessToken };
      } else {
        // Refresh Token 也過期，需要重新登入
        this.logout();
        window.location.href = '/login';
        return { success: false };
      }
    } catch (error) {
      console.error('刷新 Token 錯誤:', error);
      return { success: false };
    }
  }

  // 取得 Access Token
  getAccessToken() {
    return localStorage.getItem('accessToken') || 
           sessionStorage.getItem('accessToken');
  }

  // 取得 Refresh Token
  getRefreshToken() {
    return localStorage.getItem('refreshToken') || 
           sessionStorage.getItem('refreshToken');
  }

  // 檢查是否已登入
  isAuthenticated() {
    return !!this.getAccessToken();
  }
}

// 使用範例
const authService = new AuthService();

// 登入
await authService.login('john_doe', 'Test@1234', true);

// 登出
await authService.logout();

// 刷新 Token
await authService.refreshToken();
```

---

## 測試案例

### 成功案例

```json
// 測試案例 1: 正常登入（不記住我）
{
  "username": "john_doe",
  "password": "Test@1234",
  "rememberMe": false
}
// 預期: 200 OK, Refresh Token 有效期 24 小時

// 測試案例 2: 正常登入（記住我）
{
  "username": "john_doe",
  "password": "Test@1234",
  "rememberMe": true
}
// 預期: 200 OK, Refresh Token 有效期 7 天

// 測試案例 3: 刷新 Token
{
  "refreshToken": "valid-refresh-token"
}
// 預期: 200 OK, 返回新的 Access Token
```

### 失敗案例

```json
// 測試案例 4: 密碼錯誤
{
  "username": "john_doe",
  "password": "WrongPassword",
  "rememberMe": false
}
// 預期: 401 Unauthorized

// 測試案例 5: 帳號不存在
{
  "username": "non_existent_user",
  "password": "Test@1234",
  "rememberMe": false
}
// 預期: 401 Unauthorized

// 測試案例 6: 連續失敗 5 次
// 第 1-4 次: 401 Unauthorized
// 第 5 次: 429 Too Many Requests

// 測試案例 7: 過期的 Refresh Token
{
  "refreshToken": "expired-refresh-token"
}
// 預期: 401 Unauthorized

// 測試案例 8: 帳號被停用
{
  "username": "disabled_user",
  "password": "Test@1234",
  "rememberMe": false
}
// 預期: 403 Forbidden
```

---

## 前端整合注意事項

### 1. Token 儲存
```javascript
// 根據 rememberMe 決定儲存位置
const storage = rememberMe ? localStorage : sessionStorage;
storage.setItem('accessToken', token);
```

### 2. 自動刷新 Token
```javascript
// 在 Access Token 過期前 5 分鐘自動刷新
const scheduleTokenRefresh = () => {
  const token = getAccessToken();
  const decoded = jwtDecode(token);
  const expiresAt = decoded.exp * 1000;
  const now = Date.now();
  const refreshTime = expiresAt - now - (5 * 60 * 1000);
  
  if (refreshTime > 0) {
    setTimeout(async () => {
      await authService.refreshToken();
      scheduleTokenRefresh(); // 排程下一次刷新
    }, refreshTime);
  }
};
```

### 3. HTTP 攔截器
```javascript
// Axios 攔截器範例
axios.interceptors.request.use(config => {
  const token = authService.getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

axios.interceptors.response.use(
  response => response,
  async error => {
    if (error.response?.status === 401) {
      // Token 過期，嘗試刷新
      const result = await authService.refreshToken();
      if (result.success) {
        // 重試原始請求
        error.config.headers.Authorization = `Bearer ${result.accessToken}`;
        return axios(error.config);
      }
    }
    return Promise.reject(error);
  }
);
```

---

## 版本歷史

| 版本 | 日期 | 變更內容 |
|-----|------|---------|
| 1.0 | 2025-09-30 | 初版發布 |