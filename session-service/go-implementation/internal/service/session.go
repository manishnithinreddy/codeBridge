package service

import (
	"context"
	"fmt"
	"time"

	"github.com/codebridge/session-service/internal/config"
	"github.com/codebridge/session-service/internal/model"
	"github.com/codebridge/session-service/internal/repository"
	"github.com/codebridge/session-service/pkg/auth"
	"github.com/codebridge/session-service/pkg/logging"
)

// SessionService handles session-related business logic
type SessionService struct {
	repo           repository.Repository
	jwtManager     *auth.JWTManager
	passwordManager *auth.PasswordManager
	logger         *logging.Logger
	jwtConfig      config.JWTConfig
}

// NewSessionService creates a new session service
func NewSessionService(repo repository.Repository, jwtConfig config.JWTConfig, logger *logging.Logger) *SessionService {
	return &SessionService{
		repo:           repo,
		jwtManager:     auth.NewJWTManager(jwtConfig),
		passwordManager: auth.NewPasswordManager(10), // bcrypt cost
		logger:         logger,
		jwtConfig:      jwtConfig,
	}
}

// RegisterUser registers a new user
func (s *SessionService) RegisterUser(ctx context.Context, req *model.RegisterRequest) (*model.UserProfile, error) {
	// Hash password
	hashedPassword, err := s.passwordManager.HashPassword(req.Password)
	if err != nil {
		return nil, fmt.Errorf("failed to hash password: %w", err)
	}

	// Create user
	user := model.NewUser(req.Email, req.Username, hashedPassword)
	user.FirstName = req.FirstName
	user.LastName = req.LastName

	// Store user
	if err := s.repo.CreateUser(ctx, user); err != nil {
		return nil, fmt.Errorf("failed to create user: %w", err)
	}

	return user.ToUserProfile(), nil
}

// Login authenticates a user and creates a session
func (s *SessionService) Login(ctx context.Context, req *model.LoginRequest, userAgent, clientIP string) (*model.TokenResponse, error) {
	// Get user by email
	user, err := s.repo.GetUserByEmail(ctx, req.Email)
	if err != nil {
		return nil, fmt.Errorf("invalid email or password")
	}

	// Verify password
	if err := s.passwordManager.VerifyPassword(user.HashedPassword, req.Password); err != nil {
		return nil, fmt.Errorf("invalid email or password")
	}

	// Check if user is active
	if !user.IsActive {
		return nil, fmt.Errorf("account is disabled")
	}

	// Generate refresh token
	refreshToken, refreshExpiry, err := s.jwtManager.GenerateRefreshToken(user.ID)
	if err != nil {
		return nil, fmt.Errorf("failed to generate refresh token: %w", err)
	}

	// Create session
	session := model.NewSession(user.ID, refreshToken, userAgent, clientIP, refreshExpiry)
	if err := s.repo.CreateSession(ctx, session); err != nil {
		return nil, fmt.Errorf("failed to create session: %w", err)
	}

	// Store refresh token
	if err := s.repo.StoreRefreshToken(ctx, user.ID, refreshToken, refreshExpiry.Unix()); err != nil {
		return nil, fmt.Errorf("failed to store refresh token: %w", err)
	}

	// Generate access token
	accessToken, accessExpiry, err := s.jwtManager.GenerateAccessToken(user, session.ID)
	if err != nil {
		return nil, fmt.Errorf("failed to generate access token: %w", err)
	}

	// Update last login time
	user.LastLoginAt = time.Now()
	if err := s.repo.UpdateUser(ctx, user); err != nil {
		s.logger.Warn("Failed to update last login time", "error", err)
		// Continue anyway, this is not critical
	}

	// Return tokens
	return &model.TokenResponse{
		AccessToken:  accessToken,
		RefreshToken: refreshToken,
		ExpiresIn:    int(time.Until(accessExpiry).Seconds()),
		TokenType:    "Bearer",
	}, nil
}

// RefreshToken refreshes an access token using a refresh token
func (s *SessionService) RefreshToken(ctx context.Context, req *model.RefreshTokenRequest) (*model.TokenResponse, error) {
	// Validate refresh token
	userID, err := s.jwtManager.ValidateRefreshToken(req.RefreshToken)
	if err != nil {
		return nil, fmt.Errorf("invalid refresh token: %w", err)
	}

	// Check if refresh token exists in repository
	storedUserID, err := s.repo.GetUserIDByRefreshToken(ctx, req.RefreshToken)
	if err != nil {
		return nil, fmt.Errorf("invalid refresh token: %w", err)
	}

	// Verify user ID matches
	if userID != storedUserID {
		return nil, fmt.Errorf("invalid refresh token")
	}

	// Get user
	user, err := s.repo.GetUserByID(ctx, userID)
	if err != nil {
		return nil, fmt.Errorf("user not found: %w", err)
	}

	// Check if user is active
	if !user.IsActive {
		return nil, fmt.Errorf("account is disabled")
	}

	// Generate new refresh token
	newRefreshToken, refreshExpiry, err := s.jwtManager.GenerateRefreshToken(user.ID)
	if err != nil {
		return nil, fmt.Errorf("failed to generate refresh token: %w", err)
	}

	// Invalidate old refresh token
	if err := s.repo.InvalidateRefreshToken(ctx, req.RefreshToken); err != nil {
		s.logger.Warn("Failed to invalidate old refresh token", "error", err)
		// Continue anyway, this is not critical
	}

	// Store new refresh token
	if err := s.repo.StoreRefreshToken(ctx, user.ID, newRefreshToken, refreshExpiry.Unix()); err != nil {
		return nil, fmt.Errorf("failed to store refresh token: %w", err)
	}

	// Get session ID (use a new one if not found)
	sessionID := "refresh-" + time.Now().Format(time.RFC3339Nano)

	// Generate new access token
	accessToken, accessExpiry, err := s.jwtManager.GenerateAccessToken(user, sessionID)
	if err != nil {
		return nil, fmt.Errorf("failed to generate access token: %w", err)
	}

	// Return tokens
	return &model.TokenResponse{
		AccessToken:  accessToken,
		RefreshToken: newRefreshToken,
		ExpiresIn:    int(time.Until(accessExpiry).Seconds()),
		TokenType:    "Bearer",
	}, nil
}

// Logout invalidates a session
func (s *SessionService) Logout(ctx context.Context, refreshToken string) error {
	// Invalidate refresh token
	return s.repo.InvalidateRefreshToken(ctx, refreshToken)
}

// LogoutAll invalidates all sessions for a user
func (s *SessionService) LogoutAll(ctx context.Context, userID string) error {
	// Delete all sessions
	if err := s.repo.DeleteSessionsByUserID(ctx, userID); err != nil {
		return fmt.Errorf("failed to delete sessions: %w", err)
	}

	// Invalidate all refresh tokens
	if err := s.repo.InvalidateAllRefreshTokens(ctx, userID); err != nil {
		return fmt.Errorf("failed to invalidate refresh tokens: %w", err)
	}

	return nil
}

// GetUserProfile gets a user's profile
func (s *SessionService) GetUserProfile(ctx context.Context, userID string) (*model.UserProfile, error) {
	// Get user
	user, err := s.repo.GetUserByID(ctx, userID)
	if err != nil {
		return nil, fmt.Errorf("user not found: %w", err)
	}

	return user.ToUserProfile(), nil
}

// GetUserSessions gets all sessions for a user
func (s *SessionService) GetUserSessions(ctx context.Context, userID string) ([]*model.SessionInfo, error) {
	// Get sessions
	sessions, err := s.repo.GetSessionsByUserID(ctx, userID)
	if err != nil {
		return nil, fmt.Errorf("failed to get sessions: %w", err)
	}

	// Convert to session info
	sessionInfos := make([]*model.SessionInfo, len(sessions))
	for i, session := range sessions {
		sessionInfos[i] = session.ToSessionInfo()
	}

	return sessionInfos, nil
}

// ValidateToken validates an access token
func (s *SessionService) ValidateToken(ctx context.Context, token string) (*auth.Claims, error) {
	// Validate token
	claims, err := s.jwtManager.ValidateAccessToken(token)
	if err != nil {
		return nil, fmt.Errorf("invalid token: %w", err)
	}

	return claims, nil
}

// HealthCheck checks the health of the service
func (s *SessionService) HealthCheck(ctx context.Context) error {
	// Check repository connection
	return s.repo.Ping(ctx)
}

