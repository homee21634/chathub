package com.chathub.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    // 設定使用者線上狀態
    public void setUserOnline(UUID userId) {
        String key = "user:online:" + userId;
        redisTemplate.opsForValue().set(key, "true", Duration.ofHours(1));
    }

    // 移除使用者線上狀態
    public void removeUserOnline(UUID userId) {
        String key = "user:online:" + userId;
        redisTemplate.delete(key);
    }

    // 增加登入失敗次數
    public void incrementLoginAttempts(String username) {
        String key = "login:attempt:" + username;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, Duration.ofMinutes(15));
    }

    // 取得登入失敗次數
    public int getLoginAttempts(String username) {
        String key = "login:attempt:" + username;
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? Integer.parseInt(value.toString()) : 0;
    }

    // 清除登入失敗次數
    public void clearLoginAttempts(String username) {
        String key = "login:attempt:" + username;
        redisTemplate.delete(key);
    }

    /**
     * 將 Token 加入黑名單（登出時使用）
     *
     * @param jti JWT ID (Token 的唯一識別碼)
     * @param expirationSeconds Token 剩餘有效時間（秒）
     */
    public void addTokenToBlacklist(String jti, long expirationSeconds) {
        String key = "token:blacklist:" + jti;
        redisTemplate.opsForValue().set(key, "true", Duration.ofSeconds(expirationSeconds));
    }

    /**
     * 檢查 Token 是否在黑名單中
     *
     * @param jti JWT ID
     * @return true 表示已被列入黑名單
     */
    public boolean isTokenBlacklisted(String jti) {
        String key = "token:blacklist:" + jti;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 檢查使用者是否線上
     */
    public boolean isUserOnline(UUID userId) {
        String key = "user:online:" + userId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}