package com.chathub.service;

import com.chathub.dto.LoginRequest;
import com.chathub.dto.LoginResponse;
import com.chathub.dto.RegisterRequest;
import com.chathub.dto.RegisterResponse;

public interface UserService {
    RegisterResponse registerUser(RegisterRequest request);
    boolean isUsernameAvailable(String username);
    LoginResponse login(LoginRequest request, String ipAddress, String userAgent);

    /**
     * 刷新 Access Token
     *
     * @param refreshTokenValue Refresh Token 字串
     * @param deviceInfo 裝置資訊（用於安全檢查）
     * @param ipAddress IP 位址（用於安全檢查）
     * @return 新的 LoginResponse（包含新 Access Token）
     */
    LoginResponse refreshAccessToken(String refreshTokenValue, String deviceInfo, String ipAddress);

    /**
     * 撤銷 Refresh Token（登出時使用）
     *
     * @param refreshTokenValue Refresh Token 字串
     */
    void revokeRefreshToken(String refreshTokenValue);

    /**
     * 使用者登出
     *
     * @param accessToken Access Token
     */
    void logout(String accessToken);
}