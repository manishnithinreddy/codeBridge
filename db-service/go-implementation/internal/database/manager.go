package database

import (
	"context"
	"database/sql"
	"fmt"
	"time"

	"github.com/codebridge/db-service/internal/config"
	"github.com/codebridge/db-service/internal/model"
	"github.com/codebridge/db-service/pkg/logging"
	"github.com/codebridge/db-service/pkg/pool"
	"github.com/google/uuid"
)

// Manager handles database connections and operations
type Manager struct {
	config *config.DatabaseConfig
	pool   *pool.ConnectionPool
	logger *logging.Logger
}

// NewManager creates a new database manager
func NewManager(config config.DatabaseConfig, logger *logging.Logger) (*Manager, error) {
	// Create connection pool
	connectionPool := pool.NewConnectionPool(
		config.ConnMaxLife,
		config.MaxOpenConns,
		config.MaxIdleConns,
		logger,
	)

	return &Manager{
		config: &config,
		pool:   connectionPool,
		logger: logger,
	}, nil
}

// Connect creates a new database connection
func (m *Manager) Connect(userID string, req *model.ConnectionRequest) (*model.ConnectionResponse, error) {
	var db *sql.DB
	var err error

	// Create a new session ID
	sessionID := uuid.New().String()

	// Connect to the database based on type
	switch req.Type {
	case "mysql":
		db, err = m.connectMySQL(req)
	case "postgres":
		db, err = m.connectPostgres(req)
	case "sqlite":
		db, err = m.connectSQLite(req)
	default:
		return nil, fmt.Errorf("unsupported database type: %s", req.Type)
	}

	if err != nil {
		return &model.ConnectionResponse{
			SessionID: "",
			Type:      req.Type,
			Connected: false,
			Error:     err.Error(),
		}, nil
	}

	// Test the connection
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	if err := db.PingContext(ctx); err != nil {
		db.Close()
		return &model.ConnectionResponse{
			SessionID: "",
			Type:      req.Type,
			Connected: false,
			Error:     fmt.Sprintf("failed to ping database: %v", err),
		}, nil
	}

	// Set session expiry (24 hours by default)
	expiry := time.Now().Add(24 * time.Hour)

	// Add connection to pool
	m.pool.AddConnection(sessionID, userID, req.Type, db, expiry)

	return &model.ConnectionResponse{
		SessionID: sessionID,
		Type:      req.Type,
		Connected: true,
	}, nil
}

// Disconnect closes a database connection
func (m *Manager) Disconnect(sessionID string) (*model.DisconnectResponse, error) {
	if err := m.pool.RemoveConnection(sessionID); err != nil {
		return &model.DisconnectResponse{
			Success: false,
			Error:   err.Error(),
		}, nil
	}

	return &model.DisconnectResponse{
		Success: true,
	}, nil
}

// ExecuteQuery executes a SQL query
func (m *Manager) ExecuteQuery(sessionID string, req *model.QueryRequest) (*model.QueryResult, error) {
	// Get connection from pool
	db, err := m.pool.GetConnection(sessionID)
	if err != nil {
		return nil, fmt.Errorf("failed to get connection: %w", err)
	}

	// Create context with timeout if specified
	ctx := context.Background()
	if req.Timeout > 0 {
		var cancel context.CancelFunc
		ctx, cancel = context.WithTimeout(ctx, time.Duration(req.Timeout)*time.Second)
		defer cancel()
	}

	startTime := time.Now()
	result := &model.QueryResult{
		Warnings: []string{},
	}

	// Execute in transaction if requested
	if req.Transaction {
		tx, err := db.BeginTx(ctx, nil)
		if err != nil {
			return nil, fmt.Errorf("failed to begin transaction: %w", err)
		}
		defer func() {
			if err != nil {
				tx.Rollback()
			}
		}()

		result, err = m.executeQueryWithTx(ctx, tx, req)
		if err != nil {
			return nil, err
		}

		if err := tx.Commit(); err != nil {
			return nil, fmt.Errorf("failed to commit transaction: %w", err)
		}
	} else {
		result, err = m.executeQueryWithDB(ctx, db, req)
		if err != nil {
			return nil, err
		}
	}

	// Set execution time
	result.ExecutionTime = time.Since(startTime)

	return result, nil
}

// ExecuteBatchQueries executes multiple SQL queries
func (m *Manager) ExecuteBatchQueries(sessionID string, req *model.BatchQueryRequest) (*model.BatchQueryResult, error) {
	// Get connection from pool
	db, err := m.pool.GetConnection(sessionID)
	if err != nil {
		return nil, fmt.Errorf("failed to get connection: %w", err)
	}

	startTime := time.Now()
	result := &model.BatchQueryResult{
		Results: make([]model.QueryResult, 0, len(req.Queries)),
	}

	// Execute in transaction if requested
	if req.Transaction {
		tx, err := db.Begin()
		if err != nil {
			return nil, fmt.Errorf("failed to begin transaction: %w", err)
		}
		defer func() {
			if err != nil {
				tx.Rollback()
			}
		}()

		for _, query := range req.Queries {
			ctx := context.Background()
			if query.Timeout > 0 {
				var cancel context.CancelFunc
				ctx, cancel = context.WithTimeout(ctx, time.Duration(query.Timeout)*time.Second)
				defer cancel()
			}

			queryResult, err := m.executeQueryWithTx(ctx, tx, &query)
			if err != nil {
				result.Error = err.Error()
				return result, nil
			}

			result.Results = append(result.Results, *queryResult)
		}

		if err := tx.Commit(); err != nil {
			result.Error = fmt.Sprintf("failed to commit transaction: %v", err)
			return result, nil
		}
	} else {
		for _, query := range req.Queries {
			ctx := context.Background()
			if query.Timeout > 0 {
				var cancel context.CancelFunc
				ctx, cancel = context.WithTimeout(ctx, time.Duration(query.Timeout)*time.Second)
				defer cancel()
			}

			queryResult, err := m.executeQueryWithDB(ctx, db, &query)
			if err != nil {
				result.Error = err.Error()
				return result, nil
			}

			result.Results = append(result.Results, *queryResult)
		}
	}

	// Set execution time
	result.ExecutionTime = time.Since(startTime)

	return result, nil
}

// GetSchema retrieves database schema information
func (m *Manager) GetSchema(sessionID string, req *model.SchemaRequest) (*model.SchemaResult, error) {
	// Get connection from pool
	db, err := m.pool.GetConnection(sessionID)
	if err != nil {
		return nil, fmt.Errorf("failed to get connection: %w", err)
	}

	// Get session info to determine database type
	sessionInfo, exists := m.pool.GetSessionInfo(sessionID)
	if !exists {
		return nil, fmt.Errorf("session not found: %s", sessionID)
	}

	// Get schema based on database type
	var tables []model.TableInfo
	switch sessionInfo.Type {
	case "mysql":
		tables, err = m.getMySQLSchema(db, req.Schema, req.Table)
	case "postgres":
		tables, err = m.getPostgresSchema(db, req.Schema, req.Table)
	case "sqlite":
		tables, err = m.getSQLiteSchema(db, req.Table)
	default:
		return nil, fmt.Errorf("unsupported database type: %s", sessionInfo.Type)
	}

	if err != nil {
		return &model.SchemaResult{
			Tables: nil,
			Error:  err.Error(),
		}, nil
	}

	return &model.SchemaResult{
		Tables: tables,
	}, nil
}

// GetActiveConnectionCount returns the number of active connections
func (m *Manager) GetActiveConnectionCount() int {
	return m.pool.GetActiveConnectionCount()
}

// Close closes all database connections
func (m *Manager) Close() error {
	m.pool.Close()
	return nil
}

// Private helper methods

func (m *Manager) connectMySQL(req *model.ConnectionRequest) (*sql.DB, error) {
	// Use config defaults if not provided in request
	host := req.Host
	if host == "" {
		host = m.config.MySQL.Host
	}

	port := req.Port
	if port == 0 {
		port = m.config.MySQL.Port
	}

	user := req.User
	if user == "" {
		user = m.config.MySQL.User
	}

	password := req.Password
	if password == "" {
		password = m.config.MySQL.Password
	}

	database := req.Database
	if database == "" {
		database = m.config.MySQL.DBName
	}

	params := req.Params
	if params == "" {
		params = m.config.MySQL.Params
	}

	// Build connection string
	dsn := fmt.Sprintf("%s:%s@tcp(%s:%d)/%s", user, password, host, port, database)
	if params != "" {
		dsn += "?" + params
	}

	// Open connection
	return sql.Open("mysql", dsn)
}

func (m *Manager) connectPostgres(req *model.ConnectionRequest) (*sql.DB, error) {
	// Use config defaults if not provided in request
	host := req.Host
	if host == "" {
		host = m.config.Postgres.Host
	}

	port := req.Port
	if port == 0 {
		port = m.config.Postgres.Port
	}

	user := req.User
	if user == "" {
		user = m.config.Postgres.User
	}

	password := req.Password
	if password == "" {
		password = m.config.Postgres.Password
	}

	database := req.Database
	if database == "" {
		database = m.config.Postgres.DBName
	}

	sslMode := m.config.Postgres.SSLMode
	if req.Params != "" {
		sslMode = req.Params
	}

	// Build connection string
	dsn := fmt.Sprintf("host=%s port=%d user=%s password=%s dbname=%s sslmode=%s",
		host, port, user, password, database, sslMode)

	// Open connection
	return sql.Open("pgx", dsn)
}

func (m *Manager) connectSQLite(req *model.ConnectionRequest) (*sql.DB, error) {
	// Use config defaults if not provided in request
	path := req.FilePath
	if path == "" {
		path = m.config.SQLite.Path
	}

	// Open connection
	return sql.Open("sqlite3", path)
}

func (m *Manager) executeQueryWithDB(ctx context.Context, db *sql.DB, req *model.QueryRequest) (*model.QueryResult, error) {
	// Check if query is a SELECT or other query that returns rows
	if isSelectQuery(req.SQL) {
		return m.executeSelectQuery(ctx, db, req)
	}
	return m.executeUpdateQuery(ctx, db, req)
}

func (m *Manager) executeQueryWithTx(ctx context.Context, tx *sql.Tx, req *model.QueryRequest) (*model.QueryResult, error) {
	// Check if query is a SELECT or other query that returns rows
	if isSelectQuery(req.SQL) {
		return m.executeSelectQueryTx(ctx, tx, req)
	}
	return m.executeUpdateQueryTx(ctx, tx, req)
}

func (m *Manager) executeSelectQuery(ctx context.Context, db *sql.DB, req *model.QueryRequest) (*model.QueryResult, error) {
	// Execute query
	rows, err := db.QueryContext(ctx, req.SQL, req.Params...)
	if err != nil {
		return nil, fmt.Errorf("failed to execute query: %w", err)
	}
	defer rows.Close()

	return processRows(rows, req.MaxRows)
}

func (m *Manager) executeSelectQueryTx(ctx context.Context, tx *sql.Tx, req *model.QueryRequest) (*model.QueryResult, error) {
	// Execute query
	rows, err := tx.QueryContext(ctx, req.SQL, req.Params...)
	if err != nil {
		return nil, fmt.Errorf("failed to execute query: %w", err)
	}
	defer rows.Close()

	return processRows(rows, req.MaxRows)
}

func (m *Manager) executeUpdateQuery(ctx context.Context, db *sql.DB, req *model.QueryRequest) (*model.QueryResult, error) {
	// Execute query
	result, err := db.ExecContext(ctx, req.SQL, req.Params...)
	if err != nil {
		return nil, fmt.Errorf("failed to execute query: %w", err)
	}

	return processResult(result)
}

func (m *Manager) executeUpdateQueryTx(ctx context.Context, tx *sql.Tx, req *model.QueryRequest) (*model.QueryResult, error) {
	// Execute query
	result, err := tx.ExecContext(ctx, req.SQL, req.Params...)
	if err != nil {
		return nil, fmt.Errorf("failed to execute query: %w", err)
	}

	return processResult(result)
}

// Schema retrieval methods

func (m *Manager) getMySQLSchema(db *sql.DB, schema, table string) ([]model.TableInfo, error) {
	// Implementation for MySQL schema retrieval
	// This is a simplified version - a real implementation would be more comprehensive
	
	var query string
	var args []interface{}
	
	if table != "" {
		query = `
			SELECT 
				TABLE_NAME, 
				TABLE_SCHEMA,
				TABLE_TYPE
			FROM 
				INFORMATION_SCHEMA.TABLES 
			WHERE 
				TABLE_SCHEMA = COALESCE(?, DATABASE()) 
				AND TABLE_NAME = ?
		`
		args = []interface{}{schema, table}
	} else {
		query = `
			SELECT 
				TABLE_NAME, 
				TABLE_SCHEMA,
				TABLE_TYPE
			FROM 
				INFORMATION_SCHEMA.TABLES 
			WHERE 
				TABLE_SCHEMA = COALESCE(?, DATABASE())
		`
		args = []interface{}{schema}
	}
	
	rows, err := db.Query(query, args...)
	if err != nil {
		return nil, fmt.Errorf("failed to query tables: %w", err)
	}
	defer rows.Close()
	
	var tables []model.TableInfo
	for rows.Next() {
		var tableName, tableSchema, tableType string
		if err := rows.Scan(&tableName, &tableSchema, &tableType); err != nil {
			return nil, fmt.Errorf("failed to scan table row: %w", err)
		}
		
		// Get columns for this table
		columns, err := m.getMySQLColumns(db, tableSchema, tableName)
		if err != nil {
			return nil, err
		}
		
		tables = append(tables, model.TableInfo{
			Name:    tableName,
			Schema:  tableSchema,
			Type:    tableType,
			Columns: columns,
		})
	}
	
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("error iterating table rows: %w", err)
	}
	
	return tables, nil
}

func (m *Manager) getMySQLColumns(db *sql.DB, schema, table string) ([]model.ColumnInfo, error) {
	query := `
		SELECT 
			COLUMN_NAME, 
			DATA_TYPE,
			IS_NULLABLE,
			COLUMN_KEY,
			COLUMN_DEFAULT,
			COLUMN_COMMENT
		FROM 
			INFORMATION_SCHEMA.COLUMNS 
		WHERE 
			TABLE_SCHEMA = ? 
			AND TABLE_NAME = ?
		ORDER BY 
			ORDINAL_POSITION
	`
	
	rows, err := db.Query(query, schema, table)
	if err != nil {
		return nil, fmt.Errorf("failed to query columns: %w", err)
	}
	defer rows.Close()
	
	var columns []model.ColumnInfo
	for rows.Next() {
		var name, dataType, isNullable, columnKey, comment string
		var defaultValue sql.NullString
		
		if err := rows.Scan(&name, &dataType, &isNullable, &columnKey, &defaultValue, &comment); err != nil {
			return nil, fmt.Errorf("failed to scan column row: %w", err)
		}
		
		columns = append(columns, model.ColumnInfo{
			Name:         name,
			Type:         dataType,
			Nullable:     isNullable == "YES",
			PrimaryKey:   columnKey == "PRI",
			DefaultValue: defaultValue.String,
			Comment:      comment,
		})
	}
	
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("error iterating column rows: %w", err)
	}
	
	return columns, nil
}

func (m *Manager) getPostgresSchema(db *sql.DB, schema, table string) ([]model.TableInfo, error) {
	// Implementation for PostgreSQL schema retrieval
	// This is a simplified version - a real implementation would be more comprehensive
	
	if schema == "" {
		schema = "public"
	}
	
	var query string
	var args []interface{}
	
	if table != "" {
		query = `
			SELECT 
				tablename, 
				schemaname,
				CASE 
					WHEN tablename IN (SELECT viewname FROM pg_views WHERE schemaname = $1) THEN 'VIEW'
					ELSE 'TABLE'
				END as table_type
			FROM 
				pg_catalog.pg_tables 
			WHERE 
				schemaname = $1 
				AND tablename = $2
			UNION
			SELECT 
				viewname as tablename, 
				schemaname,
				'VIEW' as table_type
			FROM 
				pg_catalog.pg_views 
			WHERE 
				schemaname = $1 
				AND viewname = $2
		`
		args = []interface{}{schema, table}
	} else {
		query = `
			SELECT 
				tablename, 
				schemaname,
				CASE 
					WHEN tablename IN (SELECT viewname FROM pg_views WHERE schemaname = $1) THEN 'VIEW'
					ELSE 'TABLE'
				END as table_type
			FROM 
				pg_catalog.pg_tables 
			WHERE 
				schemaname = $1
			UNION
			SELECT 
				viewname as tablename, 
				schemaname,
				'VIEW' as table_type
			FROM 
				pg_catalog.pg_views 
			WHERE 
				schemaname = $1
		`
		args = []interface{}{schema}
	}
	
	rows, err := db.Query(query, args...)
	if err != nil {
		return nil, fmt.Errorf("failed to query tables: %w", err)
	}
	defer rows.Close()
	
	var tables []model.TableInfo
	for rows.Next() {
		var tableName, schemaName, tableType string
		if err := rows.Scan(&tableName, &schemaName, &tableType); err != nil {
			return nil, fmt.Errorf("failed to scan table row: %w", err)
		}
		
		// Get columns for this table
		columns, err := m.getPostgresColumns(db, schemaName, tableName)
		if err != nil {
			return nil, err
		}
		
		tables = append(tables, model.TableInfo{
			Name:    tableName,
			Schema:  schemaName,
			Type:    tableType,
			Columns: columns,
		})
	}
	
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("error iterating table rows: %w", err)
	}
	
	return tables, nil
}

func (m *Manager) getPostgresColumns(db *sql.DB, schema, table string) ([]model.ColumnInfo, error) {
	query := `
		SELECT 
			a.attname as column_name, 
			pg_catalog.format_type(a.atttypid, a.atttypmod) as data_type,
			CASE WHEN a.attnotnull THEN false ELSE true END as is_nullable,
			CASE WHEN p.contype = 'p' THEN true ELSE false END as is_primary,
			pg_catalog.pg_get_expr(d.adbin, d.adrelid) as column_default,
			col_description(a.attrelid, a.attnum) as column_comment
		FROM 
			pg_catalog.pg_attribute a
		LEFT JOIN 
			pg_catalog.pg_attrdef d ON (a.attrelid, a.attnum) = (d.adrelid, d.adnum)
		LEFT JOIN 
			pg_catalog.pg_constraint p ON p.conrelid = a.attrelid AND a.attnum = ANY(p.conkey) AND p.contype = 'p'
		JOIN 
			pg_catalog.pg_class c ON a.attrelid = c.oid
		JOIN 
			pg_catalog.pg_namespace n ON c.relnamespace = n.oid
		WHERE 
			a.attnum > 0 
			AND NOT a.attisdropped
			AND n.nspname = $1
			AND c.relname = $2
		ORDER BY 
			a.attnum
	`
	
	rows, err := db.Query(query, schema, table)
	if err != nil {
		return nil, fmt.Errorf("failed to query columns: %w", err)
	}
	defer rows.Close()
	
	var columns []model.ColumnInfo
	for rows.Next() {
		var name, dataType string
		var isNullable, isPrimary bool
		var defaultValue, comment sql.NullString
		
		if err := rows.Scan(&name, &dataType, &isNullable, &isPrimary, &defaultValue, &comment); err != nil {
			return nil, fmt.Errorf("failed to scan column row: %w", err)
		}
		
		columns = append(columns, model.ColumnInfo{
			Name:         name,
			Type:         dataType,
			Nullable:     isNullable,
			PrimaryKey:   isPrimary,
			DefaultValue: defaultValue.String,
			Comment:      comment.String,
		})
	}
	
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("error iterating column rows: %w", err)
	}
	
	return columns, nil
}

func (m *Manager) getSQLiteSchema(db *sql.DB, table string) ([]model.TableInfo, error) {
	// Implementation for SQLite schema retrieval
	// This is a simplified version - a real implementation would be more comprehensive
	
	var query string
	var args []interface{}
	
	if table != "" {
		query = `
			SELECT 
				name,
				'main' as schema,
				type
			FROM 
				sqlite_master 
			WHERE 
				type IN ('table', 'view') 
				AND name = ?
				AND name NOT LIKE 'sqlite_%'
		`
		args = []interface{}{table}
	} else {
		query = `
			SELECT 
				name,
				'main' as schema,
				type
			FROM 
				sqlite_master 
			WHERE 
				type IN ('table', 'view') 
				AND name NOT LIKE 'sqlite_%'
		`
	}
	
	rows, err := db.Query(query, args...)
	if err != nil {
		return nil, fmt.Errorf("failed to query tables: %w", err)
	}
	defer rows.Close()
	
	var tables []model.TableInfo
	for rows.Next() {
		var tableName, schemaName, tableType string
		if err := rows.Scan(&tableName, &schemaName, &tableType); err != nil {
			return nil, fmt.Errorf("failed to scan table row: %w", err)
		}
		
		// Get columns for this table
		columns, err := m.getSQLiteColumns(db, tableName)
		if err != nil {
			return nil, err
		}
		
		tables = append(tables, model.TableInfo{
			Name:    tableName,
			Schema:  schemaName,
			Type:    tableType,
			Columns: columns,
		})
	}
	
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("error iterating table rows: %w", err)
	}
	
	return tables, nil
}

func (m *Manager) getSQLiteColumns(db *sql.DB, table string) ([]model.ColumnInfo, error) {
	query := fmt.Sprintf("PRAGMA table_info(%s)", table)
	
	rows, err := db.Query(query)
	if err != nil {
		return nil, fmt.Errorf("failed to query columns: %w", err)
	}
	defer rows.Close()
	
	var columns []model.ColumnInfo
	for rows.Next() {
		var cid int
		var name, dataType, defaultValue string
		var notNull, pk int
		
		if err := rows.Scan(&cid, &name, &dataType, &notNull, &defaultValue, &pk); err != nil {
			return nil, fmt.Errorf("failed to scan column row: %w", err)
		}
		
		columns = append(columns, model.ColumnInfo{
			Name:         name,
			Type:         dataType,
			Nullable:     notNull == 0,
			PrimaryKey:   pk > 0,
			DefaultValue: defaultValue,
		})
	}
	
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("error iterating column rows: %w", err)
	}
	
	return columns, nil
}

// Helper functions

func isSelectQuery(sql string) bool {
	// Simple check - a more robust implementation would use a SQL parser
	sql = trimSQL(sql)
	return len(sql) >= 6 && (sql[:6] == "SELECT" || sql[:6] == "select")
}

func trimSQL(sql string) string {
	// Remove leading whitespace
	i := 0
	for i < len(sql) && (sql[i] == ' ' || sql[i] == '\t' || sql[i] == '\n' || sql[i] == '\r') {
		i++
	}
	return sql[i:]
}

func processRows(rows *sql.Rows, maxRows int) (*model.QueryResult, error) {
	// Get column names
	columns, err := rows.Columns()
	if err != nil {
		return nil, fmt.Errorf("failed to get column names: %w", err)
	}

	// Prepare result
	result := &model.QueryResult{
		Columns:     columns,
		Rows:        [][]interface{}{},
		RowsAffected: 0,
		Warnings:    []string{},
	}

	// Prepare values for scanning
	values := make([]interface{}, len(columns))
	valuePtrs := make([]interface{}, len(columns))
	for i := range columns {
		valuePtrs[i] = &values[i]
	}

	// Process rows
	rowCount := 0
	for rows.Next() {
		if maxRows > 0 && rowCount >= maxRows {
			result.Warnings = append(result.Warnings, fmt.Sprintf("Query returned more than %d rows, only showing first %d", maxRows, maxRows))
			break
		}

		if err := rows.Scan(valuePtrs...); err != nil {
			return nil, fmt.Errorf("failed to scan row: %w", err)
		}

		// Convert values to appropriate types
		row := make([]interface{}, len(columns))
		for i, v := range values {
			if v == nil {
				row[i] = nil
				continue
			}

			// Handle different types
			switch v := v.(type) {
			case []byte:
				// Try to convert to string if it's a text value
				row[i] = string(v)
			default:
				row[i] = v
			}
		}

		result.Rows = append(result.Rows, row)
		rowCount++
	}

	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("error iterating rows: %w", err)
	}

	result.RowsAffected = int64(rowCount)
	return result, nil
}

func processResult(result sql.Result) (*model.QueryResult, error) {
	// Get rows affected
	rowsAffected, err := result.RowsAffected()
	if err != nil {
		return nil, fmt.Errorf("failed to get rows affected: %w", err)
	}

	// Get last insert ID (if applicable)
	lastInsertID, err := result.LastInsertId()
	if err != nil && err.Error() != "LastInsertId is not supported by this driver" {
		return nil, fmt.Errorf("failed to get last insert ID: %w", err)
	}

	return &model.QueryResult{
		Columns:      []string{},
		Rows:         [][]interface{}{},
		RowsAffected: rowsAffected,
		LastInsertID: lastInsertID,
		Warnings:     []string{},
	}, nil
}

