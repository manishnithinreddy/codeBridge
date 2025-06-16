package com.codebridge.featureflag.service;

import com.codebridge.featureflag.model.EvaluationContext;
import com.codebridge.featureflag.model.EvaluationResult;
import com.codebridge.featureflag.model.FeatureFlag;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for feature flag operations.
 */
public interface FeatureFlagService {
    
    /**
     * Gets a feature flag by key and namespace.
     * 
     * @param key the flag key
     * @param namespace the namespace
     * @return an Optional containing the flag if found, empty otherwise
     */
    Optional<FeatureFlag> getFlag(String key, String namespace);
    
    /**
     * Gets multiple feature flags by keys and namespace.
     * 
     * @param keys the flag keys
     * @param namespace the namespace
     * @return a list of found flags
     */
    List<FeatureFlag> getFlags(List<String> keys, String namespace);
    
    /**
     * Evaluates a feature flag with context.
     * 
     * @param key the flag key
     * @param namespace the namespace
     * @param context the evaluation context
     * @return the evaluation result
     */
    EvaluationResult evaluateFlag(String key, String namespace, EvaluationContext context);
    
    /**
     * Sets a feature flag.
     * 
     * @param flag the flag to set
     * @return the saved flag
     */
    FeatureFlag setFlag(FeatureFlag flag);
    
    /**
     * Deletes a feature flag.
     * 
     * @param key the flag key
     * @param namespace the namespace
     * @return true if the flag was deleted, false otherwise
     */
    boolean deleteFlag(String key, String namespace);
    
    /**
     * Lists all feature flags in a namespace.
     * 
     * @param namespace the namespace
     * @param prefix optional prefix filter
     * @param tagFilter optional tag filter
     * @param offset the offset for pagination
     * @param limit the limit for pagination
     * @return a list of flags
     */
    List<FeatureFlag> listFlags(String namespace, String prefix, String tagFilter, int offset, int limit);
    
    /**
     * Counts the total number of flags in a namespace.
     * 
     * @param namespace the namespace
     * @param prefix optional prefix filter
     * @param tagFilter optional tag filter
     * @return the count of flags
     */
    int countFlags(String namespace, String prefix, String tagFilter);
}

