package com.chathub.service.impl;

import com.chathub.dto.LoginRequest;
import com.chathub.entity.RefreshToken;
import com.chathub.exception.UsernameAlreadyExistsException;
import com.chathub.repository.RefreshTokenRepository;
import com.chathub.security.JwtTokenProvider;
import com.chathub.config.JwtProperties;
import com.chathub.dto.LoginResponse;
import com.chathub.dto.RegisterRequest;
import com.chathub.dto.RegisterResponse;
import com.chathub.entity.User;
import com.chathub.exception.AuthenticationException;
import com.chathub.repository.UserRepository;
import com.chathub.service.RedisService;
import com.chathub.service.UserService;
import io.jsonwebtoken.Claims;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisService redisService;
    private final JwtProperties jwtProperties;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        String username = request.getUsername();

        // 1. 檢查登入失敗次數
        int attempts = redisService.getLoginAttempts(username);
        if (attempts >= 5) {
            throw new RuntimeException("登入失敗次數過多，請 15 分鐘後再試");
        }

        // 2. 驗證帳號密碼
        User user = userRepository.findByUsername(username)
                                  .orElseThrow(() -> new RuntimeException("帳號或密碼錯誤"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            redisService.incrementLoginAttempts(username);
            throw new RuntimeException("帳號或密碼錯誤");
        }

        // 3. 清除登入失敗記錄
        redisService.clearLoginAttempts(username);

        // 4. 生成 Tokens
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUserId(), username);
        String refreshToken = UUID.randomUUID().toString();

        // 5. 儲存 Refresh Token
        String tokenHash = hashRefreshToken(refreshToken); // 呼叫 hashRefreshToken 方法

        RefreshToken refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setUser(user);
        refreshTokenEntity.setTokenHash(tokenHash); // 儲存 Hash 而非原始 Token
        // 計算過期時間
        LocalDateTime expiresAt = request.isRememberMe()
            ? LocalDateTime.now().plusDays(7)
            : LocalDateTime.now().plusDays(1);

        refreshTokenEntity.setExpiresAt(expiresAt);
        refreshTokenEntity.setCreatedAt(LocalDateTime.now());
        refreshTokenEntity.setDeviceInfo(userAgent);
        refreshTokenEntity.setIpAddress(ipAddress);
        refreshTokenRepository.save(refreshTokenEntity);


        // 6. 更新使用者狀態
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // 7. 設定 Redis 線上狀態
        redisService.setUserOnline(user.getUserId());

        return LoginResponse.builder()
                            .accessToken(accessToken)
                            .refreshToken(refreshToken)
                            .username(username)
                            .userId(user.getUserId().toString())
                            .expiresIn(jwtProperties.getAccessTokenExpiration() / 1000)
                            .build();
    }

    @Override
    @Transactional
    public RegisterResponse registerUser(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("兩次輸入的密碼不一致");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException("此帳號已被註冊");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                        .username(request.getUsername())
                        .passwordHash(hashedPassword)
                        .build();

        User savedUser = userRepository.save(user);


        return RegisterResponse.builder()
                               .userId(savedUser.getUserId())
                               .username(savedUser.getUsername())
                               .createdAt(Instant.now().toString())
                               .build();
    }

    @Override
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    /**
     * 刷新 Access Token
     *
     * 流程：
     * 1. Hash Refresh Token 並查詢資料庫
     * 2. 驗證 Token 是否過期或已撤銷
     * 3. 生成新的 Access Token
     * 4. （可選）輪換 Refresh Token
     * 5. 更新使用者線上狀態
     */
    @Override
    @Transactional
    public LoginResponse refreshAccessToken(String refreshTokenValue, String deviceInfo, String ipAddress) {
        // 1. Hash Refresh Token
        String tokenHash = hashRefreshToken(refreshTokenValue);

        // 2. 查詢資料庫
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                                                          .orElseThrow(() -> new AuthenticationException("無效的 Refresh Token"));

        // 3. 驗證 Token 狀態
        if (refreshToken.getRevokedAt() != null) {
            throw new AuthenticationException("Refresh Token 已被撤銷");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AuthenticationException("Refresh Token 已過期");
        }

        // 4. 安全檢查：裝置資訊與 IP 是否匹配（可選，增強安全性）
        if (!refreshToken.getDeviceInfo().equals(deviceInfo)) {
            log.warn("裝置資訊不匹配 - User: {}, 原裝置: {}, 新裝置: {}",
                     refreshToken.getUser().getUserId(),
                     refreshToken.getDeviceInfo(),
                     deviceInfo);
            // 可選：拒絕刷新或發送安全警告
        }

        // 5. 取得使用者
        User user = refreshToken.getUser();

        // 6. 生成新的 Access Token
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getUserId(), user.getUsername());

        // 7. （可選）輪換 Refresh Token - 提高安全性
        String newRefreshToken = null;
        boolean rotateRefreshToken = true; // 可設定為配置項

        if (rotateRefreshToken) {
            // 撤銷舊 Token
            refreshToken.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(refreshToken);

            // 生成新 Refresh Token
            newRefreshToken = jwtTokenProvider.generateRefreshToken();
            String newTokenHash = hashRefreshToken(newRefreshToken);

            // 計算過期時間（與原 Token 相同策略）
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(
                refreshToken.getExpiresAt().isAfter(LocalDateTime.now().plusDays(1)) ? 7 : 1
            );

            // 儲存新 Token
            RefreshToken newToken = new RefreshToken();
            newToken.setUser(user);
            newToken.setTokenHash(newTokenHash);
            newToken.setCreatedAt(LocalDateTime.now());
            newToken.setExpiresAt(expiresAt);
            newToken.setDeviceInfo(deviceInfo);
            newToken.setIpAddress(ipAddress);
            refreshTokenRepository.save(newToken);
        }

        // 8. 更新 Redis 線上狀態（延長 TTL）
        redisService.setUserOnline(user.getUserId());

        // 9. 回傳結果
        return LoginResponse.builder()
                            .userId(user.getUserId().toString())  // UUID 轉 String
                            .username(user.getUsername())
                            .accessToken(newAccessToken)
                            .refreshToken(rotateRefreshToken ? newRefreshToken : refreshTokenValue)
                            .expiresIn(3600L)  // int 改為 Long
                            .build();
    }

    /**
     * Hash Refresh Token（使用 SHA-256）
     */
    private String hashRefreshToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 演算法不可用", e);
        }
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String refreshTokenValue) {
        String tokenHash = hashRefreshToken(refreshTokenValue);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                                                          .orElseThrow(() -> new AuthenticationException("無效的 Refresh Token"));

        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);
    }

    /**
     * 使用者登出
     *
     * 流程：
     * 1. 從 Token 提取資訊（JTI、userId）
     * 2. 將 Token 加入黑名單
     * 3. 撤銷對應的 Refresh Token
     * 4. 清除線上狀態
     */
    @Override
    @Transactional
    public void logout(String accessToken) {
        // 1. 驗證並提取 Token 資訊
        if (!jwtTokenProvider.validateToken(accessToken)) {
            throw new AuthenticationException("無效的 Token");
        }

        Claims claims = jwtTokenProvider.getClaimsFromToken(accessToken);
        String jti = claims.getId();
        UUID userId = UUID.fromString(claims.getSubject());
        Date expiration = claims.getExpiration();

        // 2. 計算 Token 剩餘有效時間（秒）
        long expirationSeconds = (expiration.getTime() - System.currentTimeMillis()) / 1000;

        if (expirationSeconds > 0) {
            // 3. 將 Token 加入黑名單（只需保存到過期時間）
            redisService.addTokenToBlacklist(jti, expirationSeconds);
        }

        // 4. 撤銷所有 Refresh Token（使用者無法再刷新）
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.revokeAllTokensByUserId(userId, now);

        // 5. 清除 Redis 線上狀態
        redisService.removeUserOnline(userId);

        log.info("使用者登出成功 - UserId: {}", userId);
    }
}

