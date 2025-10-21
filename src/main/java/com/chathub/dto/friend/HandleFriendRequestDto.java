package com.chathub.dto.friend;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 處理好友請求（接受/拒絕）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HandleFriendRequestDto {

    @NotBlank(message = "action 不能為空")
    private String action;  // "accept" 或 "reject"
}
