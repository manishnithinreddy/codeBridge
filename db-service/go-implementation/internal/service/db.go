package service

import (
	"bytes"
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	"github.com/codebridge/db-service/internal/config"
	"github.com/codebridge/db-service/internal/database"
	"github.com/codebridge/db-service/internal/model"
	"github.com/codebridge/db-service/pkg/logging"
)

// DBService handles database service business logic
type DBService struct {
	dbManager      *database.Manager
	sessionConfig  config.SessionServiceConfig
	logger         *logging.Logger
}

// NewDBService creates a new database service
func NewDBService(dbManager *database.Manager, sessionConfig config.SessionServiceConfig, logger *logging.Logger) *DBService {
	return &DBService{
		dbManager:      dbManager,
		sessionConfig:  sessionConfig,
		logger:         logger,
	}
}

// Connect creates a new database connection
func (s *DBService) Connect(token string, req *model.ConnectionRequest) (*model.ConnectionResponse, error) {
	// Validate token
	claims, err := s.validateToken(token)
	if err != nil {
		return nil, fmt.Errorf("invalid token: %w", err)
	}

	// Create connection
	return s.dbManager.Connect(claims.UserID, req)
}

// Disconnect closes a database connection
func (s *DBService) Disconnect(token string, req *model.DisconnectRequest) (*model.DisconnectResponse, error) {
	// Validate token
	_, err := s.validateToken(token)
	if err != nil {
		return nil, fmt.Errorf("invalid token: %w", err)
	}

	// Close connection
	return s.dbManager.Disconnect(req.SessionID)
}

// ExecuteQuery executes a SQL query
func (s *DBService) ExecuteQuery(token string, req *model.QueryRequest) (*model.QueryResult, error) {
	// Validate token
	_, err := s.validateToken(token)
	if err != nil {
		return nil, fmt.Errorf("invalid token: %w", err)
	}

	// Execute query
	return s.dbManager.ExecuteQuery(req.SessionID, req)
}

// ExecuteBatchQueries executes multiple SQL queries
func (s *DBService) ExecuteBatchQueries(token string, req *model.BatchQueryRequest) (*model.BatchQueryResult, error) {
	// Validate token
	_, err := s.validateToken(token)
	if err != nil {
		return nil, fmt.Errorf("invalid token: %w", err)
	}

	// Execute batch queries
	return s.dbManager.ExecuteBatchQueries(req.SessionID, req)
}

// GetSchema retrieves database schema information
func (s *DBService) GetSchema(token string, req *model.SchemaRequest) (*model.SchemaResult, error) {
	// Validate token
	_, err := s.validateToken(token)
	if err != nil {
		return nil, fmt.Errorf("invalid token: %w", err)
	}

	// Get schema
	return s.dbManager.GetSchema(req.SessionID, req)
}

// HealthCheck checks the health of the service
func (s *DBService) HealthCheck() *model.HealthResponse {
	return &model.HealthResponse{
		Status:           "UP",
		Version:          "1.0.0",
		ActiveConnections: s.dbManager.GetActiveConnectionCount(),
	}
}

// validateToken validates a JWT token with the session service
func (s *DBService) validateToken(token string) (*model.UserClaims, error) {
	// Create validation request
	req := model.TokenValidationRequest{
		Token: token,
	}

	// Marshal request to JSON
	reqBody, err := json.Marshal(req)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal token validation request: %w", err)
	}

	// Create HTTP client with timeout
	client := &http.Client{
		Timeout: 5 * time.Second,
	}

	// Send request to session service
	resp, err := client.Post(
		fmt.Sprintf("%s/validate-token", s.sessionConfig.URL),
		"application/json",
		bytes.NewBuffer(reqBody),
	)
	if err != nil {
		return nil, fmt.Errorf("failed to send token validation request: %w", err)
	}
	defer resp.Body.Close()

	// Check response status
	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("token validation failed with status: %d", resp.StatusCode)
	}

	// Decode response
	var validationResp model.TokenValidationResponse
	if err := json.NewDecoder(resp.Body).Decode(&validationResp); err != nil {
		return nil, fmt.Errorf("failed to decode token validation response: %w", err)
	}

	// Check if token is valid
	if !validationResp.Valid {
		return nil, fmt.Errorf("invalid token: %s", validationResp.Error)
	}

	// Return user claims
	return &model.UserClaims{
		UserID:    validationResp.UserID,
		Username:  validationResp.Username,
		Email:     validationResp.Email,
		SessionID: validationResp.SessionID,
		ExpiresAt: validationResp.ExpiresAt,
	}, nil
}

