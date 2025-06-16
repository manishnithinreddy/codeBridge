package com.codebridge.featureflag.service;

import com.codebridge.featureflag.model.EvaluationContext;
import com.codebridge.featureflag.model.EvaluationResult;
import com.codebridge.featureflag.model.FeatureFlag;
import com.codebridge.featureflag.model.FlagRule;
import com.codebridge.featureflag.model.FlagValue;
import com.codebridge.featureflag.repository.FeatureFlagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of the FeatureFlagService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureFlagServiceImpl implements FeatureFlagService {
    
    private final FeatureFlagRepository repository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic flagUpdatesTopic;
    
    @Value("${feature-flag.defaults.namespace}")
    private String defaultNamespace;
    
    @Override
    @Cacheable(value = "flags", key = "#namespace + ':' + #key")
    public Optional<FeatureFlag> getFlag(String key, String namespace) {
        namespace = namespace != null ? namespace : defaultNamespace;
        return repository.getFlag(key, namespace);
    }
    
    @Override
    public List<FeatureFlag> getFlags(List<String> keys, String namespace) {
        namespace = namespace != null ? namespace : defaultNamespace;
        return repository.getFlags(keys, namespace);
    }
    
    @Override
    public EvaluationResult evaluateFlag(String key, String namespace, EvaluationContext context) {
        namespace = namespace != null ? namespace : defaultNamespace;
        
        // Get the flag
        Optional<FeatureFlag> optionalFlag = getFlag(key, namespace);
        
        // If flag doesn't exist, return default result
        if (optionalFlag.isEmpty()) {
            return EvaluationResult.builder()
                    .flagKey(key)
                    .value(null)
                    .reason(EvaluationResult.EvaluationReason.builder()
                            .type(EvaluationResult.ReasonType.ERROR)
                            .description("Flag not found")
                            .build())
                    .build();
        }
        
        FeatureFlag flag = optionalFlag.get();
        
        // Check if flag is expired
        if (flag.isExpired()) {
            return EvaluationResult.builder()
                    .flagKey(key)
                    .value(null)
                    .reason(EvaluationResult.EvaluationReason.builder()
                            .type(EvaluationResult.ReasonType.DISABLED)
                            .description("Flag is expired")
                            .build())
                    .build();
        }
        
        // Convert context to map for rule evaluation
        Map<String, Object> contextMap = context.toMap();
        
        // Evaluate rules
        if (flag.getRules() != null) {
            for (FlagRule rule : flag.getRules()) {
                boolean ruleMatches = true;
                
                // Check all conditions
                if (rule.getConditions() != null) {
                    for (FlagRule.Condition condition : rule.getConditions()) {
                        if (!condition.evaluate(contextMap)) {
                            ruleMatches = false;
                            break;
                        }
                    }
                }
                
                // If rule matches, return its value
                if (ruleMatches) {
                    return EvaluationResult.builder()
                            .flagKey(key)
                            .value(rule.getValue())
                            .ruleId(rule.getId())
                            .reason(EvaluationResult.EvaluationReason.builder()
                                    .type(EvaluationResult.ReasonType.RULE_MATCH)
                                    .ruleId(rule.getId())
                                    .description("Rule matched: " + rule.getName())
                                    .build())
                            .build();
                }
            }
        }
        
        // Check percentage-based distribution
        if (flag.getDistribution() != null && !flag.getDistribution().isEmpty() && context.getUserId() != null) {
            // Generate a hash from the user ID and flag key
            int hash = (context.getUserId() + key).hashCode();
            double normalizedHash = (hash & 0x7FFFFFFF) / (double) 0x7FFFFFFF; // Normalize to 0-1
            
            double cumulativePercentage = 0.0;
            for (Map.Entry<String, Double> entry : flag.getDistribution().entrySet()) {
                cumulativePercentage += entry.getValue() / 100.0;
                
                if (normalizedHash < cumulativePercentage) {
                    // Find the variation value
                    if (flag.getValue() != null && flag.getValue().getType() == FlagValue.ValueType.STRING &&
                            flag.getValue().getStringValue().equals(entry.getKey())) {
                        return EvaluationResult.builder()
                                .flagKey(key)
                                .value(flag.getValue())
                                .variationId(entry.getKey())
                                .reason(EvaluationResult.EvaluationReason.builder()
                                        .type(EvaluationResult.ReasonType.FALLTHROUGH)
                                        .description("Percentage-based distribution")
                                        .build())
                                .build();
                    }
                }
            }
        }
        
        // Default to the flag's value
        return EvaluationResult.builder()
                .flagKey(key)
                .value(flag.getValue())
                .reason(EvaluationResult.EvaluationReason.builder()
                        .type(EvaluationResult.ReasonType.DEFAULT)
                        .description("Default value")
                        .build())
                .build();
    }
    
    @Override
    @CacheEvict(value = "flags", key = "#flag.namespace + ':' + #flag.key")
    public FeatureFlag setFlag(FeatureFlag flag) {
        // Set namespace to default if not provided
        if (flag.getNamespace() == null) {
            flag.setNamespace(defaultNamespace);
        }
        
        // Set timestamps
        Instant now = Instant.now();
        
        // Check if flag already exists
        Optional<FeatureFlag> existingFlag = repository.getFlag(flag.getKey(), flag.getNamespace());
        
        if (existingFlag.isPresent()) {
            // Update existing flag
            flag.setCreatedAt(existingFlag.get().getCreatedAt());
            flag.setCreatedBy(existingFlag.get().getCreatedBy());
            flag.setUpdatedAt(now);
            
            // Generate new version
            flag.setVersion(UUID.randomUUID().toString());
        } else {
            // Create new flag
            flag.setCreatedAt(now);
            flag.setUpdatedAt(now);
            flag.setVersion(UUID.randomUUID().toString());
        }
        
        // Save the flag
        FeatureFlag savedFlag = repository.saveFlag(flag);
        
        // Publish update event
        redisTemplate.convertAndSend(flagUpdatesTopic.getTopic(), savedFlag);
        
        return savedFlag;
    }
    
    @Override
    @CacheEvict(value = "flags", key = "#namespace + ':' + #key")
    public boolean deleteFlag(String key, String namespace) {
        namespace = namespace != null ? namespace : defaultNamespace;
        
        // Get the flag before deleting
        Optional<FeatureFlag> flag = repository.getFlag(key, namespace);
        
        // Delete the flag
        boolean deleted = repository.deleteFlag(key, namespace);
        
        // Publish delete event if successful
        if (deleted && flag.isPresent()) {
            redisTemplate.convertAndSend(flagUpdatesTopic.getTopic(), flag.get());
        }
        
        return deleted;
    }
    
    @Override
    public List<FeatureFlag> listFlags(String namespace, String prefix, String tagFilter, int offset, int limit) {
        namespace = namespace != null ? namespace : defaultNamespace;
        return repository.listFlags(namespace, prefix, tagFilter, offset, limit);
    }
    
    @Override
    public int countFlags(String namespace, String prefix, String tagFilter) {
        namespace = namespace != null ? namespace : defaultNamespace;
        return repository.countFlags(namespace, prefix, tagFilter);
    }
}

