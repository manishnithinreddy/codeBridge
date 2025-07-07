package securityclient

import (
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	"github.com/codebridge/session-service/pkg/logging"
)

// SecurityClient is a client for the codebridge-security service
type SecurityClient struct {
	baseURL string
	client  *http.Client
	logger  *logging.Logger
}

// NewSecurityClient creates a new SecurityClient
func NewSecurityClient(baseURL string, logger *logging.Logger) *SecurityClient {
	return &SecurityClient{
		baseURL: baseURL,
		client: &http.Client{
			Timeout: 5 * time.Second, // Configurable timeout
		},
		logger: logger,
	}
}

// GetUserPermissionsResponse represents the response from the GetUserPermissions API
type PermissionDto struct {
	Id          int    `json:"id"`
	Name        string `json:"name"`
	Description string `json:"description"`
	ResourceType string `json:"resourceType"`
	Action      string `json:"action"`
}

// GetUserPermissions fetches user permissions from the security service
func (c *SecurityClient) GetUserPermissions(userID string) ([]string, error) {
	url := fmt.Sprintf("%s/api/rbac/users/%s/permissions", c.baseURL, userID)
	
	resp, err := c.client.Get(url)
	if err != nil {
		c.logger.Error("Failed to send request to security service", "error", err, "url", url)
		return nil, fmt.Errorf("failed to connect to security service: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		// Attempt to decode error response if available
		var errorBody map[string]interface{}
		if err := json.NewDecoder(resp.Body).Decode(&errorBody); err == nil {
			c.logger.Error("Security service returned an error", "status", resp.StatusCode, "error_body", errorBody)
			return nil, fmt.Errorf("security service error: %v (status: %d)", errorBody, resp.StatusCode)
		}
		c.logger.Error("Security service returned non-OK status", "status", resp.StatusCode)
		return nil, fmt.Errorf("security service returned status %d", resp.StatusCode)
	}

	var permissionDtos []PermissionDto
	if err := json.NewDecoder(resp.Body).Decode(&permissionDtos); err != nil {
		c.logger.Error("Failed to decode success response from security service", "error", err, "url", url)
		return nil, fmt.Errorf("failed to parse security service response: %w", err)
	}

	permissions := make([]string, len(permissionDtos))
	for i, dto := range permissionDtos {
		permissions[i] = dto.Name
	}

	return permissions, nil
}
