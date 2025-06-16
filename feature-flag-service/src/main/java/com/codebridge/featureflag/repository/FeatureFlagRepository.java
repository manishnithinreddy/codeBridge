package com.codebridge.featureflag.repository;

import com.codebridge.featureflag.model.FeatureFlag;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for feature flag operations.
 */
public interface FeatureFlagRepository {
    
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
     * Saves a feature flag.
     * 
     * @param flag the flag to save
     * @return the saved flag
     */
    FeatureFlag saveFlag(FeatureFlag flag);
    
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

