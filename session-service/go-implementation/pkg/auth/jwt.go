package auth

import (
	"fmt"
	"time"

	"github.com/codebridge/session-service/internal/config"
	"github.com/codebridge/session-service/internal/model"
	"github.com/golang-jwt/jwt/v5"
	"github.com/google/uuid"
)

// JWTManager handles JWT token generation and validation
type JWTManager struct {
	config config.JWTConfig
}

// NewJWTManager creates a new JWT manager
func NewJWTManager(config config.JWTConfig) *JWTManager {
	return &JWTManager{
		config: config,
	}
}

// Claims represents the JWT claims
type Claims struct {
	UserID    string `json:"user_id"`
	Username  string `json:"username"`
	Email     string `json:"email"`
	SessionID string `json:"session_id"`
	jwt.RegisteredClaims
}

// GenerateAccessToken generates a new access token for a user
func (m *JWTManager) GenerateAccessToken(user *model.User, sessionID string) (string, time.Time, error) {
	expirationTime := time.Now().Add(time.Duration(m.config.ExpirationHours) * time.Hour)
	
	claims := &Claims{
		UserID:    user.ID,
		Username:  user.Username,
		Email:     user.Email,
		SessionID: sessionID,
		RegisteredClaims: jwt.RegisteredClaims{
			ExpiresAt: jwt.NewNumericDate(expirationTime),
			IssuedAt:  jwt.NewNumericDate(time.Now()),
			NotBefore: jwt.NewNumericDate(time.Now()),
			Issuer:    m.config.Issuer,
			Subject:   user.ID,
			ID:        uuid.New().String(),
		},
	}

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	tokenString, err := token.SignedString([]byte(m.config.Secret))
	if err != nil {
		return "", time.Time{}, fmt.Errorf("failed to sign token: %w", err)
	}

	return tokenString, expirationTime, nil
}

// GenerateRefreshToken generates a new refresh token for a user
func (m *JWTManager) GenerateRefreshToken(userID string) (string, time.Time, error) {
	expirationTime := time.Now().Add(time.Duration(m.config.RefreshExpHours) * time.Hour)
	
	claims := jwt.RegisteredClaims{
		ExpiresAt: jwt.NewNumericDate(expirationTime),
		IssuedAt:  jwt.NewNumericDate(time.Now()),
		NotBefore: jwt.NewNumericDate(time.Now()),
		Issuer:    m.config.Issuer,
		Subject:   userID,
		ID:        uuid.New().String(),
	}

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	tokenString, err := token.SignedString([]byte(m.config.RefreshSecret))
	if err != nil {
		return "", time.Time{}, fmt.Errorf("failed to sign refresh token: %w", err)
	}

	return tokenString, expirationTime, nil
}

// ValidateAccessToken validates an access token and returns the claims
func (m *JWTManager) ValidateAccessToken(tokenString string) (*Claims, error) {
	token, err := jwt.ParseWithClaims(tokenString, &Claims{}, func(token *jwt.Token) (interface{}, error) {
		if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, fmt.Errorf("unexpected signing method: %v", token.Header["alg"])
		}
		return []byte(m.config.Secret), nil
	})

	if err != nil {
		return nil, fmt.Errorf("invalid token: %w", err)
	}

	if claims, ok := token.Claims.(*Claims); ok && token.Valid {
		return claims, nil
	}

	return nil, fmt.Errorf("invalid token claims")
}

// ValidateRefreshToken validates a refresh token and returns the user ID
func (m *JWTManager) ValidateRefreshToken(tokenString string) (string, error) {
	token, err := jwt.ParseWithClaims(tokenString, &jwt.RegisteredClaims{}, func(token *jwt.Token) (interface{}, error) {
		if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, fmt.Errorf("unexpected signing method: %v", token.Header["alg"])
		}
		return []byte(m.config.RefreshSecret), nil
	})

	if err != nil {
		return "", fmt.Errorf("invalid refresh token: %w", err)
	}

	if claims, ok := token.Claims.(*jwt.RegisteredClaims); ok && token.Valid {
		return claims.Subject, nil
	}

	return "", fmt.Errorf("invalid refresh token claims")
}

