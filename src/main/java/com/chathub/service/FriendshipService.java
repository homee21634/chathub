package com.chathub.service;

import com.chathub.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 好友關係服務
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;

    /**
     * 檢查兩個使用者是否為好友
     */
    public boolean areFriends(UUID userId1, UUID userId2) {
        boolean isFriend = friendshipRepository.areFriends(userId1, userId2);
        log.debug("檢查好友關係：{} 與 {} = {}", userId1, userId2, isFriend);
        return isFriend;
    }
}