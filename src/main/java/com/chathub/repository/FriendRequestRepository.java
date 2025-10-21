package com.chathub.repository;

import com.chathub.entity.FriendRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, UUID> {

    /**
     * 查詢收到的好友請求（待處理）
     */
    @Query("SELECT fr FROM FriendRequest fr " +
        "WHERE fr.toUser.userId = :userId " +
        "AND fr.status = 'PENDING' " +
        "ORDER BY fr.createdAt DESC")
    List<FriendRequest> findReceivedPendingRequests(UUID userId);

    /**
     * 檢查是否已有待處理的請求（雙向檢查）
     */
    @Query("SELECT fr FROM FriendRequest fr " +
        "WHERE ((fr.fromUser.userId = :fromUserId AND fr.toUser.userId = :toUserId) " +
        "OR (fr.fromUser.userId = :toUserId AND fr.toUser.userId = :fromUserId)) " +
        "AND fr.status = 'PENDING'")
    Optional<FriendRequest> findPendingRequestBetweenUsers(UUID fromUserId, UUID toUserId);

    /**
     * 查詢特定請求（用於接受/拒絕）
     */
    @Query("SELECT fr FROM FriendRequest fr " +
        "WHERE fr.requestId = :requestId " +
        "AND fr.toUser.userId = :userId " +
        "AND fr.status = 'PENDING'")
    Optional<FriendRequest> findPendingRequestById(UUID requestId, UUID userId);
}