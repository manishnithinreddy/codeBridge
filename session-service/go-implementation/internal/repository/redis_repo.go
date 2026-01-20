package repository

import (
	"context"
	"encoding/json"
	"fmt"
	"time"

	"github.com/codebridge/session-service/internal/config"
	"github.com/codebridge/session-service/internal/model"
	"github.com/go-redis/redis/v8"
)

const (
	userPrefix       = "user:"
	userEmailPrefix  = "user:email:"
	userNamePrefix   = "user:username:"
	sessionPrefix    = "session:"
	userSessionsPrefix = "user:sessions:"
	refreshTokenPrefix = "refresh_token:"
)

// RedisRepository implements Repository interface using Redis
type RedisRepository struct {
	client *redis.Client
}

// NewRedisRepository creates a new Redis repository
func NewRedisRepository(cfg config.RedisConfig) (*RedisRepository, error) {
	client := redis.NewClient(&redis.Options{
		Addr:     fmt.Sprintf("%s:%d", cfg.Host, cfg.Port),
		Password: cfg.Password,
		DB:       cfg.DB,
		PoolSize: cfg.PoolSize,
	})

	// Test connection
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	
	if err := client.Ping(ctx).Err(); err != nil {
		return nil, fmt.Errorf("failed to connect to Redis: %w", err)
	}

	return &RedisRepository{
		client: client,
	}, nil
}

// CreateUser stores a user in Redis
func (r *RedisRepository) CreateUser(ctx context.Context, user *model.User) error {
	// Check if email already exists
	exists, err := r.client.Exists(ctx, userEmailPrefix+user.Email).Result()
	if err != nil {
		return fmt.Errorf("failed to check if email exists: %w", err)
	}
	if exists == 1 {
		return fmt.Errorf("email already exists")
	}

	// Check if username already exists
	exists, err = r.client.Exists(ctx, userNamePrefix+user.Username).Result()
	if err != nil {
		return fmt.Errorf("failed to check if username exists: %w", err)
	}
	if exists == 1 {
		return fmt.Errorf("username already exists")
	}

	// Serialize user
	userData, err := json.Marshal(user)
	if err != nil {
		return fmt.Errorf("failed to marshal user: %w", err)
	}

	// Use a transaction to ensure atomicity
	pipe := r.client.TxPipeline()
	
	// Store user data
	pipe.Set(ctx, userPrefix+user.ID, userData, 0)
	
	// Create indexes
	pipe.Set(ctx, userEmailPrefix+user.Email, user.ID, 0)
	pipe.Set(ctx, userNamePrefix+user.Username, user.ID, 0)

	// Execute transaction
	_, err = pipe.Exec(ctx)
	if err != nil {
		return fmt.Errorf("failed to create user: %w", err)
	}

	return nil
}

// GetUserByID retrieves a user by ID
func (r *RedisRepository) GetUserByID(ctx context.Context, id string) (*model.User, error) {
	userData, err := r.client.Get(ctx, userPrefix+id).Bytes()
	if err != nil {
		if err == redis.Nil {
			return nil, fmt.Errorf("user not found")
		}
		return nil, fmt.Errorf("failed to get user: %w", err)
	}

	var user model.User
	if err := json.Unmarshal(userData, &user); err != nil {
		return nil, fmt.Errorf("failed to unmarshal user: %w", err)
	}

	return &user, nil
}

// GetUserByEmail retrieves a user by email
func (r *RedisRepository) GetUserByEmail(ctx context.Context, email string) (*model.User, error) {
	// Get user ID by email
	userID, err := r.client.Get(ctx, userEmailPrefix+email).Result()
	if err != nil {
		if err == redis.Nil {
			return nil, fmt.Errorf("user not found")
		}
		return nil, fmt.Errorf("failed to get user ID by email: %w", err)
	}

	// Get user by ID
	return r.GetUserByID(ctx, userID)
}

// GetUserByUsername retrieves a user by username
func (r *RedisRepository) GetUserByUsername(ctx context.Context, username string) (*model.User, error) {
	// Get user ID by username
	userID, err := r.client.Get(ctx, userNamePrefix+username).Result()
	if err != nil {
		if err == redis.Nil {
			return nil, fmt.Errorf("user not found")
		}
		return nil, fmt.Errorf("failed to get user ID by username: %w", err)
	}

	// Get user by ID
	return r.GetUserByID(ctx, userID)
}

// UpdateUser updates a user in Redis
func (r *RedisRepository) UpdateUser(ctx context.Context, user *model.User) error {
	// Get existing user to check for email/username changes
	existingUser, err := r.GetUserByID(ctx, user.ID)
	if err != nil {
		return err
	}

	// Update timestamp
	user.UpdatedAt = time.Now()

	// Serialize user
	userData, err := json.Marshal(user)
	if err != nil {
		return fmt.Errorf("failed to marshal user: %w", err)
	}

	// Use a transaction to ensure atomicity
	pipe := r.client.TxPipeline()

	// Update email index if changed
	if existingUser.Email != user.Email {
		pipe.Del(ctx, userEmailPrefix+existingUser.Email)
		pipe.Set(ctx, userEmailPrefix+user.Email, user.ID, 0)
	}

	// Update username index if changed
	if existingUser.Username != user.Username {
		pipe.Del(ctx, userNamePrefix+existingUser.Username)
		pipe.Set(ctx, userNamePrefix+user.Username, user.ID, 0)
	}

	// Update user data
	pipe.Set(ctx, userPrefix+user.ID, userData, 0)

	// Execute transaction
	_, err = pipe.Exec(ctx)
	if err != nil {
		return fmt.Errorf("failed to update user: %w", err)
	}

	return nil
}

// DeleteUser deletes a user from Redis
func (r *RedisRepository) DeleteUser(ctx context.Context, id string) error {
	// Get existing user to delete indexes
	existingUser, err := r.GetUserByID(ctx, id)
	if err != nil {
		return err
	}

	// Use a transaction to ensure atomicity
	pipe := r.client.TxPipeline()

	// Delete user data
	pipe.Del(ctx, userPrefix+id)

	// Delete indexes
	pipe.Del(ctx, userEmailPrefix+existingUser.Email)
	pipe.Del(ctx, userNamePrefix+existingUser.Username)

	// Delete all user sessions
	sessionKeys, err := r.client.SMembers(ctx, userSessionsPrefix+id).Result()
	if err != nil && err != redis.Nil {
		return fmt.Errorf("failed to get user sessions: %w", err)
	}

	for _, sessionKey := range sessionKeys {
		pipe.Del(ctx, sessionPrefix+sessionKey)
	}
	pipe.Del(ctx, userSessionsPrefix+id)

	// Execute transaction
	_, err = pipe.Exec(ctx)
	if err != nil {
		return fmt.Errorf("failed to delete user: %w", err)
	}

	return nil
}

// CreateSession stores a session in Redis
func (r *RedisRepository) CreateSession(ctx context.Context, session *model.Session) error {
	// Serialize session
	sessionData, err := json.Marshal(session)
	if err != nil {
		return fmt.Errorf("failed to marshal session: %w", err)
	}

	// Calculate TTL based on expiration time
	ttl := time.Until(session.ExpiresAt)
	if ttl <= 0 {
		return fmt.Errorf("session already expired")
	}

	// Use a transaction to ensure atomicity
	pipe := r.client.TxPipeline()

	// Store session data with TTL
	pipe.Set(ctx, sessionPrefix+session.ID, sessionData, ttl)

	// Add session to user's sessions set
	pipe.SAdd(ctx, userSessionsPrefix+session.UserID, session.ID)
	pipe.ExpireAt(ctx, userSessionsPrefix+session.UserID, session.ExpiresAt)

	// Execute transaction
	_, err = pipe.Exec(ctx)
	if err != nil {
		return fmt.Errorf("failed to create session: %w", err)
	}

	return nil
}

// GetSessionByID retrieves a session by ID
func (r *RedisRepository) GetSessionByID(ctx context.Context, id string) (*model.Session, error) {
	sessionData, err := r.client.Get(ctx, sessionPrefix+id).Bytes()
	if err != nil {
		if err == redis.Nil {
			return nil, fmt.Errorf("session not found")
		}
		return nil, fmt.Errorf("failed to get session: %w", err)
	}

	var session model.Session
	if err := json.Unmarshal(sessionData, &session); err != nil {
		return nil, fmt.Errorf("failed to unmarshal session: %w", err)
	}

	return &session, nil
}

// GetSessionsByUserID retrieves all sessions for a user
func (r *RedisRepository) GetSessionsByUserID(ctx context.Context, userID string) ([]*model.Session, error) {
	// Get session IDs for user
	sessionIDs, err := r.client.SMembers(ctx, userSessionsPrefix+userID).Result()
	if err != nil {
		return nil, fmt.Errorf("failed to get user sessions: %w", err)
	}

	if len(sessionIDs) == 0 {
		return []*model.Session{}, nil
	}

	// Get session data for each ID
	sessions := make([]*model.Session, 0, len(sessionIDs))
	for _, id := range sessionIDs {
		session, err := r.GetSessionByID(ctx, id)
		if err != nil {
			// Skip sessions that can't be retrieved (might be expired)
			continue
		}
		sessions = append(sessions, session)
	}

	return sessions, nil
}

// UpdateSession updates a session in Redis
func (r *RedisRepository) UpdateSession(ctx context.Context, session *model.Session) error {
	// Update timestamp
	session.UpdatedAt = time.Now()

	// Serialize session
	sessionData, err := json.Marshal(session)
	if err != nil {
		return fmt.Errorf("failed to marshal session: %w", err)
	}

	// Calculate TTL based on expiration time
	ttl := time.Until(session.ExpiresAt)
	if ttl <= 0 {
		return fmt.Errorf("session already expired")
	}

	// Update session data with TTL
	if err := r.client.Set(ctx, sessionPrefix+session.ID, sessionData, ttl).Err(); err != nil {
		return fmt.Errorf("failed to update session: %w", err)
	}

	return nil
}

// DeleteSession deletes a session from Redis
func (r *RedisRepository) DeleteSession(ctx context.Context, id string) error {
	// Get session to get user ID
	session, err := r.GetSessionByID(ctx, id)
	if err != nil {
		return err
	}

	// Use a transaction to ensure atomicity
	pipe := r.client.TxPipeline()

	// Delete session data
	pipe.Del(ctx, sessionPrefix+id)

	// Remove session from user's sessions set
	pipe.SRem(ctx, userSessionsPrefix+session.UserID, id)

	// Execute transaction
	_, err = pipe.Exec(ctx)
	if err != nil {
		return fmt.Errorf("failed to delete session: %w", err)
	}

	return nil
}

// DeleteSessionsByUserID deletes all sessions for a user
func (r *RedisRepository) DeleteSessionsByUserID(ctx context.Context, userID string) error {
	// Get session IDs for user
	sessionIDs, err := r.client.SMembers(ctx, userSessionsPrefix+userID).Result()
	if err != nil {
		return fmt.Errorf("failed to get user sessions: %w", err)
	}

	if len(sessionIDs) == 0 {
		return nil
	}

	// Use a transaction to ensure atomicity
	pipe := r.client.TxPipeline()

	// Delete each session
	for _, id := range sessionIDs {
		pipe.Del(ctx, sessionPrefix+id)
	}

	// Delete user's sessions set
	pipe.Del(ctx, userSessionsPrefix+userID)

	// Execute transaction
	_, err = pipe.Exec(ctx)
	if err != nil {
		return fmt.Errorf("failed to delete user sessions: %w", err)
	}

	return nil
}

// StoreRefreshToken stores a refresh token with association to a user
func (r *RedisRepository) StoreRefreshToken(ctx context.Context, userID, refreshToken string, expiresAt int64) error {
	// Calculate TTL based on expiration time
	ttl := time.Until(time.Unix(expiresAt, 0))
	if ttl <= 0 {
		return fmt.Errorf("token already expired")
	}

	// Store refresh token with TTL
	if err := r.client.Set(ctx, refreshTokenPrefix+refreshToken, userID, ttl).Err(); err != nil {
		return fmt.Errorf("failed to store refresh token: %w", err)
	}

	return nil
}

// GetUserIDByRefreshToken gets the user ID associated with a refresh token
func (r *RedisRepository) GetUserIDByRefreshToken(ctx context.Context, refreshToken string) (string, error) {
	userID, err := r.client.Get(ctx, refreshTokenPrefix+refreshToken).Result()
	if err != nil {
		if err == redis.Nil {
			return "", fmt.Errorf("refresh token not found")
		}
		return "", fmt.Errorf("failed to get user ID by refresh token: %w", err)
	}

	return userID, nil
}

// InvalidateRefreshToken invalidates a refresh token
func (r *RedisRepository) InvalidateRefreshToken(ctx context.Context, refreshToken string) error {
	if err := r.client.Del(ctx, refreshTokenPrefix+refreshToken).Err(); err != nil {
		return fmt.Errorf("failed to invalidate refresh token: %w", err)
	}

	return nil
}

// InvalidateAllRefreshTokens invalidates all refresh tokens for a user
func (r *RedisRepository) InvalidateAllRefreshTokens(ctx context.Context, userID string) error {
	// This would require scanning all refresh tokens and checking if they belong to the user
	// For simplicity, we'll just rely on token expiration
	// In a production system, you might want to maintain a separate index of tokens by user
	return nil
}

// Ping checks the connection to Redis
func (r *RedisRepository) Ping(ctx context.Context) error {
	return r.client.Ping(ctx).Err()
}

// Close closes the Redis connection
func (r *RedisRepository) Close() error {
	return r.client.Close()
}

