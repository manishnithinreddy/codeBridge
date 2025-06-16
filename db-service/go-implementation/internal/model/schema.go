package model

import "time"

// DBSession represents a database session
type DBSession struct {
	ID           string    `json:"id"`
	UserID       string    `json:"user_id"`
	Type         string    `json:"type"`
	Host         string    `json:"host,omitempty"`
	Port         int       `json:"port,omitempty"`
	User         string    `json:"user,omitempty"`
	Database     string    `json:"database,omitempty"`
	FilePath     string    `json:"file_path,omitempty"`
	Connected    bool      `json:"connected"`
	CreatedAt    time.Time `json:"created_at"`
	LastAccessAt time.Time `json:"last_access_at"`
	ExpiresAt    time.Time `json:"expires_at"`
}

// DBSessionInfo represents public database session information
type DBSessionInfo struct {
	ID        string    `json:"id"`
	Type      string    `json:"type"`
	Host      string    `json:"host,omitempty"`
	Port      int       `json:"port,omitempty"`
	User      string    `json:"user,omitempty"`
	Database  string    `json:"database,omitempty"`
	FilePath  string    `json:"file_path,omitempty"`
	Connected bool      `json:"connected"`
	CreatedAt time.Time `json:"created_at"`
	ExpiresAt time.Time `json:"expires_at"`
}

// ToSessionInfo converts a DBSession to DBSessionInfo
func (s *DBSession) ToSessionInfo() *DBSessionInfo {
	return &DBSessionInfo{
		ID:        s.ID,
		Type:      s.Type,
		Host:      s.Host,
		Port:      s.Port,
		User:      s.User,
		Database:  s.Database,
		FilePath:  s.FilePath,
		Connected: s.Connected,
		CreatedAt: s.CreatedAt,
		ExpiresAt: s.ExpiresAt,
	}
}

// TokenValidationRequest represents a token validation request
type TokenValidationRequest struct {
	Token string `json:"token" binding:"required"`
}

// TokenValidationResponse represents a token validation response
type TokenValidationResponse struct {
	Valid     bool   `json:"valid"`
	UserID    string `json:"user_id,omitempty"`
	Username  string `json:"username,omitempty"`
	Email     string `json:"email,omitempty"`
	SessionID string `json:"session_id,omitempty"`
	ExpiresAt int64  `json:"expires_at,omitempty"`
	Error     string `json:"error,omitempty"`
}

// UserClaims represents user claims from a JWT token
type UserClaims struct {
	UserID    string `json:"user_id"`
	Username  string `json:"username"`
	Email     string `json:"email"`
	SessionID string `json:"session_id"`
	ExpiresAt int64  `json:"expires_at"`
}

