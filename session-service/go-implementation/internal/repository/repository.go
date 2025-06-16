package repository

import (
	"context"

	"github.com/codebridge/session-service/internal/model"
)

// Repository defines the interface for session and user data storage
type Repository interface {
	// User operations
	CreateUser(ctx context.Context, user *model.User) error
	GetUserByID(ctx context.Context, id string) (*model.User, error)
	GetUserByEmail(ctx context.Context, email string) (*model.User, error)
	GetUserByUsername(ctx context.Context, username string) (*model.User, error)
	UpdateUser(ctx context.Context, user *model.User) error
	DeleteUser(ctx context.Context, id string) error

	// Session operations
	CreateSession(ctx context.Context, session *model.Session) error
	GetSessionByID(ctx context.Context, id string) (*model.Session, error)
	GetSessionsByUserID(ctx context.Context, userID string) ([]*model.Session, error)
	UpdateSession(ctx context.Context, session *model.Session) error
	DeleteSession(ctx context.Context, id string) error
	DeleteSessionsByUserID(ctx context.Context, userID string) error
	
	// Token operations
	StoreRefreshToken(ctx context.Context, userID, refreshToken string, expiresAt int64) error
	GetUserIDByRefreshToken(ctx context.Context, refreshToken string) (string, error)
	InvalidateRefreshToken(ctx context.Context, refreshToken string) error
	InvalidateAllRefreshTokens(ctx context.Context, userID string) error

	// Health check
	Ping(ctx context.Context) error
	
	// Close connection
	Close() error
}

