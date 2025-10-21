package com.chathub.service.impl;

import com.chathub.dto.friend.FriendInfoDto;
import com.chathub.dto.friend.FriendRequestResponseDto;
import com.chathub.entity.FriendRequest;
import com.chathub.entity.Friendship;
import com.chathub.entity.User;
import com.chathub.repository.FriendRequestRepository;
import com.chathub.repository.FriendshipRepository;
import com.chathub.repository.UserRepository;
import com.chathub.service.FriendService;
import com.chathub.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {

    private final UserRepository userRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final RedisService redisService;

    /**
     * 發送好友請求
     */
    @Override
    @Transactional
    public FriendRequestResponseDto sendFriendRequest(UUID fromUserId, String toUsername) {
        // 1. 查詢發送者
        User fromUser = userRepository.findById(fromUserId)
                                      .orElseThrow(() -> new RuntimeException("使用者不存在"));

        // 2. 查詢接收者
        User toUser = userRepository.findByUsername(toUsername)
                                    .orElseThrow(() -> new RuntimeException("目標使用者不存在"));

        UUID toUserId = toUser.getUserId();

        // 3. 檢查：不能加自己為好友
        if (fromUserId.equals(toUserId)) {
            throw new RuntimeException("不能加自己為好友");
        }

        // 4. 檢查：是否已是好友
        if (friendshipRepository.existsFriendship(fromUserId, toUserId)) {
            throw new RuntimeException("已經是好友了");
        }

        // 5. 檢查：是否已有待處理的請求（雙向檢查）
        if (friendRequestRepository.findPendingRequestBetweenUsers(fromUserId, toUserId).isPresent()) {
            throw new RuntimeException("已有待處理的好友請求");
        }

        // 6. 建立好友請求
        FriendRequest request = FriendRequest.builder()
                                             .fromUser(fromUser)
                                             .toUser(toUser)
                                             .status(FriendRequest.RequestStatus.PENDING)
                                             .build();

        FriendRequest savedRequest = friendRequestRepository.save(request);

        log.info("好友請求已發送 - From: {}, To: {}", fromUser.getUsername(), toUser.getUsername());

        // 7. 回傳結果
        return FriendRequestResponseDto.builder()
                                       .requestId(savedRequest.getRequestId())
                                       .fromUserId(fromUserId)
                                       .fromUsername(fromUser.getUsername())
                                       .status("PENDING")
                                       .createdAt(savedRequest.getCreatedAt())
                                       .build();
    }

    /**
     * 查詢收到的好友請求
     */
    @Override
    @Transactional(readOnly = true)
    public List<FriendRequestResponseDto> getReceivedRequests(UUID userId) {
        // 查詢所有待處理的請求
        List<FriendRequest> requests = friendRequestRepository.findReceivedPendingRequests(userId);

        // 轉換為 DTO
        return requests.stream()
                       .map(request -> FriendRequestResponseDto.builder()
                                                               .requestId(request.getRequestId())
                                                               .fromUserId(request.getFromUser().getUserId())
                                                               .fromUsername(request.getFromUser().getUsername())
                                                               .status(request.getStatus().name())
                                                               .createdAt(request.getCreatedAt())
                                                               .build())
                       .collect(Collectors.toList());
    }

    /**
     * 接受好友請求
     */
    @Override
    @Transactional
    public void acceptFriendRequest(UUID requestId, UUID userId) {
        // 1. 查詢請求（只能處理發給自己且待處理的請求）
        FriendRequest request = friendRequestRepository.findPendingRequestById(requestId, userId)
                                                       .orElseThrow(() -> new RuntimeException("找不到待處理的好友請求"));

        // 2. 更新請求狀態
        request.setStatus(FriendRequest.RequestStatus.ACCEPTED);
        request.setRespondedAt(LocalDateTime.now());
        friendRequestRepository.save(request);

        // 3. 建立雙向好友關係
        User fromUser = request.getFromUser();
        User toUser = request.getToUser();

        // A -> B
        Friendship friendship1 = Friendship.builder()
                                           .user(fromUser)
                                           .friend(toUser)
                                           .build();

        // B -> A
        Friendship friendship2 = Friendship.builder()
                                           .user(toUser)
                                           .friend(fromUser)
                                           .build();

        friendshipRepository.save(friendship1);
        friendshipRepository.save(friendship2);

        log.info("好友請求已接受 - {} 和 {} 成為好友", fromUser.getUsername(), toUser.getUsername());
    }

    /**
     * 拒絕好友請求
     */
    @Override
    @Transactional
    public void rejectFriendRequest(UUID requestId, UUID userId) {
        // 1. 查詢請求
        FriendRequest request = friendRequestRepository.findPendingRequestById(requestId, userId)
                                                       .orElseThrow(() -> new RuntimeException("找不到待處理的好友請求"));

        // 2. 更新請求狀態
        request.setStatus(FriendRequest.RequestStatus.REJECTED);
        request.setRespondedAt(LocalDateTime.now());
        friendRequestRepository.save(request);

        log.info("好友請求已拒絕 - From: {}, To: {}",
                 request.getFromUser().getUsername(),
                 request.getToUser().getUsername());
    }

    /**
     * 查詢好友列表
     */
    @Override
    @Transactional(readOnly = true)
    public List<FriendInfoDto> getFriendsList(UUID userId) {
        // 1. 查詢所有好友關係
        List<Friendship> friendships = friendshipRepository.findFriendsByUserId(userId);

        // 2. 轉換為 DTO，並查詢線上狀態
        return friendships.stream()
                          .map(friendship -> {
                              User friend = friendship.getFriend();

                              // 從 Redis 查詢線上狀態
                              boolean isOnline = redisService.isUserOnline(friend.getUserId());

                              return FriendInfoDto.builder()
                                                  .userId(friend.getUserId())
                                                  .username(friend.getUsername())
                                                  .isOnline(isOnline)
                                                  .friendsSince(friendship.getCreatedAt())
                                                  .build();
                          })
                          .collect(Collectors.toList());
    }

    /**
     * 刪除好友
     */
    @Override
    @Transactional
    public void deleteFriend(UUID userId, UUID friendId) {
        // 1. 檢查好友關係是否存在
        if (!friendshipRepository.existsFriendship(userId, friendId)) {
            throw new RuntimeException("不是好友關係");
        }

        // 2. 刪除雙向好友關係
        int deleted1 = friendshipRepository.deleteFriendship(userId, friendId);
        int deleted2 = friendshipRepository.deleteFriendship(friendId, userId);

        if (deleted1 == 0 || deleted2 == 0) {
            throw new RuntimeException("刪除好友失敗");
        }

        log.info("好友關係已刪除 - User: {}, Friend: {}", userId, friendId);
    }
}