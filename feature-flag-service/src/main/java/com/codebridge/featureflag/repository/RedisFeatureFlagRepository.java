package com.codebridge.featureflag.repository;

import com.codebridge.featureflag.model.FeatureFlag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Redis implementation of the FeatureFlagRepository.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class RedisFeatureFlagRepository implements FeatureFlagRepository {
    
    private static final String FLAG_KEY_PREFIX = "feature-flag:";
    private static final String NAMESPACE_KEY_PREFIX = "feature-flag-namespace:";
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Constructs the Redis key for a flag.
     * 
     * @param key the flag key
     * @param namespace the namespace
     * @return the Redis key
     */
    private String constructKey(String key, String namespace) {
        return FLAG_KEY_PREFIX + namespace + ":" + key;
    }
    
    /**
     * Constructs the Redis key for a namespace.
     * 
     * @param namespace the namespace
     * @return the Redis key
     */
    private String constructNamespaceKey(String namespace) {
        return NAMESPACE_KEY_PREFIX + namespace;
    }
    
    @Override
    public Optional<FeatureFlag> getFlag(String key, String namespace) {
        String redisKey = constructKey(key, namespace);
        FeatureFlag flag = (FeatureFlag) redisTemplate.opsForValue().get(redisKey);
        return Optional.ofNullable(flag);
    }
    
    @Override
    public List<FeatureFlag> getFlags(List<String> keys, String namespace) {
        List<String> redisKeys = keys.stream()
                .map(key -> constructKey(key, namespace))
                .collect(Collectors.toList());
        
        List<Object> values = redisTemplate.opsForValue().multiGet(redisKeys);
        
        if (values == null) {
            return new ArrayList<>();
        }
        
        return values.stream()
                .filter(value -> value instanceof FeatureFlag)
                .map(value -> (FeatureFlag) value)
                .collect(Collectors.toList());
    }
    
    @Override
    public FeatureFlag saveFlag(FeatureFlag flag) {
        String redisKey = constructKey(flag.getKey(), flag.getNamespace());
        String namespaceKey = constructNamespaceKey(flag.getNamespace());
        
        // Save the flag
        redisTemplate.opsForValue().set(redisKey, flag);
        
        // Add the flag key to the namespace set
        redisTemplate.opsForSet().add(namespaceKey, flag.getKey());
        
        return flag;
    }
    
    @Override
    public boolean deleteFlag(String key, String namespace) {
        String redisKey = constructKey(key, namespace);
        String namespaceKey = constructNamespaceKey(namespace);
        
        // Remove the flag key from the namespace set
        redisTemplate.opsForSet().remove(namespaceKey, key);
        
        // Delete the flag
        return Boolean.TRUE.equals(redisTemplate.delete(redisKey));
    }
    
    @Override
    public List<FeatureFlag> listFlags(String namespace, String prefix, String tagFilter, int offset, int limit) {
        String namespaceKey = constructNamespaceKey(namespace);
        
        // Get all flag keys in the namespace
        List<Object> flagKeys = new ArrayList<>(redisTemplate.opsForSet().members(namespaceKey));
        
        // Apply prefix filter if provided
        if (prefix != null && !prefix.isEmpty()) {
            flagKeys = flagKeys.stream()
                    .filter(key -> ((String) key).startsWith(prefix))
                    .collect(Collectors.toList());
        }
        
        // Apply pagination
        int end = Math.min(offset + limit, flagKeys.size());
        if (offset >= flagKeys.size() || offset >= end) {
            return new ArrayList<>();
        }
        
        List<Object> paginatedKeys = flagKeys.subList(offset, end);
        
        // Get the flags
        List<String> redisKeys = paginatedKeys.stream()
                .map(key -> constructKey((String) key, namespace))
                .collect(Collectors.toList());
        
        List<Object> values = redisTemplate.opsForValue().multiGet(redisKeys);
        
        if (values == null) {
            return new ArrayList<>();
        }
        
        List<FeatureFlag> flags = values.stream()
                .filter(value -> value instanceof FeatureFlag)
                .map(value -> (FeatureFlag) value)
                .collect(Collectors.toList());
        
        // Apply tag filter if provided
        if (tagFilter != null && !tagFilter.isEmpty()) {
            flags = flags.stream()
                    .filter(flag -> {
                        Map<String, String> tags = flag.getTags();
                        return tags != null && tags.containsKey(tagFilter);
                    })
                    .collect(Collectors.toList());
        }
        
        return flags;
    }
    
    @Override
    public int countFlags(String namespace, String prefix, String tagFilter) {
        String namespaceKey = constructNamespaceKey(namespace);
        
        // Get all flag keys in the namespace
        List<Object> flagKeys = new ArrayList<>(redisTemplate.opsForSet().members(namespaceKey));
        
        // Apply prefix filter if provided
        if (prefix != null && !prefix.isEmpty()) {
            flagKeys = flagKeys.stream()
                    .filter(key -> ((String) key).startsWith(prefix))
                    .collect(Collectors.toList());
        }
        
        // If no tag filter, return the count
        if (tagFilter == null || tagFilter.isEmpty()) {
            return flagKeys.size();
        }
        
        // Get the flags to apply tag filter
        List<String> redisKeys = flagKeys.stream()
                .map(key -> constructKey((String) key, namespace))
                .collect(Collectors.toList());
        
        List<Object> values = redisTemplate.opsForValue().multiGet(redisKeys);
        
        if (values == null) {
            return 0;
        }
        
        // Count flags with the tag
        return (int) values.stream()
                .filter(value -> value instanceof FeatureFlag)
                .map(value -> (FeatureFlag) value)
                .filter(flag -> {
                    Map<String, String> tags = flag.getTags();
                    return tags != null && tags.containsKey(tagFilter);
                })
                .count();
    }
}

