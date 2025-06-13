package com.codebridge.scalability.service.impl;

import com.codebridge.scalability.service.IdempotencyService;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Hazelcast-based implementation of the IdempotencyService.
 */
@RequiredArgsConstructor
public class HazelcastIdempotencyService implements IdempotencyService {

    private static final String MAP_NAME = "idempotency-store";

    private final HazelcastInstance hazelcastInstance;
    private final int expirationHours;

    @Override
    public <T> boolean recordKey(String key, T result) {
        IMap<String, Object> map = hazelcastInstance.getMap(MAP_NAME);
        Object existing = map.putIfAbsent(key, result);
        
        if (existing == null) {
            map.setTtl(key, expirationHours, TimeUnit.HOURS);
            return true;
        }
        
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getResult(String key, Class<T> resultClass) {
        IMap<String, Object> map = hazelcastInstance.getMap(MAP_NAME);
        Object result = map.get(key);
        
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
        IMap<String, Object> map = hazelcastInstance.getMap(MAP_NAME);
        return map.containsKey(key);
    }

    @Override
    public void removeKey(String key) {
        IMap<String, Object> map = hazelcastInstance.getMap(MAP_NAME);
        map.remove(key);
    }
}

