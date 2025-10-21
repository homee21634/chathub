docker exec -it postgres-chat bash

psql -U postgres -d chatdb

-- 列出所有資料庫
\l 或 \list

-- 切換資料庫
\c 資料庫名稱

-- 查看當前資料庫
SELECT current_database();

-- 創建資料庫
CREATE DATABASE 資料庫名稱;

-- 刪除資料庫
DROP DATABASE 資料庫名稱;

-- 列出當前資料庫的所有資料表
\dt

-- 查看資料表結構
\d 資料表名稱

-- 創建資料表
CREATE TABLE users (
id SERIAL PRIMARY KEY,
name VARCHAR(100),
email VARCHAR(100),
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 刪除資料表
DROP TABLE 資料表名稱;

-- 插入資料
INSERT INTO users (name, email) VALUES ('張三', 'zhang@example.com');

-- 查詢資料
SELECT * FROM users;
SELECT name, email FROM users WHERE id = 1;

-- 更新資料
UPDATE users SET email = 'newemail@example.com' WHERE id = 1;

-- 刪除資料
DELETE FROM users WHERE id = 1;

-- 列出所有使用者
\du

-- 創建使用者
CREATE USER 使用者名稱 WITH PASSWORD '密碼';

-- 授予權限
GRANT ALL PRIVILEGES ON DATABASE 資料庫名稱 TO 使用者名稱;