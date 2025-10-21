package com.chathub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "帳號不能為空")
    @Size(min = 4, max = 20, message = "帳號長度必須在 4-20 字元之間")
    private String username;

    @NotBlank(message = "密碼不能為空")
    private String password;

    private boolean rememberMe = false;
}