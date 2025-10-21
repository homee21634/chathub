package com.chathub.repository;

import com.chathub.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    /**
     * 查詢使用者的所有好友
     */
    @Query("SELECT f FROM Friendship f " +
        "JOIN FETCH f.friend " +
        "WHERE f.user.userId = :userId " +
        "ORDER BY f.createdAt DESC")
    List<Friendship> findFriendsByUserId(UUID userId);

    /**
     * 檢查兩個使用者是否為好友
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END " +
        "FROM Friendship f " +
        "WHERE f.user.userId = :userId " +
        "AND f.friend.userId = :friendId")
    boolean existsFriendship(UUID userId, UUID friendId);

    /**
     * 刪除好友關係（單向）
     */
    @Modifying
    @Query("DELETE FROM Friendship f " +
        "WHERE f.user.userId = :userId " +
        "AND f.friend.userId = :friendId")
    int deleteFriendship(UUID userId, UUID friendId);

    /**
     * 或者使用 @Query（更明確，推薦）
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END " +
        "FROM Friendship f " +
        "WHERE f.user.userId = :userId AND f.friend.userId = :friendId")
    boolean areFriends(@Param("userId") UUID userId, @Param("friendId") UUID friendId);
}