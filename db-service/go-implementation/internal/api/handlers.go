package api

import (
	"net/http"

	"github.com/codebridge/db-service/internal/model"
	"github.com/codebridge/db-service/internal/service"
	"github.com/gin-gonic/gin"
)

// handleConnect handles database connection requests
func handleConnect(dbService *service.DBService) gin.HandlerFunc {
	return func(c *gin.Context) {
		// Get token from context
		token, exists := c.Get("token")
		if !exists {
			c.JSON(http.StatusUnauthorized, gin.H{"error": "Authorization token is required"})
			return
		}

		// Parse request
		var req model.ConnectionRequest
		if err := c.ShouldBindJSON(&req); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}

		// Connect to database
		resp, err := dbService.Connect(token.(string), &req)
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
			return
		}

		c.JSON(http.StatusOK, resp)
	}
}

// handleDisconnect handles database disconnection requests
func handleDisconnect(dbService *service.DBService) gin.HandlerFunc {
	return func(c *gin.Context) {
		// Get token from context
		token, exists := c.Get("token")
		if !exists {
			c.JSON(http.StatusUnauthorized, gin.H{"error": "Authorization token is required"})
			return
		}

		// Parse request
		var req model.DisconnectRequest
		if err := c.ShouldBindJSON(&req); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}

		// Disconnect from database
		resp, err := dbService.Disconnect(token.(string), &req)
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
			return
		}

		c.JSON(http.StatusOK, resp)
	}
}

// handleQuery handles database query requests
func handleQuery(dbService *service.DBService) gin.HandlerFunc {
	return func(c *gin.Context) {
		// Get token from context
		token, exists := c.Get("token")
		if !exists {
			c.JSON(http.StatusUnauthorized, gin.H{"error": "Authorization token is required"})
			return
		}

		// Parse request
		var req model.QueryRequest
		if err := c.ShouldBindJSON(&req); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}

		// Execute query
		resp, err := dbService.ExecuteQuery(token.(string), &req)
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
			return
		}

		c.JSON(http.StatusOK, resp)
	}
}

// handleBatchQuery handles batch query requests
func handleBatchQuery(dbService *service.DBService) gin.HandlerFunc {
	return func(c *gin.Context) {
		// Get token from context
		token, exists := c.Get("token")
		if !exists {
			c.JSON(http.StatusUnauthorized, gin.H{"error": "Authorization token is required"})
			return
		}

		// Parse request
		var req model.BatchQueryRequest
		if err := c.ShouldBindJSON(&req); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}

		// Execute batch query
		resp, err := dbService.ExecuteBatchQueries(token.(string), &req)
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
			return
		}

		c.JSON(http.StatusOK, resp)
	}
}

// handleSchema handles schema information requests
func handleSchema(dbService *service.DBService) gin.HandlerFunc {
	return func(c *gin.Context) {
		// Get token from context
		token, exists := c.Get("token")
		if !exists {
			c.JSON(http.StatusUnauthorized, gin.H{"error": "Authorization token is required"})
			return
		}

		// Parse request
		var req model.SchemaRequest
		if err := c.ShouldBindJSON(&req); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}

		// Get schema information
		resp, err := dbService.GetSchema(token.(string), &req)
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
			return
		}

		c.JSON(http.StatusOK, resp)
	}
}

