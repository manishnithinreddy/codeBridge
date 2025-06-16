package model

import "time"

// ConnectionRequest represents a database connection request
type ConnectionRequest struct {
	Type     string `json:"type"`               // mysql, postgres, sqlite
	Host     string `json:"host,omitempty"`     // Database host
	Port     int    `json:"port,omitempty"`     // Database port
	User     string `json:"user,omitempty"`     // Database user
	Password string `json:"password,omitempty"` // Database password
	Database string `json:"database,omitempty"` // Database name
	FilePath string `json:"filePath,omitempty"` // SQLite file path
	Params   string `json:"params,omitempty"`   // Additional connection parameters
}

// ConnectionResponse represents a database connection response
type ConnectionResponse struct {
	SessionID string `json:"sessionId"`         // Session ID for the connection
	Type      string `json:"type"`              // Database type
	Connected bool   `json:"connected"`         // Connection status
	Error     string `json:"error,omitempty"`   // Error message if connection failed
}

// DisconnectRequest represents a database disconnection request
type DisconnectRequest struct {
	SessionID string `json:"sessionId"` // Session ID for the connection
}

// DisconnectResponse represents a database disconnection response
type DisconnectResponse struct {
	Success bool   `json:"success"`          // Disconnection status
	Error   string `json:"error,omitempty"`  // Error message if disconnection failed
}

// QueryRequest represents a database query request
type QueryRequest struct {
	SessionID   string        `json:"sessionId"`             // Session ID for the connection
	SQL         string        `json:"sql"`                   // SQL query
	Params      []interface{} `json:"params,omitempty"`      // Query parameters
	Transaction bool          `json:"transaction,omitempty"` // Execute in transaction
	Timeout     int           `json:"timeout,omitempty"`     // Query timeout in seconds
	MaxRows     int           `json:"maxRows,omitempty"`     // Maximum number of rows to return
}

// BatchQueryRequest represents a batch query request
type BatchQueryRequest struct {
	SessionID   string        `json:"sessionId"`             // Session ID for the connection
	Queries     []QueryRequest `json:"queries"`              // Queries to execute
	Transaction bool          `json:"transaction,omitempty"` // Execute in transaction
}

// QueryResult represents a database query result
type QueryResult struct {
	Columns      []string        `json:"columns"`                // Column names
	Rows         [][]interface{} `json:"rows"`                   // Row data
	RowsAffected int64           `json:"rowsAffected"`           // Number of rows affected
	LastInsertID int64           `json:"lastInsertId,omitempty"` // Last insert ID
	ExecutionTime time.Duration  `json:"executionTime,omitempty"`// Query execution time
	Warnings     []string        `json:"warnings,omitempty"`     // Warning messages
}

// BatchQueryResult represents a batch query result
type BatchQueryResult struct {
	Results      []QueryResult  `json:"results"`                // Query results
	ExecutionTime time.Duration `json:"executionTime,omitempty"`// Total execution time
	Error        string         `json:"error,omitempty"`        // Error message if execution failed
}

// SchemaRequest represents a database schema request
type SchemaRequest struct {
	SessionID string `json:"sessionId"`          // Session ID for the connection
	Schema    string `json:"schema,omitempty"`   // Schema name (for MySQL/PostgreSQL)
	Table     string `json:"table,omitempty"`    // Table name
}

// SchemaResult represents a database schema result
type SchemaResult struct {
	Tables []TableInfo `json:"tables"`           // Table information
	Error  string      `json:"error,omitempty"`  // Error message if retrieval failed
}

// TableInfo represents table information
type TableInfo struct {
	Name    string       `json:"name"`    // Table name
	Schema  string       `json:"schema"`  // Schema name
	Type    string       `json:"type"`    // Table type (TABLE, VIEW, etc.)
	Columns []ColumnInfo `json:"columns"` // Column information
}

// ColumnInfo represents column information
type ColumnInfo struct {
	Name         string `json:"name"`                   // Column name
	Type         string `json:"type"`                   // Data type
	Nullable     bool   `json:"nullable"`               // Whether the column is nullable
	PrimaryKey   bool   `json:"primaryKey"`             // Whether the column is a primary key
	DefaultValue string `json:"defaultValue,omitempty"` // Default value
	Comment      string `json:"comment,omitempty"`      // Column comment
}

// HealthResponse represents a health check response
type HealthResponse struct {
	Status           string `json:"status"`           // Service status (UP, DOWN)
	Version          string `json:"version"`          // Service version
	ActiveConnections int    `json:"activeConnections"` // Number of active connections
}

// TokenValidationRequest represents a token validation request
type TokenValidationRequest struct {
	Token string `json:"token"` // JWT token
}

// TokenValidationResponse represents a token validation response
type TokenValidationResponse struct {
	Valid     bool   `json:"valid"`               // Token validity
	UserID    string `json:"userId,omitempty"`    // User ID
	Username  string `json:"username,omitempty"`  // Username
	Email     string `json:"email,omitempty"`     // Email
	SessionID string `json:"sessionId,omitempty"` // Session ID
	ExpiresAt int64  `json:"expiresAt,omitempty"` // Token expiration time
	Error     string `json:"error,omitempty"`     // Error message if validation failed
}

// UserClaims represents JWT token claims
type UserClaims struct {
	UserID    string `json:"userId"`    // User ID
	Username  string `json:"username"`  // Username
	Email     string `json:"email"`     // Email
	SessionID string `json:"sessionId"` // Session ID
	ExpiresAt int64  `json:"expiresAt"` // Token expiration time
}

