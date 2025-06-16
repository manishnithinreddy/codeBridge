package pool

import (
	"context"
	"database/sql"
	"fmt"
	"sync"
	"time"

	"github.com/codebridge/db-service/pkg/logging"
)

// ConnectionPool manages a pool of database connections
type ConnectionPool struct {
	sessions     map[string]*sql.DB
	sessionInfo  map[string]SessionInfo
	maxLifetime  time.Duration
	maxOpenConns int
	maxIdleConns int
	mu           sync.RWMutex
	logger       *logging.Logger
}

// SessionInfo holds metadata about a database session
type SessionInfo struct {
	UserID    string
	Type      string
	CreatedAt time.Time
	LastUsed  time.Time
	ExpiresAt time.Time
}

// NewConnectionPool creates a new connection pool
func NewConnectionPool(maxLifetime time.Duration, maxOpenConns, maxIdleConns int, logger *logging.Logger) *ConnectionPool {
	pool := &ConnectionPool{
		sessions:     make(map[string]*sql.DB),
		sessionInfo:  make(map[string]SessionInfo),
		maxLifetime:  maxLifetime,
		maxOpenConns: maxOpenConns,
		maxIdleConns: maxIdleConns,
		logger:       logger,
	}

	// Start a goroutine to clean up expired sessions
	go pool.cleanupExpiredSessions()

	return pool
}

// AddConnection adds a database connection to the pool
func (p *ConnectionPool) AddConnection(sessionID, userID, dbType string, db *sql.DB, expiry time.Time) {
	p.mu.Lock()
	defer p.mu.Unlock()

	// Configure connection pool settings
	db.SetConnMaxLifetime(p.maxLifetime)
	db.SetMaxOpenConns(p.maxOpenConns)
	db.SetMaxIdleConns(p.maxIdleConns)

	// Store the connection and its metadata
	p.sessions[sessionID] = db
	p.sessionInfo[sessionID] = SessionInfo{
		UserID:    userID,
		Type:      dbType,
		CreatedAt: time.Now(),
		LastUsed:  time.Now(),
		ExpiresAt: expiry,
	}

	p.logger.Info("Added database connection to pool", 
		"session_id", sessionID, 
		"user_id", userID, 
		"db_type", dbType,
		"expires_at", expiry)
}

// GetConnection retrieves a database connection from the pool
func (p *ConnectionPool) GetConnection(sessionID string) (*sql.DB, error) {
	p.mu.RLock()
	defer p.mu.RUnlock()

	db, exists := p.sessions[sessionID]
	if !exists {
		return nil, fmt.Errorf("session not found: %s", sessionID)
	}

	// Update last used time
	info := p.sessionInfo[sessionID]
	info.LastUsed = time.Now()
	p.sessionInfo[sessionID] = info

	return db, nil
}

// RemoveConnection removes a database connection from the pool
func (p *ConnectionPool) RemoveConnection(sessionID string) error {
	p.mu.Lock()
	defer p.mu.Unlock()

	db, exists := p.sessions[sessionID]
	if !exists {
		return fmt.Errorf("session not found: %s", sessionID)
	}

	// Close the database connection
	if err := db.Close(); err != nil {
		return fmt.Errorf("failed to close database connection: %w", err)
	}

	// Remove from maps
	delete(p.sessions, sessionID)
	delete(p.sessionInfo, sessionID)

	p.logger.Info("Removed database connection from pool", "session_id", sessionID)

	return nil
}

// GetSessionInfo retrieves session information
func (p *ConnectionPool) GetSessionInfo(sessionID string) (SessionInfo, bool) {
	p.mu.RLock()
	defer p.mu.RUnlock()

	info, exists := p.sessionInfo[sessionID]
	return info, exists
}

// GetAllSessionsForUser retrieves all sessions for a user
func (p *ConnectionPool) GetAllSessionsForUser(userID string) []string {
	p.mu.RLock()
	defer p.mu.RUnlock()

	var sessions []string
	for sessionID, info := range p.sessionInfo {
		if info.UserID == userID {
			sessions = append(sessions, sessionID)
		}
	}

	return sessions
}

// GetActiveConnectionCount returns the number of active connections
func (p *ConnectionPool) GetActiveConnectionCount() int {
	p.mu.RLock()
	defer p.mu.RUnlock()

	return len(p.sessions)
}

// cleanupExpiredSessions periodically removes expired sessions
func (p *ConnectionPool) cleanupExpiredSessions() {
	ticker := time.NewTicker(5 * time.Minute)
	defer ticker.Stop()

	for range ticker.C {
		p.mu.Lock()
		now := time.Now()
		
		for sessionID, info := range p.sessionInfo {
			if now.After(info.ExpiresAt) {
				// Session has expired
				db := p.sessions[sessionID]
				if db != nil {
					if err := db.Close(); err != nil {
						p.logger.Error("Failed to close expired database connection", 
							"session_id", sessionID, 
							"error", err)
					}
				}
				
				delete(p.sessions, sessionID)
				delete(p.sessionInfo, sessionID)
				
				p.logger.Info("Removed expired database connection", "session_id", sessionID)
			}
		}
		
		p.mu.Unlock()
	}
}

// PingAll pings all connections to check if they're still alive
func (p *ConnectionPool) PingAll(ctx context.Context) map[string]error {
	p.mu.RLock()
	defer p.mu.RUnlock()

	results := make(map[string]error)
	for sessionID, db := range p.sessions {
		results[sessionID] = db.PingContext(ctx)
	}

	return results
}

// Close closes all connections in the pool
func (p *ConnectionPool) Close() {
	p.mu.Lock()
	defer p.mu.Unlock()

	for sessionID, db := range p.sessions {
		if err := db.Close(); err != nil {
			p.logger.Error("Failed to close database connection", 
				"session_id", sessionID, 
				"error", err)
		}
	}

	p.sessions = make(map[string]*sql.DB)
	p.sessionInfo = make(map[string]SessionInfo)

	p.logger.Info("Closed all database connections")
}

