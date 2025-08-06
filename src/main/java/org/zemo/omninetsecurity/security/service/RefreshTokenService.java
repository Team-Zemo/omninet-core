package org.zemo.omninetsecurity.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zemo.omninetsecurity.security.model.RefreshToken;
import org.zemo.omninetsecurity.security.model.User;
import org.zemo.omninetsecurity.security.repository.RefreshTokenRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final UserService userService;

    @Transactional
    public RefreshToken createRefreshToken(User user, String userAgent, String ipAddress) {
        // Limit the number of active refresh tokens per user (max 5 devices)
        cleanupExcessiveTokens(user.getId(), 4);
        
        String tokenString = jwtService.generateRefreshToken(user);
        LocalDateTime expiresAt = LocalDateTime.now().plus(jwtService.getRefreshTokenExpiration(), ChronoUnit.MILLIS);
        
        RefreshToken refreshToken = new RefreshToken(tokenString, user.getId(), expiresAt);
        refreshToken.setUserAgent(userAgent);
        refreshToken.setIpAddress(ipAddress);
        
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public Optional<String> refreshAccessToken(String refreshTokenString) {
        try {
            if (!jwtService.isTokenValid(refreshTokenString) || !jwtService.isRefreshToken(refreshTokenString)) {
                log.warn("Invalid refresh token provided");
                return Optional.empty();
            }

            Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenString);
            
            if (refreshTokenOpt.isEmpty()) {
                log.warn("Refresh token not found in database");
                return Optional.empty();
            }

            RefreshToken refreshToken = refreshTokenOpt.get();
            
            if (!refreshToken.isValid()) {
                log.warn("Refresh token is expired or revoked");
                refreshTokenRepository.delete(refreshToken);
                return Optional.empty();
            }

            Optional<User> userOpt = userService.getUserById(refreshToken.getUserId());
            if (userOpt.isEmpty()) {
                log.warn("User not found for refresh token");
                refreshTokenRepository.delete(refreshToken);
                return Optional.empty();
            }

            User user = userOpt.get();
            String newAccessToken = jwtService.generateAccessToken(user);
            
            log.info("Successfully refreshed access token for user: {}", user.getEmail());
            return Optional.of(newAccessToken);

        } catch (Exception e) {
            log.error("Error refreshing access token: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Transactional
    public void revokeToken(String refreshTokenString) {
        Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenString);
        if (refreshTokenOpt.isPresent()) {
            RefreshToken refreshToken = refreshTokenOpt.get();
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            log.info("Revoked refresh token for user: {}", refreshToken.getUserId());
        }
    }

    @Transactional
    public void revokeAllUserTokens(String userId) {
        List<RefreshToken> userTokens = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
        userTokens.forEach(token -> token.setRevoked(true));
        refreshTokenRepository.saveAll(userTokens);
        log.info("Revoked all refresh tokens for user: {}", userId);
    }

    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteByExpiresAtBeforeOrRevokedTrue(LocalDateTime.now());
        log.info("Cleaned up expired and revoked refresh tokens");
    }

    private void cleanupExcessiveTokens(String userId, int maxTokens) {
        int tokenCount = refreshTokenRepository.countByUserIdAndRevokedFalse(userId);
        if (tokenCount >= maxTokens) {
            List<RefreshToken> userTokens = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
            // Sort by creation date and revoke oldest tokens
            userTokens.stream()
                    .sorted(Comparator.comparing(RefreshToken::getCreatedAt))
                    .limit(tokenCount - maxTokens + 1)
                    .forEach(token -> token.setRevoked(true));
            refreshTokenRepository.saveAll(userTokens);
        }
    }
}
