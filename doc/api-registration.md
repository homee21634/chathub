# 註冊 API 文件

**版本：** 1.0  
**最後更新：** 2025-09-30

---

## API 列表

1. [註冊新使用者](#1-註冊新使用者)
2. [檢查帳號可用性](#2-檢查帳號可用性)

---

## 1. 註冊新使用者

### 基本資訊

- **端點：** `POST /api/v1/auth/register`
- **描述：** 建立新的使用者帳號
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
  "confirmPassword": "string"
}
```

### 請求欄位說明

| 欄位名稱 | 類型 | 必填 | 長度限制 | 說明 | 驗證規則 |
|---------|------|------|---------|------|---------|
| username | String | 是 | 4-20 字元 | 使用者帳號 | 只允許英文字母、數字、底線 |
| password | String | 是 | 最少 8 字元 | 使用者密碼 | 必須包含大小寫字母、數字、符號各至少一個 |
| confirmPassword | String | 是 | 最少 8 字元 | 確認密碼 | 必須與 password 相同 |

### 密碼驗證規則

**正規表達式：**
```regex
^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$
```

**規則說明：**
- ✅ 至少 8 位數
- ✅ 至少一個大寫字母 (A-Z)
- ✅ 至少一個小寫字母 (a-z)
- ✅ 至少一個數字 (0-9)
- ✅ 至少一個符號 (@$!%*?&...)

### 請求範例

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "Test@1234",
    "confirmPassword": "Test@1234"
  }'
```

### 成功回應 (201 Created)

```json
{
  "success": true,
  "message": "註冊成功",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "username": "john_doe",
    "createdAt": "2025-09-30T10:30:00Z"
  }
}
```

### 成功回應欄位說明

| 欄位路徑 | 類型 | 說明 |
|---------|------|------|
| success | Boolean | 請求是否成功 |
| message | String | 回應訊息 |
| data.userId | String (UUID) | 使用者唯一識別碼 |
| data.username | String | 使用者帳號 |
| data.createdAt | String (ISO 8601) | 帳號建立時間 |

### 錯誤回應

#### 400 Bad Request - 驗證失敗

**情況：** 欄位驗證失敗

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

**可能的錯誤訊息：**

| 欄位 | 錯誤訊息 |
|-----|---------|
| username | 帳號長度必須在 4-20 字元之間 |
| username | 帳號只能包含英文字母、數字和底線 |
| password | 密碼必須包含大小寫字母、數字和符號各至少一個 |
| password | 密碼長度必須至少 8 字元 |
| confirmPassword | 兩次輸入的密碼不一致 |

#### 409 Conflict - 帳號已存在

**情況：** 使用者名稱已被註冊

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

#### 500 Internal Server Error - 伺服器錯誤

**情況：** 伺服器內部錯誤

```json
{
  "success": false,
  "message": "伺服器錯誤，請稍後再試"
}
```

### HTTP 狀態碼

| 狀態碼 | 說明 |
|-------|------|
| 201 | 註冊成功 |
| 400 | 請求參數驗證失敗 |
| 409 | 帳號已存在 |
| 500 | 伺服器內部錯誤 |

---

## 2. 檢查帳號可用性

### 基本資訊

- **端點：** `GET /api/v1/auth/check-username`
- **描述：** 檢查帳號是否已被使用（用於即時驗證）
- **需要認證：** 否

### 請求標頭

```
無需特殊標頭
```

### 查詢參數 (Query Parameters)

| 參數名稱 | 類型 | 必填 | 說明 | 範例 |
|---------|------|------|------|------|
| username | String | 是 | 要檢查的帳號 | john_doe |

### 請求範例

```bash
curl -X GET "http://localhost:8080/api/v1/auth/check-username?username=john_doe"
```

### 成功回應 (200 OK) - 帳號可用

```json
{
  "success": true,
  "available": true,
  "message": "此帳號可以使用"
}
```

### 成功回應 (200 OK) - 帳號已被使用

```json
{
  "success": true,
  "available": false,
  "message": "此帳號已被使用"
}
```

### 回應欄位說明

| 欄位名稱 | 類型 | 說明 |
|---------|------|------|
| success | Boolean | 請求是否成功 |
| available | Boolean | 帳號是否可用（true=可用，false=已被使用） |
| message | String | 回應訊息 |

### 錯誤回應

#### 400 Bad Request - 參數錯誤

**情況：** 未提供 username 參數或參數格式錯誤

```json
{
  "success": false,
  "message": "請提供有效的帳號"
}
```

### HTTP 狀態碼

| 狀態碼 | 說明 |
|-------|------|
| 200 | 查詢成功 |
| 400 | 參數錯誤 |
| 500 | 伺服器內部錯誤 |

---

## 業務規則

### 1. 帳號規則
- 長度：4-20 字元
- 允許字元：英文字母（a-z, A-Z）、數字（0-9）、底線（_）
- 唯一性：系統中不可重複
- 大小寫：區分大小寫（john_doe 和 John_Doe 是不同帳號）

### 2. 密碼規則
- 最小長度：8 字元
- 必須包含：
  - 至少一個小寫字母
  - 至少一個大寫字母
  - 至少一個數字
  - 至少一個符號（@$!%*?&）
- 加密方式：BCrypt（強度 10）
- 儲存方式：僅儲存 Hash 值，不儲存明文

### 3. 安全性
- 密碼在傳輸時使用 HTTPS 加密
- 密碼 Hash 使用 BCrypt 算法
- 帳號查詢有頻率限制（防止枚舉攻擊）
- 註冊成功後不自動登入（需另外呼叫登入 API）

---

## 錯誤碼對照表

| 錯誤碼 | HTTP 狀態 | 說明 | 解決方式 |
|-------|----------|------|---------|
| VALIDATION_ERROR | 400 | 欄位驗證失敗 | 檢查欄位格式是否符合規則 |
| USERNAME_TOO_SHORT | 400 | 帳號太短 | 帳號至少 4 字元 |
| USERNAME_TOO_LONG | 400 | 帳號太長 | 帳號最多 20 字元 |
| USERNAME_INVALID_CHARS | 400 | 帳號包含非法字元 | 只使用英文、數字、底線 |
| PASSWORD_TOO_WEAK | 400 | 密碼強度不足 | 符合密碼複雜度要求 |
| PASSWORD_MISMATCH | 400 | 密碼不一致 | 確認密碼須與密碼相同 |
| USERNAME_ALREADY_EXISTS | 409 | 帳號已存在 | 使用其他帳號 |
| SERVER_ERROR | 500 | 伺服器錯誤 | 稍後再試或聯絡客服 |

---

## 測試案例

### 成功案例

```json
// 測試案例 1: 正常註冊
{
  "username": "test_user_01",
  "password": "Test@1234",
  "confirmPassword": "Test@1234"
}
// 預期: 201 Created

// 測試案例 2: 邊界值測試（最短帳號）
{
  "username": "test",
  "password": "Test@1234",
  "confirmPassword": "Test@1234"
}
// 預期: 201 Created

// 測試案例 3: 邊界值測試（最長帳號）
{
  "username": "test_user_12345678",
  "password": "Test@1234",
  "confirmPassword": "Test@1234"
}
// 預期: 201 Created
```

### 失敗案例

```json
// 測試案例 4: 帳號太短
{
  "username": "usr",
  "password": "Test@1234",
  "confirmPassword": "Test@1234"
}
// 預期: 400 Bad Request

// 測試案例 5: 密碼缺少大寫字母
{
  "username": "test_user",
  "password": "test@1234",
  "confirmPassword": "test@1234"
}
// 預期: 400 Bad Request

// 測試案例 6: 密碼不一致
{
  "username": "test_user",
  "password": "Test@1234",
  "confirmPassword": "Test@5678"
}
// 預期: 400 Bad Request

// 測試案例 7: 帳號已存在
{
  "username": "existing_user",
  "password": "Test@1234",
  "confirmPassword": "Test@1234"
}
// 預期: 409 Conflict
```

---

## 整合範例

### JavaScript (Fetch API)

```javascript
async function registerUser(username, password, confirmPassword) {
  try {
    const response = await fetch('http://localhost:8080/api/v1/auth/register', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        username,
        password,
        confirmPassword
      })
    });

    const data = await response.json();

    if (response.ok) {
      console.log('註冊成功:', data.data);
      return { success: true, data: data.data };
    } else {
      console.error('註冊失敗:', data.message);
      return { success: false, errors: data.errors };
    }
  } catch (error) {
    console.error('網路錯誤:', error);
    return { success: false, error: '網路連線失敗' };
  }
}

// 使用範例
registerUser('john_doe', 'Test@1234', 'Test@1234')
  .then(result => {
    if (result.success) {
      alert('註冊成功！');
      // 跳轉到登入頁面
    }
  });
```

### JavaScript (Axios)

```javascript
import axios from 'axios';

const registerUser = async (username, password, confirmPassword) => {
  try {
    const response = await axios.post(
            'http://localhost:8080/api/v1/auth/register',
            {
              username,
              password,
              confirmPassword
            }
    );

    return {
      success: true,
      data: response.data.data
    };
  } catch (error) {
    if (error.response) {
      // 伺服器回應錯誤
      return {
        success: false,
        message: error.response.data.message,
        errors: error.response.data.errors
      };
    } else {
      // 網路錯誤
      return {
        success: false,
        message: '網路連線失敗'
      };
    }
  }
};
```

---

## 版本歷史

| 版本 | 日期 | 變更內容 |
|-----|------|---------|
| 1.0 | 2025-09-30 | 初版發布 |