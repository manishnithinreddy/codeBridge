package com.codebridge.monitoring.scalability.service.impl;

import com.codebridge.monitoring.scalability.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based implementation of the IdempotencyService.
 */
@RequiredArgsConstructor
public class RedisIdempotencyService implements IdempotencyService {

    private static final String KEY_PREFIX = "idempotency:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final int expirationHours;

    @Override
    public <T> boolean recordKey(String key, T result) {
        String redisKey = KEY_PREFIX + key;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(redisKey, result, expirationHours, TimeUnit.HOURS);
        return Boolean.TRUE.equals(success);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getResult(String key, Class<T> resultClass) {
        String redisKey = KEY_PREFIX + key;
        Object result = redisTemplate.opsForValue().get(redisKey);
        
        if (result == null) {
            return Optional.empty();
        }
        
        if (resultClass.isInstance(result)) {
            return Optional.of((T) result);
        }
        
        throw new ClassCastException("Stored result is not of type " + resultClass.getName());
    }

    @Override
    public boolean exists(String key) {
        String redisKey = KEY_PREFIX + key;
        Boolean exists = redisTemplate.hasKey(redisKey);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public void removeKey(String key) {
        String redisKey = KEY_PREFIX + key;
        redisTemplate.delete(redisKey);
    }
}

