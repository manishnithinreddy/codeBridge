package com.codebridge.auth.service;

import com.codebridge.auth.exception.AuthenticationException;
import com.codebridge.auth.model.RefreshToken;
import com.codebridge.auth.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for refresh token operations.
 */
@Service
public class RefreshTokenService {

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
    public Mono<RefreshToken> createRefreshToken(UUID userId) {
        return Mono.fromCallable(() -> {
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setId(UUID.randomUUID());
            refreshToken.setUserId(userId);
            refreshToken.setToken(UUID.randomUUID().toString());
            refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
            refreshToken.setRevoked(false);
            
            return refreshTokenRepository.save(refreshToken);
        });
    }

    /**
     * Verifies a refresh token.
     *
     * @param token the refresh token
     * @return the verified refresh token
     */
    @Transactional(readOnly = true)
    public Mono<RefreshToken> verifyExpiration(String token) {
        return Mono.fromCallable(() -> refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new AuthenticationException("Refresh token not found")))
                .flatMap(refreshToken -> {
                    if (refreshToken.isRevoked()) {
                        return Mono.error(new AuthenticationException("Refresh token was revoked"));
                    }
                    
                    if (refreshToken.getExpiryDate().compareTo(Instant.now()) < 0) {
                        refreshTokenRepository.delete(refreshToken);
                        return Mono.error(new AuthenticationException("Refresh token was expired"));
                    }
                    
                    return Mono.just(refreshToken);
                });
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
    public boolean revokeToken(String token) {
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByToken(token);
        if (refreshToken.isPresent()) {
            RefreshToken tokenToRevoke = refreshToken.get();
            tokenToRevoke.setRevoked(true);
            refreshTokenRepository.save(tokenToRevoke);
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
        return refreshTokenRepository.revokeAllUserTokens(userId);
    }

    /**
     * Scheduled task to delete expired tokens.
     * Runs daily at midnight.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void deleteExpiredTokens() {
        refreshTokenRepository.deleteAllExpiredTokens(Instant.now());
    }
}

