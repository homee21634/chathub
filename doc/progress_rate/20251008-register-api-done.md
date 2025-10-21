`is_active` 和 `is_online` 沒有預設值。修改 `User.java` 的 `@PrePersist`：

```java
@PrePersist
protected void onCreate() {
    createdAt = Timestamp.from(Instant.now());
    updatedAt = Timestamp.from(Instant.now());
    if (isActive == null) {
        isActive = true;
    }
    if (isOnline == null) {
        isOnline = false;
    }
}
```

或直接在欄位初始化：

```java
@Column(nullable = false)
private Boolean isActive = Boolean.TRUE;

@Column(nullable = false)
private Boolean isOnline = Boolean.FALSE;
```

重啟測試。