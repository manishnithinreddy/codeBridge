package com.codebridge.featureflag.controller;

import com.codebridge.featureflag.model.EvaluationContext;
import com.codebridge.featureflag.model.EvaluationResult;
import com.codebridge.featureflag.model.FeatureFlag;
import com.codebridge.featureflag.service.FeatureFlagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST controller for the Feature Flag Service.
 */
@RestController
@RequestMapping("/api/v1/flags")
@RequiredArgsConstructor
@Slf4j
public class FeatureFlagController {
    
    private final FeatureFlagService featureFlagService;
    
    /**
     * Gets a feature flag by key and namespace.
     * 
     * @param key the flag key
     * @param namespace the namespace (optional)
     * @return the flag if found, 404 otherwise
     */
    @GetMapping("/{key}")
    public ResponseEntity<FeatureFlag> getFlag(@PathVariable String key, @RequestParam(required = false) String namespace) {
        Optional<FeatureFlag> flag = featureFlagService.getFlag(key, namespace);
        return flag.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Gets multiple feature flags by keys and namespace.
     * 
     * @param keys the flag keys
     * @param namespace the namespace (optional)
     * @return the flags
     */
    @GetMapping
    public ResponseEntity<List<FeatureFlag>> getFlags(@RequestParam List<String> keys, @RequestParam(required = false) String namespace) {
        List<FeatureFlag> flags = featureFlagService.getFlags(keys, namespace);
        return ResponseEntity.ok(flags);
    }
    
    /**
     * Evaluates a feature flag with context.
     * 
     * @param key the flag key
     * @param context the evaluation context
     * @param namespace the namespace (optional)
     * @return the evaluation result
     */
    @PostMapping("/{key}/evaluate")
    public ResponseEntity<EvaluationResult> evaluateFlag(@PathVariable String key, @RequestBody EvaluationContext context, @RequestParam(required = false) String namespace) {
        EvaluationResult result = featureFlagService.evaluateFlag(key, namespace, context);
        return ResponseEntity.ok(result);
    }
    
    /**
     * Sets a feature flag.
     * 
     * @param flag the flag to set
     * @return the saved flag
     */
    @PostMapping
    public ResponseEntity<FeatureFlag> setFlag(@RequestBody FeatureFlag flag) {
        FeatureFlag savedFlag = featureFlagService.setFlag(flag);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedFlag);
    }
    
    /**
     * Updates a feature flag.
     * 
     * @param key the flag key
     * @param flag the flag to update
     * @param namespace the namespace (optional)
     * @return the updated flag
     */
    @PutMapping("/{key}")
    public ResponseEntity<FeatureFlag> updateFlag(@PathVariable String key, @RequestBody FeatureFlag flag, @RequestParam(required = false) String namespace) {
        // Set key and namespace
        flag.setKey(key);
        if (namespace != null) {
            flag.setNamespace(namespace);
        }
        
        FeatureFlag updatedFlag = featureFlagService.setFlag(flag);
        return ResponseEntity.ok(updatedFlag);
    }
    
    /**
     * Deletes a feature flag.
     * 
     * @param key the flag key
     * @param namespace the namespace (optional)
     * @return 204 if deleted, 404 otherwise
     */
    @DeleteMapping("/{key}")
    public ResponseEntity<Void> deleteFlag(@PathVariable String key, @RequestParam(required = false) String namespace) {
        boolean deleted = featureFlagService.deleteFlag(key, namespace);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
    
    /**
     * Lists all feature flags in a namespace.
     * 
     * @param namespace the namespace (optional)
     * @param prefix optional prefix filter
     * @param tagFilter optional tag filter
     * @param offset the offset for pagination (default: 0)
     * @param limit the limit for pagination (default: 10)
     * @return the flags
     */
    @GetMapping("/list")
    public ResponseEntity<ListFlagsResponse> listFlags(
            @RequestParam(required = false) String namespace,
            @RequestParam(required = false) String prefix,
            @RequestParam(required = false) String tagFilter,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit) {
        
        List<FeatureFlag> flags = featureFlagService.listFlags(namespace, prefix, tagFilter, offset, limit);
        int totalCount = featureFlagService.countFlags(namespace, prefix, tagFilter);
        
        ListFlagsResponse response = new ListFlagsResponse(flags, totalCount, offset, limit);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Response class for listing flags.
     */
    public static class ListFlagsResponse {
        private final List<FeatureFlag> flags;
        private final int totalCount;
        private final int offset;
        private final int limit;
        private final boolean hasMore;
        
        public ListFlagsResponse(List<FeatureFlag> flags, int totalCount, int offset, int limit) {
            this.flags = flags;
            this.totalCount = totalCount;
            this.offset = offset;
            this.limit = limit;
            this.hasMore = offset + limit < totalCount;
        }
        
        public List<FeatureFlag> getFlags() {
            return flags;
        }
        
        public int getTotalCount() {
            return totalCount;
        }
        
        public int getOffset() {
            return offset;
        }
        
        public int getLimit() {
            return limit;
        }
        
        public boolean isHasMore() {
            return hasMore;
        }
    }
}

