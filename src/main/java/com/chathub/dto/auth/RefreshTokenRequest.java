package com.chathub.dto.auth;


import jakarta.validation.constraints.NotBlank;

/**
 * Token 刷新請求 DTO
 */
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token 不能為空")
    private String refreshToken;

    // Constructors
    public RefreshTokenRequest() {}

    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    // Getters and Setters
    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}