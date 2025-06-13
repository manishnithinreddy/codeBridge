package com.codebridge.security.apikey.service;

import com.codebridge.security.apikey.dto.ApiKeyCreationRequest;
import com.codebridge.security.apikey.dto.ApiKeyDto;
import com.codebridge.security.apikey.dto.ApiKeyResponse;
import com.codebridge.security.apikey.model.ApiKey;
import com.codebridge.security.apikey.repository.ApiKeyRepository;
import com.codebridge.security.audit.AuditLogger;
import com.codebridge.security.rbac.model.Permission;
import com.codebridge.security.rbac.service.RbacService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Service for API key management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;
    private final RbacService rbacService;
    private final AuditLogger auditLogger;
    private final RedisTemplate<String, String> redisTemplate;
    
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String API_KEY_PREFIX = "cbk_";
    private static final int PREFIX_LENGTH = 8;
    private static final int KEY_LENGTH = 32;
    
    @Value("${security.api-key.expiration-days}")
    private int defaultExpirationDays;
    
    @Value("${security.api-key.max-keys-per-user}")
    private int maxKeysPerUser;

    /**
     * Creates an API key.
     *
     * @param request The API key creation request
     * @return The API key response
     */
    @Transactional
    public ApiKeyResponse createApiKey(ApiKeyCreationRequest request) {
        // Enforce permission
        rbacService.enforcePermission(Permission.createPermissionName(
                Permission.ResourceType.API_KEY, Permission.ActionType.CREATE));
        
        // Get current user ID
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());
        
        // Check if user has reached the maximum number of API keys
        LocalDateTime now = LocalDateTime.now();
        long activeKeyCount = apiKeyRepository.countActiveKeysByUserId(userId, now);
        
        if (activeKeyCount >= maxKeysPerUser) {
            throw new IllegalStateException("Maximum number of API keys reached. Please revoke an existing key before creating a new one.");
        }
        
        // Generate API key
        String keyPrefix = generateKeyPrefix();
        String keyValue = generateKeyValue();
        String salt = generateSalt();
        String keyHash = passwordEncoder.encode(keyValue + salt);
        
        // Calculate expiration date
        LocalDateTime expirationDate = request.getExpirationDays() != null
                ? now.plusDays(request.getExpirationDays())
                : now.plusDays(defaultExpirationDays);
        
        // Create API key entity
        ApiKey apiKey = ApiKey.builder()
                .name(request.getName())
                .keyPrefix(keyPrefix)
                .keyHash(keyHash)
                .salt(salt)
                .userId(userId)
                .expirationDate(expirationDate)
                .enabled(true)
                .scopes(new HashSet<>(request.getScopes()))
                .ipRestrictions(request.getIpRestrictions() != null
                        ? new HashSet<>(request.getIpRestrictions())
                        : new HashSet<>())
                .rateLimit(request.getRateLimit())
                .build();
        
        ApiKey savedApiKey = apiKeyRepository.save(apiKey);
        
        // Log API key creation
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", userId);
        metadata.put("apiKeyId", savedApiKey.getId());
        metadata.put("apiKeyName", savedApiKey.getName());
        metadata.put("expirationDate", savedApiKey.getExpirationDate());
        
        auditLogger.logSecurityEvent(
                "API_KEY_CREATED",
                "API key created",
                metadata
        );
        
        // Return full API key only once
        String fullApiKey = API_KEY_PREFIX + keyPrefix + "." + keyValue;
        
        return ApiKeyResponse.builder()
                .id(savedApiKey.getId())
                .name(savedApiKey.getName())
                .apiKey(fullApiKey)
                .prefix(keyPrefix)
                .expirationDate(savedApiKey.getExpirationDate())
                .scopes(savedApiKey.getScopes())
                .ipRestrictions(savedApiKey.getIpRestrictions())
                .rateLimit(savedApiKey.getRateLimit())
                .createdAt(savedApiKey.getCreatedAt())
                .build();
    }

    /**
     * Gets API keys for the current user.
     *
     * @return The API keys
     */
    public List<ApiKeyDto> getCurrentUserApiKeys() {
        // Get current user ID
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.parseLong(authentication.getName());
        
        return getUserApiKeys(userId);
    }

    /**
     * Gets API keys for a user.
     *
     * @param userId The user ID
     * @return The API keys
     */
    public List<ApiKeyDto> getUserApiKeys(Long userId) {
        // Enforce permission for viewing other users' API keys
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.parseLong(authentication.getName());
        
        if (!userId.equals(currentUserId)) {
            rbacService.enforcePermission(Permission.createPermissionName(
                    Permission.ResourceType.API_KEY, Permission.ActionType.READ));
        }
        
        List<ApiKey> apiKeys = apiKeyRepository.findByUserId(userId);
        List<ApiKeyDto> apiKeyDtos = new ArrayList<>();
        
        for (ApiKey apiKey : apiKeys) {
            apiKeyDtos.add(convertToApiKeyDto(apiKey));
        }
        
        return apiKeyDtos;
    }

    /**
     * Gets an API key by ID.
     *
     * @param id The API key ID
     * @return The API key
     */
    public ApiKeyDto getApiKeyById(Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API key not found"));
        
        // Enforce permission for viewing other users' API keys
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.parseLong(authentication.getName());
        
        if (!apiKey.getUserId().equals(currentUserId)) {
            rbacService.enforcePermission(Permission.createPermissionName(
                    Permission.ResourceType.API_KEY, Permission.ActionType.READ));
        }
        
        return convertToApiKeyDto(apiKey);
    }

    /**
     * Revokes an API key.
     *
     * @param id The API key ID
     * @param reason The revocation reason
     */
    @Transactional
    public void revokeApiKey(Long id, String reason) {
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API key not found"));
        
        // Enforce permission for revoking other users' API keys
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.parseLong(authentication.getName());
        
        if (!apiKey.getUserId().equals(currentUserId)) {
            rbacService.enforcePermission(Permission.createPermissionName(
                    Permission.ResourceType.API_KEY, Permission.ActionType.DELETE));
        }
        
        // Revoke API key
        apiKey.revoke(currentUserId, reason);
        apiKeyRepository.save(apiKey);
        
        // Log API key revocation
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", currentUserId);
        metadata.put("apiKeyId", apiKey.getId());
        metadata.put("apiKeyName", apiKey.getName());
        metadata.put("reason", reason);
        
        auditLogger.logSecurityEvent(
                "API_KEY_REVOKED",
                "API key revoked",
                metadata
        );
    }

    /**
     * Validates an API key.
     *
     * @param apiKey The API key
     * @param ipAddress The IP address
     * @return The API key entity, if valid
     */
    public ApiKey validateApiKey(String apiKey, String ipAddress) {
        if (apiKey == null || !apiKey.startsWith(API_KEY_PREFIX)) {
            return null;
        }
        
        // Extract prefix and key value
        String keyWithoutPrefix = apiKey.substring(API_KEY_PREFIX.length());
        int separatorIndex = keyWithoutPrefix.indexOf('.');
        
        if (separatorIndex == -1) {
            return null;
        }
        
        String prefix = keyWithoutPrefix.substring(0, separatorIndex);
        String keyValue = keyWithoutPrefix.substring(separatorIndex + 1);
        
        // Find API key by prefix
        ApiKey apiKeyEntity = apiKeyRepository.findByKeyPrefix(prefix)
                .orElse(null);
        
        if (apiKeyEntity == null || !apiKeyEntity.isValid()) {
            return null;
        }
        
        // Check IP restrictions
        if (!apiKeyEntity.getIpRestrictions().isEmpty() && !apiKeyEntity.getIpRestrictions().contains(ipAddress)) {
            // Log IP restriction violation
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("apiKeyId", apiKeyEntity.getId());
            metadata.put("apiKeyName", apiKeyEntity.getName());
            metadata.put("userId", apiKeyEntity.getUserId());
            metadata.put("ipAddress", ipAddress);
            
            auditLogger.logSecurityEvent(
                    "API_KEY_IP_RESTRICTION_VIOLATION",
                    "API key IP restriction violation",
                    metadata
            );
            
            return null;
        }
        
        // Verify key hash
        if (!passwordEncoder.matches(keyValue + apiKeyEntity.getSalt(), apiKeyEntity.getKeyHash())) {
            return null;
        }
        
        // Check rate limit
        if (apiKeyEntity.getRateLimit() != null) {
            String rateLimitKey = "rate_limit:api_key:" + apiKeyEntity.getId();
            String count = redisTemplate.opsForValue().get(rateLimitKey);
            
            if (count == null) {
                redisTemplate.opsForValue().set(rateLimitKey, "1", 1, TimeUnit.MINUTES);
            } else {
                long currentCount = Long.parseLong(count);
                
                if (currentCount >= apiKeyEntity.getRateLimit()) {
                    // Log rate limit exceeded
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("apiKeyId", apiKeyEntity.getId());
                    metadata.put("apiKeyName", apiKeyEntity.getName());
                    metadata.put("userId", apiKeyEntity.getUserId());
                    metadata.put("rateLimit", apiKeyEntity.getRateLimit());
                    
                    auditLogger.logSecurityEvent(
                            "API_KEY_RATE_LIMIT_EXCEEDED",
                            "API key rate limit exceeded",
                            metadata
                    );
                    
                    throw new AccessDeniedException("API key rate limit exceeded");
                }
                
                redisTemplate.opsForValue().increment(rateLimitKey);
            }
        }
        
        // Record API key usage
        apiKeyEntity.recordUsage();
        apiKeyRepository.save(apiKeyEntity);
        
        return apiKeyEntity;
    }

    /**
     * Rotates an API key.
     *
     * @param id The API key ID
     * @return The API key response
     */
    @Transactional
    public ApiKeyResponse rotateApiKey(Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API key not found"));
        
        // Enforce permission for rotating other users' API keys
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.parseLong(authentication.getName());
        
        if (!apiKey.getUserId().equals(currentUserId)) {
            rbacService.enforcePermission(Permission.createPermissionName(
                    Permission.ResourceType.API_KEY, Permission.ActionType.UPDATE));
        }
        
        // Generate new key value
        String keyValue = generateKeyValue();
        String salt = generateSalt();
        String keyHash = passwordEncoder.encode(keyValue + salt);
        
        // Update API key
        apiKey.setKeyHash(keyHash);
        apiKey.setSalt(salt);
        apiKey.setUpdatedAt(LocalDateTime.now());
        
        ApiKey savedApiKey = apiKeyRepository.save(apiKey);
        
        // Log API key rotation
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", currentUserId);
        metadata.put("apiKeyId", savedApiKey.getId());
        metadata.put("apiKeyName", savedApiKey.getName());
        
        auditLogger.logSecurityEvent(
                "API_KEY_ROTATED",
                "API key rotated",
                metadata
        );
        
        // Return full API key only once
        String fullApiKey = API_KEY_PREFIX + apiKey.getKeyPrefix() + "." + keyValue;
        
        return ApiKeyResponse.builder()
                .id(savedApiKey.getId())
                .name(savedApiKey.getName())
                .apiKey(fullApiKey)
                .prefix(apiKey.getKeyPrefix())
                .expirationDate(savedApiKey.getExpirationDate())
                .scopes(savedApiKey.getScopes())
                .ipRestrictions(savedApiKey.getIpRestrictions())
                .rateLimit(savedApiKey.getRateLimit())
                .createdAt(savedApiKey.getCreatedAt())
                .updatedAt(savedApiKey.getUpdatedAt())
                .build();
    }

    /**
     * Updates API key scopes.
     *
     * @param id The API key ID
     * @param scopes The scopes
     * @return The API key DTO
     */
    @Transactional
    public ApiKeyDto updateApiKeyScopes(Long id, Set<String> scopes) {
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API key not found"));
        
        // Enforce permission for updating other users' API keys
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.parseLong(authentication.getName());
        
        if (!apiKey.getUserId().equals(currentUserId)) {
            rbacService.enforcePermission(Permission.createPermissionName(
                    Permission.ResourceType.API_KEY, Permission.ActionType.UPDATE));
        }
        
        // Update scopes
        apiKey.setScopes(new HashSet<>(scopes));
        ApiKey savedApiKey = apiKeyRepository.save(apiKey);
        
        // Log API key scope update
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", currentUserId);
        metadata.put("apiKeyId", savedApiKey.getId());
        metadata.put("apiKeyName", savedApiKey.getName());
        metadata.put("scopes", scopes);
        
        auditLogger.logSecurityEvent(
                "API_KEY_SCOPES_UPDATED",
                "API key scopes updated",
                metadata
        );
        
        return convertToApiKeyDto(savedApiKey);
    }

    /**
     * Updates API key IP restrictions.
     *
     * @param id The API key ID
     * @param ipRestrictions The IP restrictions
     * @return The API key DTO
     */
    @Transactional
    public ApiKeyDto updateApiKeyIpRestrictions(Long id, Set<String> ipRestrictions) {
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API key not found"));
        
        // Enforce permission for updating other users' API keys
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.parseLong(authentication.getName());
        
        if (!apiKey.getUserId().equals(currentUserId)) {
            rbacService.enforcePermission(Permission.createPermissionName(
                    Permission.ResourceType.API_KEY, Permission.ActionType.UPDATE));
        }
        
        // Update IP restrictions
        apiKey.setIpRestrictions(new HashSet<>(ipRestrictions));
        ApiKey savedApiKey = apiKeyRepository.save(apiKey);
        
        // Log API key IP restrictions update
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", currentUserId);
        metadata.put("apiKeyId", savedApiKey.getId());
        metadata.put("apiKeyName", savedApiKey.getName());
        metadata.put("ipRestrictions", ipRestrictions);
        
        auditLogger.logSecurityEvent(
                "API_KEY_IP_RESTRICTIONS_UPDATED",
                "API key IP restrictions updated",
                metadata
        );
        
        return convertToApiKeyDto(savedApiKey);
    }

    /**
     * Updates API key rate limit.
     *
     * @param id The API key ID
     * @param rateLimit The rate limit
     * @return The API key DTO
     */
    @Transactional
    public ApiKeyDto updateApiKeyRateLimit(Long id, Integer rateLimit) {
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API key not found"));
        
        // Enforce permission for updating other users' API keys
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.parseLong(authentication.getName());
        
        if (!apiKey.getUserId().equals(currentUserId)) {
            rbacService.enforcePermission(Permission.createPermissionName(
                    Permission.ResourceType.API_KEY, Permission.ActionType.UPDATE));
        }
        
        // Update rate limit
        apiKey.setRateLimit(rateLimit);
        ApiKey savedApiKey = apiKeyRepository.save(apiKey);
        
        // Log API key rate limit update
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", currentUserId);
        metadata.put("apiKeyId", savedApiKey.getId());
        metadata.put("apiKeyName", savedApiKey.getName());
        metadata.put("rateLimit", rateLimit);
        
        auditLogger.logSecurityEvent(
                "API_KEY_RATE_LIMIT_UPDATED",
                "API key rate limit updated",
                metadata
        );
        
        return convertToApiKeyDto(savedApiKey);
    }

    /**
     * Converts an API key to an API key DTO.
     *
     * @param apiKey The API key
     * @return The API key DTO
     */
    private ApiKeyDto convertToApiKeyDto(ApiKey apiKey) {
        return ApiKeyDto.builder()
                .id(apiKey.getId())
                .name(apiKey.getName())
                .prefix(apiKey.getKeyPrefix())
                .userId(apiKey.getUserId())
                .expirationDate(apiKey.getExpirationDate())
                .enabled(apiKey.isEnabled())
                .lastUsed(apiKey.getLastUsed())
                .usageCount(apiKey.getUsageCount())
                .rateLimit(apiKey.getRateLimit())
                .scopes(apiKey.getScopes())
                .ipRestrictions(apiKey.getIpRestrictions())
                .createdAt(apiKey.getCreatedAt())
                .updatedAt(apiKey.getUpdatedAt())
                .revokedAt(apiKey.getRevokedAt())
                .revokedBy(apiKey.getRevokedBy())
                .revocationReason(apiKey.getRevocationReason())
                .build();
    }

    /**
     * Generates a key prefix.
     *
     * @return The key prefix
     */
    private String generateKeyPrefix() {
        byte[] bytes = new byte[PREFIX_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).substring(0, PREFIX_LENGTH);
    }

    /**
     * Generates a key value.
     *
     * @return The key value
     */
    private String generateKeyValue() {
        byte[] bytes = new byte[KEY_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Generates a salt.
     *
     * @return The salt
     */
    private String generateSalt() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}

