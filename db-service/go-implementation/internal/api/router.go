package api

import (
	"net/http"

	"github.com/codebridge/db-service/internal/config"
	"github.com/codebridge/db-service/internal/service"
	"github.com/codebridge/db-service/pkg/logging"
	"github.com/gin-gonic/gin"
)

// NewRouter creates a new HTTP router
func NewRouter(dbService *service.DBService, cfg *config.Config, logger *logging.Logger) *gin.Engine {
	// Set Gin mode
	if gin.Mode() == gin.DebugMode {
		gin.SetMode(gin.DebugMode)
	} else {
		gin.SetMode(gin.ReleaseMode)
	}

	// Create router
	router := gin.New()

	// Add middleware
	router.Use(gin.Recovery())
	router.Use(corsMiddleware())
	router.Use(requestLoggerMiddleware(logger))

	// Health check
	router.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, dbService.HealthCheck())
	})

	// API routes
	api := router.Group("/api")
	{
		// All routes require authentication
		api.Use(authMiddleware())

		// Connection management
		api.POST("/connect", handleConnect(dbService))
		api.POST("/disconnect", handleDisconnect(dbService))

		// Query execution
		api.POST("/query", handleQuery(dbService))
		api.POST("/batch", handleBatchQuery(dbService))

		// Schema information
		api.POST("/schema", handleSchema(dbService))
	}

	return router
}

// corsMiddleware adds CORS headers
func corsMiddleware() gin.HandlerFunc {
	return func(c *gin.Context) {
		c.Writer.Header().Set("Access-Control-Allow-Origin", "*")
		c.Writer.Header().Set("Access-Control-Allow-Credentials", "true")
		c.Writer.Header().Set("Access-Control-Allow-Headers", "Content-Type, Content-Length, Accept-Encoding, X-CSRF-Token, Authorization, accept, origin, Cache-Control, X-Requested-With")
		c.Writer.Header().Set("Access-Control-Allow-Methods", "POST, OPTIONS, GET, PUT, DELETE")

		if c.Request.Method == "OPTIONS" {
			c.AbortWithStatus(204)
			return
		}

		c.Next()
	}
}

// requestLoggerMiddleware logs HTTP requests
func requestLoggerMiddleware(logger *logging.Logger) gin.HandlerFunc {
	return func(c *gin.Context) {
		// Process request
		c.Next()

		// Log request
		logger.Info("Request",
			"method", c.Request.Method,
			"path", c.Request.URL.Path,
			"status", c.Writer.Status(),
			"client_ip", c.ClientIP(),
			"user_agent", c.Request.UserAgent(),
		)
	}
}

// authMiddleware extracts the JWT token from the Authorization header
func authMiddleware() gin.HandlerFunc {
	return func(c *gin.Context) {
		// Get authorization header
		token := c.GetHeader("Authorization")
		if token == "" {
			c.JSON(http.StatusUnauthorized, gin.H{"error": "Authorization header is required"})
			c.Abort()
			return
		}

		// Remove "Bearer " prefix if present
		if len(token) > 7 && token[:7] == "Bearer " {
			token = token[7:]
		}

		// Store token in context
		c.Set("token", token)

		c.Next()
	}
}

