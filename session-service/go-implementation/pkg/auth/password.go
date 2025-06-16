package auth

import (
	"fmt"

	"golang.org/x/crypto/bcrypt"
)

// PasswordManager handles password hashing and verification
type PasswordManager struct {
	cost int
}

// NewPasswordManager creates a new password manager
func NewPasswordManager(cost int) *PasswordManager {
	// If cost is too low, use default
	if cost < bcrypt.MinCost {
		cost = bcrypt.DefaultCost
	}

	return &PasswordManager{
		cost: cost,
	}
}

// HashPassword hashes a password using bcrypt
func (m *PasswordManager) HashPassword(password string) (string, error) {
	hashedBytes, err := bcrypt.GenerateFromPassword([]byte(password), m.cost)
	if err != nil {
		return "", fmt.Errorf("failed to hash password: %w", err)
	}
	return string(hashedBytes), nil
}

// VerifyPassword verifies a password against a hash
func (m *PasswordManager) VerifyPassword(hashedPassword, password string) error {
	return bcrypt.CompareHashAndPassword([]byte(hashedPassword), []byte(password))
}

