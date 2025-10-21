package com.chathub.security;

import com.chathub.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    // 生成 Access Token
    public String generateAccessToken(UUID userId, String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());

        return Jwts.builder()
                   .setSubject(userId.toString())
                   .claim("username", username)
                   .claim("type", "access")
                   .setIssuedAt(now)
                   .setExpiration(expiryDate)
                   .setId(UUID.randomUUID().toString())
                   .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                   .compact();
    }

    // 驗證 Token
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT Token 已過期");
            return false;
        } catch (MalformedJwtException e) {
            log.warn("JWT Token 格式錯誤");
            return false;
        } catch (SignatureException e) {
            log.warn("JWT Token 簽章無效");
            return false;
        } catch (Exception e) {
            log.error("JWT Token 驗證失敗", e);
            return false;
        }
    }

    // 從 Token 取得 userId
    public UUID getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                            .setSigningKey(getSigningKey())
                            .build()
                            .parseClaimsJws(token)
                            .getBody();
        return UUID.fromString(claims.getSubject());
    }

    /**
     * 從 Token 提取使用者名稱
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("username", String.class);
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }
    /**
     * 從 Token 提取 Claims
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                   .setSigningKey(getSigningKey())
                   .build()
                   .parseClaimsJws(token)
                   .getBody();
    }

  /** 檢查 Token 是否即將過期（剩餘時間少於 5 分鐘） */
  public boolean isTokenExpiringSoon(String token) {
        try {
          Claims claims = getClaimsFromToken(token);
          Date expiration = claims.getExpiration();
          long remainingTime = expiration.getTime() - System.currentTimeMillis();
          return remainingTime < 5 * 60 * 1000; // 5 分鐘
        } catch (Exception e) {
          return true;
        }
  }

}