package com.codebridge.security.service;

import com.codebridge.security.model.RefreshToken;
import com.codebridge.security.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for refresh token operations.
 */
@Service
public class RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private long refreshTokenDurationMs;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Creates a new refresh token for a user.
     *
     * @param userId the user ID
     * @return the created refresh token
     */
    @Transactional
    public RefreshToken createRefreshToken(UUID userId) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setUserId(userId);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setRevoked(false);
        
        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Finds a refresh token by its token value.
     *
     * @param token the token value
     * @return the refresh token if found
     */
    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    /**
     * Finds all refresh tokens for a user.
     *
     * @param userId the user ID
     * @return list of refresh tokens
     */
    @Transactional(readOnly = true)
    public List<RefreshToken> findByUserId(UUID userId) {
        return refreshTokenRepository.findByUserId(userId);
    }

    /**
     * Finds all active refresh tokens for a user.
     *
     * @param userId the user ID
     * @return list of active refresh tokens
     */
    @Transactional(readOnly = true)
    public List<RefreshToken> findActiveTokensByUserId(UUID userId) {
        return refreshTokenRepository.findByUserIdAndRevoked(userId, false);
    }

    /**
     * Revokes a refresh token.
     *
     * @param token the token value
     * @return true if the token was revoked, false otherwise
     */
    @Transactional
    public boolean revokeRefreshToken(String token) {
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByToken(token);
        
        if (refreshToken.isPresent()) {
            RefreshToken tokenToRevoke = refreshToken.get();
            tokenToRevoke.setRevoked(true);
            refreshTokenRepository.save(tokenToRevoke);
            
            logger.info("Revoked refresh token: {}", tokenToRevoke.getId());
            
            return true;
        }
        
        return false;
    }

    /**
     * Revokes all refresh tokens for a user.
     *
     * @param userId the user ID
     * @return the number of revoked tokens
     */
    @Transactional
    public int revokeAllUserTokens(UUID userId) {
        int result = refreshTokenRepository.revokeAllUserTokens(userId);
        
        if (result > 0) {
            logger.info("Revoked {} refresh tokens for user: {}", result, userId);
        }
        
        return result;
    }

    /**
     * Verifies if a refresh token is valid.
     *
     * @param token the token value
     * @return true if the token is valid, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now()) || token.isRevoked()) {
            return false;
        }
        
        return true;
    }

    /**
     * Scheduled task to clean up expired tokens.
     * Runs every day at midnight.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            int deleted = refreshTokenRepository.deleteAllExpiredTokens(Instant.now());
            if (deleted > 0) {
                logger.info("Cleaned up {} expired refresh tokens", deleted);
            }
        } catch (Exception e) {
            logger.error("Error cleaning up expired refresh tokens", e);
        }
    }
}

