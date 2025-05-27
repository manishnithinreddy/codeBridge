package com.codebridge.security.repository;

import com.codebridge.core.repository.BaseRepository;
import com.codebridge.security.model.RefreshToken;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for RefreshToken entity operations.
 */
@Repository
public interface RefreshTokenRepository extends BaseRepository<RefreshToken, UUID> {

    /**
     * Finds a refresh token by its token value.
     *
     * @param token the token value
     * @return the refresh token if found
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Finds all refresh tokens for a user.
     *
     * @param userId the user ID
     * @return list of refresh tokens
     */
    List<RefreshToken> findByUserId(UUID userId);

    /**
     * Finds all refresh tokens for a user with a specific revoked status.
     *
     * @param userId the user ID
     * @param revoked the revoked status
     * @return list of refresh tokens
     */
    List<RefreshToken> findByUserIdAndRevoked(UUID userId, boolean revoked);

    /**
     * Revokes all refresh tokens for a user.
     *
     * @param userId the user ID
     * @return the number of revoked tokens
     */
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.userId = :userId AND r.revoked = false")
    int revokeAllUserTokens(@Param("userId") UUID userId);

    /**
     * Deletes all expired tokens.
     *
     * @param now the current time
     * @return the number of deleted tokens
     */
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiryDate < :now")
    int deleteAllExpiredTokens(@Param("now") Instant now);
}

