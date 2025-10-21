package com.chathub.security;

import com.chathub.service.RedisService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * JWT 認證過濾器
 *
 * 功能：
 * 1. 從請求 Header 提取 JWT Token
 * 2. 驗證 Token 有效性
 * 3. 檢查 Token 黑名單（登出功能用）
 * 4. 將使用者資訊載入 SecurityContext
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            // 1. 從 Header 提取 Token
            String token = extractTokenFromRequest(request);

            if (token == null) {
                // 沒有 Token，繼續執行（可能是公開端點）
                filterChain.doFilter(request, response);
                return;
            }

            // 2. 驗證 Token 格式與簽章
            if (!jwtTokenProvider.validateToken(token)) {
                log.warn("無效的 JWT Token");
                filterChain.doFilter(request, response);
                return;
            }

            // 3. 提取 Token 資訊
            Claims claims = jwtTokenProvider.getClaimsFromToken(token);
            String userId = claims.getSubject();
            String username = claims.get("username", String.class);
            String tokenType = claims.get("type", String.class);

            // 4. 檢查 Token 類型（必須是 Access Token）
            if (!"access".equals(tokenType)) {
                log.warn("Token 類型錯誤: {}", tokenType);
                filterChain.doFilter(request, response);
                return;
            }

            // 5. 檢查 Token 黑名單（可選，登出功能會用到）
            String jti = claims.getId(); // JWT ID
            if (jti != null && redisService.isTokenBlacklisted(jti)) {
                log.warn("Token 已被列入黑名單: {}", jti);
                filterChain.doFilter(request, response);
                return;
            }

            // 建立 UserDetails，username 使用 userId
            UserDetails userDetails = User.builder()
                                          .username(userId)  // ✅ 這裡用 userId
                                          .password("")
                                          .authorities(Collections.emptyList())
                                          .build();

            // 6. 載入使用者資訊到 SecurityContext
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    userDetails,           // Principal (使用者 ID)
                    null,            // Credentials (不需要密碼)
                    new ArrayList<>() // Authorities (暫時空的，之後可加入角色)
                );

            // 設定額外資訊（可在 Controller 中取得）
            authentication.setDetails(username);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("JWT 驗證成功 - User: {}, Username: {}", userId, username);

        } catch (ExpiredJwtException e) {
            log.warn("JWT Token 已過期: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT Token 格式錯誤: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("JWT Token 簽章驗證失敗: {}", e.getMessage());
        } catch (Exception e) {
            log.error("JWT 驗證過程發生錯誤", e);
        }

        // 繼續執行過濾器鏈
        filterChain.doFilter(request, response);
    }

    /**
     * 從 HTTP Request 提取 JWT Token
     *
     * 支援兩種方式：
     * 1. Authorization Header: Bearer {token}
     * 2. Query Parameter: ?token={token} (WebSocket 連線用)
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        // 方式 1: Authorization Header
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // 移除 "Bearer " 前綴
        }

        // 方式 2: Query Parameter (WebSocket 用)
        String tokenParam = request.getParameter("token");
        if (tokenParam != null && !tokenParam.isEmpty()) {
            return tokenParam;
        }

        return null;
    }

    /**
     * 判斷是否跳過此過濾器
     *
     * 公開端點不需要 JWT 驗證
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // 公開端點清單
        return path.startsWith("/api/v1/auth/register") ||
            path.startsWith("/api/v1/auth/login") ||
            path.startsWith("/api/v1/auth/refresh") ||
            path.startsWith("/api/v1/auth/check-username") ||
            path.startsWith("/ws/"); // WebSocket 端點（之後會處理）
    }
}