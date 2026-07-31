package com.domuspacis.auth.application;

import com.domuspacis.aop.annotation.SensitiveParam;
import com.domuspacis.auth.domain.TokenBlacklist;
import com.domuspacis.auth.domain.User;
import com.domuspacis.auth.infrastructure.TokenBlacklistRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    private final TokenBlacklistRepository tokenBlacklistRepository;

    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";
    private static final String CLAIM_TOKEN_TYPE = "typ";
    private static final String CLAIM_PASSWORD_CHANGED_AT = "pca";

    public String extractUsername(@SensitiveParam String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        extraClaims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS);
        embedPasswordChangedAt(extraClaims, userDetails);
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH);
        embedPasswordChangedAt(claims, userDetails);
        return buildToken(claims, userDetails, refreshExpiration);
    }

    private void embedPasswordChangedAt(Map<String, Object> claims, UserDetails userDetails) {
        if (userDetails instanceof User user) {
            claims.put(CLAIM_PASSWORD_CHANGED_AT, user.getPasswordChangedAt().toEpochMilli());
        } else {
            claims.put(CLAIM_PASSWORD_CHANGED_AT, Instant.now().toEpochMilli());
        }
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .claim("roles", userDetails.getAuthorities().stream()
                        .map(a -> a.getAuthority()).toList())
                .signWith(getSignInKey())
                .compact();
    }

    public boolean isTokenValid(@SensitiveParam String token, UserDetails userDetails) {
        return isTokenValid(token, userDetails, TOKEN_TYPE_ACCESS);
    }

    public boolean isRefreshTokenValid(@SensitiveParam String token, UserDetails userDetails) {
        return isTokenValid(token, userDetails, TOKEN_TYPE_REFRESH);
    }

    private boolean isTokenValid(String token, UserDetails userDetails, String expectedType) {
        try {
            final Claims claims = extractAllClaims(token);
            final String username = claims.getSubject();
            final String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);

            // Validate token type
            if (!expectedType.equals(tokenType)) {
                log.warn("Token type mismatch: expected={} actual={}", expectedType, tokenType);
                return false;
            }

            // Validate username match
            if (!username.equals(userDetails.getUsername())) {
                return false;
            }

            // Validate expiry
            if (claims.getExpiration().before(new Date())) {
                return false;
            }

            // Validate user is active
            if (!userDetails.isEnabled()) {
                log.warn("Token rejected: user {} is disabled", username);
                return false;
            }

            // Validate password_changed_at — reject tokens issued before the last password change
            Long tokenPca = claims.get(CLAIM_PASSWORD_CHANGED_AT, Long.class);
            if (tokenPca != null && userDetails instanceof User user) {
                long dbPca = user.getPasswordChangedAt().toEpochMilli();
                if (dbPca > tokenPca) {
                    log.warn("Token rejected: password changed after token issuance for user {}", username);
                    return false;
                }
            }

            // Check blacklist
            String tokenHash = hashToken(token);
            if (tokenBlacklistRepository.existsByTokenHash(tokenHash)) {
                log.warn("Token rejected: blacklisted for user {}", username);
                return false;
            }

            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public void invalidateToken(@SensitiveParam String token, String reason) {
        try {
            final Claims claims = extractAllClaims(token);
            String tokenHash = hashToken(token);
            String userId = claims.getSubject();
            String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
            Instant expiresAt = claims.getExpiration().toInstant();

            TokenBlacklist entry = TokenBlacklist.builder()
                    .tokenHash(tokenHash)
                    .userId(userId)
                    .tokenType(tokenType)
                    .expiresAt(expiresAt)
                    .invalidatedAt(Instant.now())
                    .reason(reason)
                    .build();

            tokenBlacklistRepository.save(entry);
            log.info("Token invalidated: user={} type={} reason={}", userId, tokenType, reason);
        } catch (Exception e) {
            log.warn("Failed to invalidate token: {}", e.getMessage());
        }
    }

    /**
     * Scheduled cleanup of expired blacklist entries.
     * Runs every hour.
     */
    @Scheduled(fixedRateString = "${jwt.blacklist.cleanup-interval-ms:3600000}")
    @Transactional
    public void cleanupExpiredBlacklistEntries() {
        int deleted = tokenBlacklistRepository.deleteExpiredTokens(Instant.now());
        if (deleted > 0) {
            log.debug("Cleaned up {} expired token blacklist entries", deleted);
        }
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public long getExpirationMillis() {
        return jwtExpiration;
    }
}