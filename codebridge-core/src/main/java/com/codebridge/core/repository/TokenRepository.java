package com.codebridge.core.repository;

import com.codebridge.core.model.Token;
import com.codebridge.core.model.Token.TokenType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenRepository extends JpaRepository<Token, UUID> {
    
    Optional<Token> findByValue(String value);
    
    List<Token> findByUserId(UUID userId);
    
    Page<Token> findByUserId(UUID userId, Pageable pageable);
    
    List<Token> findByTeamId(UUID teamId);
    
    Page<Token> findByTeamId(UUID teamId, Pageable pageable);
    
    List<Token> findByType(TokenType type);
    
    Page<Token> findByType(TokenType type, Pageable pageable);
    
    List<Token> findByUserIdAndType(UUID userId, TokenType type);
    
    List<Token> findByTeamIdAndType(UUID teamId, TokenType type);
    
    @Query("SELECT t FROM Token t WHERE t.expiresAt < :now AND t.revoked = false")
    List<Token> findExpiredTokens(@Param("now") LocalDateTime now);
    
    @Query("SELECT t FROM Token t WHERE t.userId = :userId AND t.type = :type AND t.revoked = false AND t.expiresAt > :now")
    List<Token> findValidTokensByUserIdAndType(@Param("userId") UUID userId, @Param("type") TokenType type, @Param("now") LocalDateTime now);
    
    @Query("SELECT t FROM Token t WHERE t.teamId = :teamId AND t.type = :type AND t.revoked = false AND t.expiresAt > :now")
    List<Token> findValidTokensByTeamIdAndType(@Param("teamId") UUID teamId, @Param("type") TokenType type, @Param("now") LocalDateTime now);
}

