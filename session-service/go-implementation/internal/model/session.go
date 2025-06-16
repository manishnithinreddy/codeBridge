package model

import (
	"time"

	"github.com/google/uuid"
)

// Session represents a user session
type Session struct {
	ID           string    `json:"id"`
	UserID       string    `json:"user_id"`
	RefreshToken string    `json:"refresh_token,omitempty"`
	UserAgent    string    `json:"user_agent,omitempty"`
	ClientIP     string    `json:"client_ip,omitempty"`
	IsBlocked    bool      `json:"is_blocked"`
	ExpiresAt    time.Time `json:"expires_at"`
	CreatedAt    time.Time `json:"created_at"`
	UpdatedAt    time.Time `json:"updated_at"`
}

// NewSession creates a new session for a user
func NewSession(userID string, refreshToken, userAgent, clientIP string, expiresAt time.Time) *Session {
	return &Session{
		ID:           uuid.New().String(),
		UserID:       userID,
		RefreshToken: refreshToken,
		UserAgent:    userAgent,
		ClientIP:     clientIP,
		IsBlocked:    false,
		ExpiresAt:    expiresAt,
		CreatedAt:    time.Now(),
		UpdatedAt:    time.Now(),
	}
}

// SessionInfo represents public session information
type SessionInfo struct {
	ID        string    `json:"id"`
	UserID    string    `json:"user_id"`
	UserAgent string    `json:"user_agent,omitempty"`
	ClientIP  string    `json:"client_ip,omitempty"`
	ExpiresAt time.Time `json:"expires_at"`
	CreatedAt time.Time `json:"created_at"`
}

// ToSessionInfo converts a Session to SessionInfo
func (s *Session) ToSessionInfo() *SessionInfo {
	return &SessionInfo{
		ID:        s.ID,
		UserID:    s.UserID,
		UserAgent: s.UserAgent,
		ClientIP:  s.ClientIP,
		ExpiresAt: s.ExpiresAt,
		CreatedAt: s.CreatedAt,
	}
}

