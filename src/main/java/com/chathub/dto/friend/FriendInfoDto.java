package com.chathub.dto.friend;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 好友資訊回應
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendInfoDto {

    private UUID userId;
    private String username;
    private Boolean isOnline;
    private String friendsSince;
}
