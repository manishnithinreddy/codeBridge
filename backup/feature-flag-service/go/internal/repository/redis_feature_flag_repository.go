package repository

import (
	"context"
	"encoding/json"
	"fmt"
	"strings"
	"sync"
	"time"

	"github.com/codebridge/feature-flag-service/internal/model"
	"github.com/go-redis/redis/v8"
)

// RedisFeatureFlagRepository implements FeatureFlagRepository using Redis
type RedisFeatureFlagRepository struct {
	client       *redis.Client
	ttlSeconds   int
	pubSubClient *redis.PubSub
	mutex        sync.Mutex
	callbacks    []func(*model.FeatureFlag)
}

// NewRedisFeatureFlagRepository creates a new Redis-backed feature flag repository
func NewRedisFeatureFlagRepository(client *redis.Client, ttlSeconds int) *RedisFeatureFlagRepository {
	return &RedisFeatureFlagRepository{
		client:     client,
		ttlSeconds: ttlSeconds,
		callbacks:  make([]func(*model.FeatureFlag), 0),
	}
}

// GetFlag gets a flag by key and namespace
func (r *RedisFeatureFlagRepository) GetFlag(ctx context.Context, key, namespace string) (*model.FeatureFlag, error) {
	// Construct the Redis key
	redisKey := getFlagKey(key, namespace)

	// Get the flag from Redis
	data, err := r.client.Get(ctx, redisKey).Result()
	if err != nil {
		if err == redis.Nil {
			return nil, nil // Flag not found
		}
		return nil, fmt.Errorf("failed to get flag from Redis: %w", err)
	}

	// Unmarshal the flag
	var flag model.FeatureFlag
	if err := json.Unmarshal([]byte(data), &flag); err != nil {
		return nil, fmt.Errorf("failed to unmarshal flag: %w", err)
	}

	return &flag, nil
}

// GetFlags gets multiple flags by keys and namespace
func (r *RedisFeatureFlagRepository) GetFlags(ctx context.Context, keys []string, namespace string) ([]*model.FeatureFlag, error) {
	if len(keys) == 0 {
		return make([]*model.FeatureFlag, 0), nil
	}

	// Construct Redis keys
	redisKeys := make([]string, len(keys))
	for i, key := range keys {
		redisKeys[i] = getFlagKey(key, namespace)
	}

	// Get flags from Redis
	data, err := r.client.MGet(ctx, redisKeys...).Result()
	if err != nil {
		return nil, fmt.Errorf("failed to get flags from Redis: %w", err)
	}

	// Unmarshal flags
	flags := make([]*model.FeatureFlag, 0, len(data))
	for i, item := range data {
		if item == nil {
			continue // Flag not found
		}

		var flag model.FeatureFlag
		if err := json.Unmarshal([]byte(item.(string)), &flag); err != nil {
			return nil, fmt.Errorf("failed to unmarshal flag: %w", err)
		}

		flags = append(flags, &flag)
	}

	return flags, nil
}

// SetFlag sets a flag
func (r *RedisFeatureFlagRepository) SetFlag(ctx context.Context, flag *model.FeatureFlag) error {
	// Update timestamps
	flag.UpdatedAt = time.Now()

	// Marshal the flag
	data, err := json.Marshal(flag)
	if err != nil {
		return fmt.Errorf("failed to marshal flag: %w", err)
	}

	// Construct the Redis key
	redisKey := getFlagKey(flag.Key, flag.Namespace)

	// Set the flag in Redis
	var ttl time.Duration
	if flag.Temporary && flag.ExpiresAt != nil {
		// Use the flag's expiration time
		ttl = time.Until(*flag.ExpiresAt)
	} else if r.ttlSeconds > 0 {
		// Use the repository's TTL
		ttl = time.Duration(r.ttlSeconds) * time.Second
	} else {
		// No TTL
		ttl = 0
	}

	if err := r.client.Set(ctx, redisKey, data, ttl).Err(); err != nil {
		return fmt.Errorf("failed to set flag in Redis: %w", err)
	}

	// Add to index
	if err := r.addToIndex(ctx, flag); err != nil {
		return fmt.Errorf("failed to add flag to index: %w", err)
	}

	// Publish update
	if err := r.publishUpdate(ctx, flag); err != nil {
		return fmt.Errorf("failed to publish flag update: %w", err)
	}

	return nil
}

// DeleteFlag deletes a flag
func (r *RedisFeatureFlagRepository) DeleteFlag(ctx context.Context, key, namespace string) error {
	// Get the flag first to remove from indexes
	flag, err := r.GetFlag(ctx, key, namespace)
	if err != nil {
		return fmt.Errorf("failed to get flag for deletion: %w", err)
	}

	if flag == nil {
		return nil // Flag not found, nothing to delete
	}

	// Construct the Redis key
	redisKey := getFlagKey(key, namespace)

	// Delete the flag from Redis
	if err := r.client.Del(ctx, redisKey).Err(); err != nil {
		return fmt.Errorf("failed to delete flag from Redis: %w", err)
	}

	// Remove from index
	if err := r.removeFromIndex(ctx, flag); err != nil {
		return fmt.Errorf("failed to remove flag from index: %w", err)
	}

	// Publish deletion
	flag.Value = nil // Clear the value to indicate deletion
	if err := r.publishUpdate(ctx, flag); err != nil {
		return fmt.Errorf("failed to publish flag deletion: %w", err)
	}

	return nil
}

// ListFlags lists flags by namespace and optional prefix
func (r *RedisFeatureFlagRepository) ListFlags(ctx context.Context, namespace, prefix string) ([]*model.FeatureFlag, error) {
	// Construct the Redis key pattern
	pattern := getNamespaceKey(namespace) + ":*"
	if prefix != "" {
		pattern = getNamespaceKey(namespace) + ":" + prefix + "*"
	}

	// Scan for matching keys
	var cursor uint64
	var keys []string
	var err error

	for {
		var batch []string
		cursor, batch, err = r.client.Scan(ctx, cursor, pattern, 100).Result()
		if err != nil {
			return nil, fmt.Errorf("failed to scan Redis keys: %w", err)
		}

		keys = append(keys, batch...)

		if cursor == 0 {
			break
		}
	}

	if len(keys) == 0 {
		return make([]*model.FeatureFlag, 0), nil
	}

	// Get flags from Redis
	data, err := r.client.MGet(ctx, keys...).Result()
	if err != nil {
		return nil, fmt.Errorf("failed to get flags from Redis: %w", err)
	}

	// Unmarshal flags
	flags := make([]*model.FeatureFlag, 0, len(data))
	for i, item := range data {
		if item == nil {
			continue // Flag not found
		}

		var flag model.FeatureFlag
		if err := json.Unmarshal([]byte(item.(string)), &flag); err != nil {
			return nil, fmt.Errorf("failed to unmarshal flag: %w", err)
		}

		flags = append(flags, &flag)
	}

	return flags, nil
}

// ListFlagsByTag lists flags by tag
func (r *RedisFeatureFlagRepository) ListFlagsByTag(ctx context.Context, namespace, tagKey, tagValue string) ([]*model.FeatureFlag, error) {
	// Construct the Redis key for the tag index
	indexKey := getTagIndexKey(namespace, tagKey, tagValue)

	// Get flag keys from the index
	flagKeys, err := r.client.SMembers(ctx, indexKey).Result()
	if err != nil {
		return nil, fmt.Errorf("failed to get flag keys from tag index: %w", err)
	}

	if len(flagKeys) == 0 {
		return make([]*model.FeatureFlag, 0), nil
	}

	// Get flags from Redis
	data, err := r.client.MGet(ctx, flagKeys...).Result()
	if err != nil {
		return nil, fmt.Errorf("failed to get flags from Redis: %w", err)
	}

	// Unmarshal flags
	flags := make([]*model.FeatureFlag, 0, len(data))
	for i, item := range data {
		if item == nil {
			continue // Flag not found
		}

		var flag model.FeatureFlag
		if err := json.Unmarshal([]byte(item.(string)), &flag); err != nil {
			return nil, fmt.Errorf("failed to unmarshal flag: %w", err)
		}

		flags = append(flags, &flag)
	}

	return flags, nil
}

// Subscribe subscribes to flag updates
func (r *RedisFeatureFlagRepository) Subscribe(ctx context.Context, callback func(*model.FeatureFlag)) error {
	r.mutex.Lock()
	defer r.mutex.Unlock()

	// Add the callback
	r.callbacks = append(r.callbacks, callback)

	// Start the subscription if not already started
	if r.pubSubClient == nil {
		pubSub := r.client.Subscribe(ctx, "feature-flag-updates")
		r.pubSubClient = pubSub

		// Start a goroutine to handle messages
		go r.handleMessages(ctx)
	}

	return nil
}

// Unsubscribe unsubscribes from flag updates
func (r *RedisFeatureFlagRepository) Unsubscribe(ctx context.Context) error {
	r.mutex.Lock()
	defer r.mutex.Unlock()

	// Clear callbacks
	r.callbacks = make([]func(*model.FeatureFlag), 0)

	// Close the subscription if it exists
	if r.pubSubClient != nil {
		if err := r.pubSubClient.Close(); err != nil {
			return fmt.Errorf("failed to close Redis PubSub: %w", err)
		}
		r.pubSubClient = nil
	}

	return nil
}

// handleMessages handles messages from the Redis PubSub
func (r *RedisFeatureFlagRepository) handleMessages(ctx context.Context) {
	for {
		msg, err := r.pubSubClient.ReceiveMessage(ctx)
		if err != nil {
			// Check if the context is done
			if ctx.Err() != nil {
				return
			}

			// Log the error and continue
			fmt.Printf("Error receiving message from Redis PubSub: %v\n", err)
			continue
		}

		// Unmarshal the flag
		var flag model.FeatureFlag
		if err := json.Unmarshal([]byte(msg.Payload), &flag); err != nil {
			fmt.Printf("Failed to unmarshal flag from Redis PubSub: %v\n", err)
			continue
		}

		// Call the callbacks
		r.mutex.Lock()
		for _, callback := range r.callbacks {
			callback(&flag)
		}
		r.mutex.Unlock()
	}
}

// addToIndex adds a flag to the indexes
func (r *RedisFeatureFlagRepository) addToIndex(ctx context.Context, flag *model.FeatureFlag) error {
	// Add to namespace index
	namespaceKey := getNamespaceIndexKey(flag.Namespace)
	if err := r.client.SAdd(ctx, namespaceKey, flag.Key).Err(); err != nil {
		return fmt.Errorf("failed to add flag to namespace index: %w", err)
	}

	// Add to tag indexes
	for tagKey, tagValue := range flag.Tags {
		tagIndexKey := getTagIndexKey(flag.Namespace, tagKey, tagValue)
		flagKey := getFlagKey(flag.Key, flag.Namespace)
		if err := r.client.SAdd(ctx, tagIndexKey, flagKey).Err(); err != nil {
			return fmt.Errorf("failed to add flag to tag index: %w", err)
		}
	}

	return nil
}

// removeFromIndex removes a flag from the indexes
func (r *RedisFeatureFlagRepository) removeFromIndex(ctx context.Context, flag *model.FeatureFlag) error {
	// Remove from namespace index
	namespaceKey := getNamespaceIndexKey(flag.Namespace)
	if err := r.client.SRem(ctx, namespaceKey, flag.Key).Err(); err != nil {
		return fmt.Errorf("failed to remove flag from namespace index: %w", err)
	}

	// Remove from tag indexes
	for tagKey, tagValue := range flag.Tags {
		tagIndexKey := getTagIndexKey(flag.Namespace, tagKey, tagValue)
		flagKey := getFlagKey(flag.Key, flag.Namespace)
		if err := r.client.SRem(ctx, tagIndexKey, flagKey).Err(); err != nil {
			return fmt.Errorf("failed to remove flag from tag index: %w", err)
		}
	}

	return nil
}

// publishUpdate publishes a flag update
func (r *RedisFeatureFlagRepository) publishUpdate(ctx context.Context, flag *model.FeatureFlag) error {
	// Marshal the flag
	data, err := json.Marshal(flag)
	if err != nil {
		return fmt.Errorf("failed to marshal flag for publishing: %w", err)
	}

	// Publish the update
	if err := r.client.Publish(ctx, "feature-flag-updates", data).Err(); err != nil {
		return fmt.Errorf("failed to publish flag update: %w", err)
	}

	return nil
}

// Helper functions for Redis keys

// getFlagKey returns the Redis key for a flag
func getFlagKey(key, namespace string) string {
	return fmt.Sprintf("feature-flag:%s:%s", namespace, key)
}

// getNamespaceKey returns the Redis key prefix for a namespace
func getNamespaceKey(namespace string) string {
	return fmt.Sprintf("feature-flag:%s", namespace)
}

// getNamespaceIndexKey returns the Redis key for a namespace index
func getNamespaceIndexKey(namespace string) string {
	return fmt.Sprintf("feature-flag:index:namespace:%s", namespace)
}

// getTagIndexKey returns the Redis key for a tag index
func getTagIndexKey(namespace, tagKey, tagValue string) string {
	return fmt.Sprintf("feature-flag:index:tag:%s:%s:%s", namespace, tagKey, tagValue)
}

