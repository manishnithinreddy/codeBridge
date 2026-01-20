package model

import "time"

// QueryRequest represents a database query request
type QueryRequest struct {
	SQL         string                 `json:"sql" binding:"required"`
	Params      []interface{}          `json:"params,omitempty"`
	SessionID   string                 `json:"session_id" binding:"required"`
	MaxRows     int                    `json:"max_rows,omitempty"`
	Timeout     int                    `json:"timeout,omitempty"` // in seconds
	Transaction bool                   `json:"transaction,omitempty"`
	Metadata    map[string]interface{} `json:"metadata,omitempty"`
}

// QueryResult represents a database query result
type QueryResult struct {
	Columns     []string        `json:"columns"`
	Rows        [][]interface{} `json:"rows"`
	RowsAffected int64          `json:"rows_affected"`
	LastInsertID int64          `json:"last_insert_id,omitempty"`
	ExecutionTime time.Duration `json:"execution_time"`
	Error        string         `json:"error,omitempty"`
	Warnings     []string       `json:"warnings,omitempty"`
}

// BatchQueryRequest represents a batch of database queries
type BatchQueryRequest struct {
	Queries    []QueryRequest `json:"queries" binding:"required"`
	SessionID  string         `json:"session_id" binding:"required"`
	Transaction bool          `json:"transaction,omitempty"`
}

// BatchQueryResult represents the results of a batch of queries
type BatchQueryResult struct {
	Results      []QueryResult `json:"results"`
	ExecutionTime time.Duration `json:"execution_time"`
	Error        string        `json:"error,omitempty"`
}

// SchemaRequest represents a request for database schema information
type SchemaRequest struct {
	SessionID string `json:"session_id" binding:"required"`
	Table     string `json:"table,omitempty"`
	Schema    string `json:"schema,omitempty"`
}

// TableInfo represents information about a database table
type TableInfo struct {
	Name    string       `json:"name"`
	Schema  string       `json:"schema,omitempty"`
	Type    string       `json:"type,omitempty"` // TABLE, VIEW, etc.
	Columns []ColumnInfo `json:"columns"`
}

// ColumnInfo represents information about a database column
type ColumnInfo struct {
	Name         string `json:"name"`
	Type         string `json:"type"`
	Nullable     bool   `json:"nullable"`
	PrimaryKey   bool   `json:"primary_key"`
	DefaultValue string `json:"default_value,omitempty"`
	Comment      string `json:"comment,omitempty"`
}

// SchemaResult represents the result of a schema request
type SchemaResult struct {
	Tables []TableInfo `json:"tables"`
	Error  string      `json:"error,omitempty"`
}

// ConnectionRequest represents a database connection request
type ConnectionRequest struct {
	Type     string `json:"type" binding:"required,oneof=mysql postgres sqlite"`
	Host     string `json:"host,omitempty"`
	Port     int    `json:"port,omitempty"`
	User     string `json:"user,omitempty"`
	Password string `json:"password,omitempty"`
	Database string `json:"database,omitempty"`
	Params   string `json:"params,omitempty"`
	FilePath string `json:"file_path,omitempty"` // For SQLite
}

// ConnectionResponse represents a database connection response
type ConnectionResponse struct {
	SessionID string `json:"session_id"`
	Type      string `json:"type"`
	Connected bool   `json:"connected"`
	Error     string `json:"error,omitempty"`
}

// DisconnectRequest represents a database disconnect request
type DisconnectRequest struct {
	SessionID string `json:"session_id" binding:"required"`
}

// DisconnectResponse represents a database disconnect response
type DisconnectResponse struct {
	Success bool   `json:"success"`
	Error   string `json:"error,omitempty"`
}

// HealthResponse represents a health check response
type HealthResponse struct {
	Status           string `json:"status"`
	Version          string `json:"version"`
	ActiveConnections int    `json:"active_connections"`
}

