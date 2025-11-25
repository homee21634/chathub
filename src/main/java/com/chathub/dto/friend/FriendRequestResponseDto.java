package com.chathub.dto.friend;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
/**
 * 好友請求回應
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestResponseDto {

    private UUID requestId;
    private UUID fromUserId;
    private String fromUsername;
    private String status;
    private String createdAt;
}
