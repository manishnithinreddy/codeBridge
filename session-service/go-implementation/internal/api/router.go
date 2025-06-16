package api

import (
	"net/http"

	"github.com/codebridge/session-service/internal/config"
	"github.com/codebridge/session-service/internal/service"
	"github.com/codebridge/session-service/pkg/logging"
	"github.com/gin-gonic/gin"
)

// NewRouter creates a new HTTP router
func NewRouter(sessionService *service.SessionService, cfg *config.Config, logger *logging.Logger) *gin.Engine {
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
		c.JSON(http.StatusOK, gin.H{
			"status": "UP",
			"service": "session-service",
			"version": "1.0.0",
		})
	})

	// API routes
	api := router.Group("/api")
	{
		// Public routes
		api.POST("/register", handleRegister(sessionService))
		api.POST("/login", handleLogin(sessionService))
		api.POST("/refresh", handleRefreshToken(sessionService))
		api.POST("/logout", handleLogout(sessionService))

		// Protected routes
		authorized := api.Group("/")
		authorized.Use(authMiddleware(sessionService))
		{
			authorized.GET("/profile", handleGetProfile(sessionService))
			authorized.GET("/sessions", handleGetSessions(sessionService))
			authorized.POST("/logout-all", handleLogoutAll(sessionService))
		}

		// Token validation endpoint (for other services)
		api.POST("/validate-token", handleValidateToken(sessionService))
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

