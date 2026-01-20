package api

import (
	"net/http"

	"github.com/codebridge/session-service/internal/model"
	"github.com/codebridge/session-service/internal/service"
	"github.com/gin-gonic/gin"
)

// handleRegister handles user registration
func handleRegister(sessionService *service.SessionService) gin.HandlerFunc {
	return func(c *gin.Context) {
		var req model.RegisterRequest
		if err := c.ShouldBindJSON(&req); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}

		profile, err := sessionService.RegisterUser(c.Request.Context(), &req)
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}

		c.JSON(http.StatusCreated, profile)
	}
}

// handleLogin handles user login
func handleLogin(sessionService *service.SessionService) gin.HandlerFunc {
	return func(c *gin.Context) {
		var req model.LoginRequest
		if err := c.ShouldBindJSON(&req); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}

		userAgent := c.Request.UserAgent()
		clientIP := c.ClientIP()

		tokenResponse, err := sessionService.Login(c.Request.Context(), &req, userAgent, clientIP)
		if err != nil {
			c.JSON(http.StatusUnauthorized, gin.H{"error": err.Error()})
			return
		}

		c.JSON(http.StatusOK, tokenResponse)
	}
}

// handleRefreshToken handles token refresh
func handleRefreshToken(sessionService *service.SessionService) gin.HandlerFunc {
	return func(c *gin.Context) {
		var req model.RefreshTokenRequest
		if err := c.ShouldBindJSON(&req); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}

		tokenResponse, err := sessionService.RefreshToken(c.Request.Context(), &req)
		if err != nil {
			c.JSON(http.StatusUnauthorized, gin.H{"error": err.Error()})
			return
		}

		c.JSON(http.StatusOK, tokenResponse)
	}
}

// handleLogout handles user logout
func handleLogout(sessionService *service.SessionService) gin.HandlerFunc {
	return func(c *gin.Context) {
		var req model.RefreshTokenRequest
		if err := c.ShouldBindJSON(&req); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}

		if err := sessionService.Logout(c.Request.Context(), req.RefreshToken); err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
			return
		}

		c.JSON(http.StatusOK, gin.H{"message": "Successfully logged out"})
	}
}

// handleLogoutAll handles logging out all sessions for a user
func handleLogoutAll(sessionService *service.SessionService) gin.HandlerFunc {
	return func(c *gin.Context) {
		userID, exists := c.Get("user_id")
		if !exists {
			c.JSON(http.StatusUnauthorized, gin.H{"error": "User not authenticated"})
			return
		}

		if err := sessionService.LogoutAll(c.Request.Context(), userID.(string)); err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
			return
		}

		c.JSON(http.StatusOK, gin.H{"message": "Successfully logged out from all devices"})
	}
}

// handleGetProfile handles getting user profile
func handleGetProfile(sessionService *service.SessionService) gin.HandlerFunc {
	return func(c *gin.Context) {
		userID, exists := c.Get("user_id")
		if !exists {
			c.JSON(http.StatusUnauthorized, gin.H{"error": "User not authenticated"})
			return
		}

		profile, err := sessionService.GetUserProfile(c.Request.Context(), userID.(string))
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
			return
		}

		c.JSON(http.StatusOK, profile)
	}
}

// handleGetSessions handles getting user sessions
func handleGetSessions(sessionService *service.SessionService) gin.HandlerFunc {
	return func(c *gin.Context) {
		userID, exists := c.Get("user_id")
		if !exists {
			c.JSON(http.StatusUnauthorized, gin.H{"error": "User not authenticated"})
			return
		}

		sessions, err := sessionService.GetUserSessions(c.Request.Context(), userID.(string))
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
			return
		}

		c.JSON(http.StatusOK, sessions)
	}
}

// handleValidateToken handles token validation
func handleValidateToken(sessionService *service.SessionService) gin.HandlerFunc {
	return func(c *gin.Context) {
		// Get token from request
		var req struct {
			Token string `json:"token" binding:"required"`
		}
		if err := c.ShouldBindJSON(&req); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}

		// Validate token
		claims, err := sessionService.ValidateToken(c.Request.Context(), req.Token)
		if err != nil {
			c.JSON(http.StatusUnauthorized, gin.H{"error": err.Error()})
			return
		}

		// Return claims
		c.JSON(http.StatusOK, gin.H{
			"valid":      true,
			"user_id":    claims.UserID,
			"username":   claims.Username,
			"email":      claims.Email,
			"session_id": claims.SessionID,
			"expires_at": claims.ExpiresAt,
		})
	}
}

