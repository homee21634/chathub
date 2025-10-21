package com.chathub.repository;

import com.chathub.entity.RefreshToken;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    /**
     * 透過 Token Hash 查詢
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * 查詢使用者所有未撤銷的 Refresh Token
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user.userId = :userId AND rt.revokedAt IS NULL")
    List<RefreshToken> findActiveTokensByUserId(UUID userId);

    /**
     * 撤銷使用者所有 Token（登出所有裝置）
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = :revokedAt WHERE rt.user.userId = :userId AND rt.revokedAt IS NULL")
    int revokeAllTokensByUserId(UUID userId, LocalDateTime revokedAt);

    /**
     * 刪除已過期的 Token（定期清理）
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    int deleteExpiredTokens(LocalDateTime now);
}