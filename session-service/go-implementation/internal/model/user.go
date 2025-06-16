package model

import (
	"time"

	"github.com/google/uuid"
)

// User represents a user in the system
type User struct {
	ID             string    `json:"id"`
	Email          string    `json:"email"`
	Username       string    `json:"username"`
	HashedPassword string    `json:"-"` // Never expose password in JSON
	FirstName      string    `json:"first_name,omitempty"`
	LastName       string    `json:"last_name,omitempty"`
	IsActive       bool      `json:"is_active"`
	IsVerified     bool      `json:"is_verified"`
	LastLoginAt    time.Time `json:"last_login_at,omitempty"`
	CreatedAt      time.Time `json:"created_at"`
	UpdatedAt      time.Time `json:"updated_at"`
}

// NewUser creates a new user
func NewUser(email, username, hashedPassword string) *User {
	now := time.Now()
	return &User{
		ID:             uuid.New().String(),
		Email:          email,
		Username:       username,
		HashedPassword: hashedPassword,
		IsActive:       true,
		IsVerified:     false,
		CreatedAt:      now,
		UpdatedAt:      now,
	}
}

// UserProfile represents public user information
type UserProfile struct {
	ID         string    `json:"id"`
	Email      string    `json:"email"`
	Username   string    `json:"username"`
	FirstName  string    `json:"first_name,omitempty"`
	LastName   string    `json:"last_name,omitempty"`
	IsVerified bool      `json:"is_verified"`
	CreatedAt  time.Time `json:"created_at"`
}

// ToUserProfile converts a User to UserProfile
func (u *User) ToUserProfile() *UserProfile {
	return &UserProfile{
		ID:         u.ID,
		Email:      u.Email,
		Username:   u.Username,
		FirstName:  u.FirstName,
		LastName:   u.LastName,
		IsVerified: u.IsVerified,
		CreatedAt:  u.CreatedAt,
	}
}

// LoginRequest represents a login request
type LoginRequest struct {
	Email    string `json:"email" binding:"required,email"`
	Password string `json:"password" binding:"required,min=8"`
}

// RegisterRequest represents a registration request
type RegisterRequest struct {
	Email     string `json:"email" binding:"required,email"`
	Username  string `json:"username" binding:"required,min=3,max=30"`
	Password  string `json:"password" binding:"required,min=8"`
	FirstName string `json:"first_name"`
	LastName  string `json:"last_name"`
}

// TokenResponse represents a token response
type TokenResponse struct {
	AccessToken  string `json:"access_token"`
	RefreshToken string `json:"refresh_token"`
	ExpiresIn    int    `json:"expires_in"` // Seconds until expiration
	TokenType    string `json:"token_type"` // Usually "Bearer"
}

// RefreshTokenRequest represents a refresh token request
type RefreshTokenRequest struct {
	RefreshToken string `json:"refresh_token" binding:"required"`
}

