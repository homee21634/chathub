package com.chathub.dto.friend;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

// ============ Request DTOs ============

/**
 * 發送好友請求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendFriendRequestDto {

    @NotBlank(message = "使用者名稱不能為空")
    private String username;
}
