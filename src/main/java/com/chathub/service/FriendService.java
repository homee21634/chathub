package com.chathub.service;

import com.chathub.dto.friend.FriendRequestResponseDto;
import com.chathub.dto.friend.FriendInfoDto;

import java.util.List;
import java.util.UUID;

public interface FriendService {

    /**
     * 發送好友請求
     *
     * @param fromUserId 發送者 ID
     * @param toUsername 接收者使用者名稱
     * @return 請求資訊
     */
    FriendRequestResponseDto sendFriendRequest(UUID fromUserId, String toUsername);

    /**
     * 查詢收到的好友請求
     *
     * @param userId 使用者 ID
     * @return 請求列表
     */
    List<FriendRequestResponseDto> getReceivedRequests(UUID userId);

    /**
     * 接受好友請求
     *
     * @param requestId 請求 ID
     * @param userId 接收者 ID
     */
    void acceptFriendRequest(UUID requestId, UUID userId);

    /**
     * 拒絕好友請求
     *
     * @param requestId 請求 ID
     * @param userId 接收者 ID
     */
    void rejectFriendRequest(UUID requestId, UUID userId);

    /**
     * 查詢好友列表
     *
     * @param userId 使用者 ID
     * @return 好友列表
     */
    List<FriendInfoDto> getFriendsList(UUID userId);

    /**
     * 刪除好友
     *
     * @param userId 使用者 ID
     * @param friendId 好友 ID
     */
    void deleteFriend(UUID userId, UUID friendId);
}