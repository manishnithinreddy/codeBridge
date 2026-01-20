package api

import (
	"encoding/json"
	"fmt"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/codebridge/feature-flag-service/internal/model"
	"github.com/codebridge/feature-flag-service/internal/service"
	"github.com/gorilla/mux"
)

// FeatureFlagHandler handles HTTP requests for feature flags
type FeatureFlagHandler struct {
	service service.FeatureFlagService
}

// NewFeatureFlagHandler creates a new feature flag handler
func NewFeatureFlagHandler(service service.FeatureFlagService) *FeatureFlagHandler {
	return &FeatureFlagHandler{
		service: service,
	}
}

// RegisterRoutes registers the routes for the handler
func (h *FeatureFlagHandler) RegisterRoutes(router *mux.Router) {
	// Flag operations
	router.HandleFunc("/api/v1/flags/{key}", h.GetFlag).Methods("GET")
	router.HandleFunc("/api/v1/flags", h.GetFlags).Methods("GET").Queries("keys", "{keys}")
	router.HandleFunc("/api/v1/flags", h.ListFlags).Methods("GET")
	router.HandleFunc("/api/v1/flags/{key}", h.SetFlag).Methods("PUT")
	router.HandleFunc("/api/v1/flags/{key}", h.DeleteFlag).Methods("DELETE")

	// Evaluation
	router.HandleFunc("/api/v1/flags/{key}/evaluate", h.EvaluateFlag).Methods("POST")

	// Health check
	router.HandleFunc("/health", h.HealthCheck).Methods("GET")
}

// GetFlag gets a flag by key
func (h *FeatureFlagHandler) GetFlag(w http.ResponseWriter, r *http.Request) {
	// Get the key from the URL
	vars := mux.Vars(r)
	key := vars["key"]

	// Get the namespace from the query
	namespace := r.URL.Query().Get("namespace")

	// Get the flag
	flag, err := h.service.GetFlag(r.Context(), key, namespace)
	if err != nil {
		http.Error(w, fmt.Sprintf("Failed to get flag: %v", err), http.StatusInternalServerError)
		return
	}

	// If flag not found, return 404
	if flag == nil {
		http.Error(w, "Flag not found", http.StatusNotFound)
		return
	}

	// Return the flag
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(flag)
}

// GetFlags gets multiple flags by keys
func (h *FeatureFlagHandler) GetFlags(w http.ResponseWriter, r *http.Request) {
	// Get the keys from the query
	keysParam := r.URL.Query().Get("keys")
	keys := strings.Split(keysParam, ",")

	// Get the namespace from the query
	namespace := r.URL.Query().Get("namespace")

	// Get the flags
	flags, err := h.service.GetFlags(r.Context(), keys, namespace)
	if err != nil {
		http.Error(w, fmt.Sprintf("Failed to get flags: %v", err), http.StatusInternalServerError)
		return
	}

	// Return the flags
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(flags)
}

// ListFlags lists flags
func (h *FeatureFlagHandler) ListFlags(w http.ResponseWriter, r *http.Request) {
	// Get the namespace from the query
	namespace := r.URL.Query().Get("namespace")

	// Get the prefix from the query
	prefix := r.URL.Query().Get("prefix")

	// Get the tag filter from the query
	tagFilter := r.URL.Query().Get("tag")

	// List flags
	var flags []*model.FeatureFlag
	var err error

	if tagFilter != "" {
		// Parse tag filter (format: key=value)
		tagKey, tagValue := parseTagFilter(tagFilter)
		flags, err = h.service.ListFlagsByTag(r.Context(), namespace, tagKey, tagValue)
	} else {
		flags, err = h.service.ListFlags(r.Context(), namespace, prefix)
	}

	if err != nil {
		http.Error(w, fmt.Sprintf("Failed to list flags: %v", err), http.StatusInternalServerError)
		return
	}

	// Return the flags
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(flags)
}

// SetFlag sets a flag
func (h *FeatureFlagHandler) SetFlag(w http.ResponseWriter, r *http.Request) {
	// Get the key from the URL
	vars := mux.Vars(r)
	key := vars["key"]

	// Parse the request body
	var request struct {
		Namespace     string            `json:"namespace"`
		Value         json.RawMessage   `json:"value"`
		ValueType     string            `json:"valueType"`
		Description   string            `json:"description"`
		Tags          map[string]string `json:"tags"`
		Temporary     bool              `json:"temporary"`
		ExpirationTime int64            `json:"expirationTime"`
		Version       string            `json:"version"`
	}

	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		http.Error(w, fmt.Sprintf("Failed to parse request: %v", err), http.StatusBadRequest)
		return
	}

	// Create the flag
	flag := &model.FeatureFlag{
		Key:         key,
		Namespace:   request.Namespace,
		Description: request.Description,
		Tags:        request.Tags,
		Temporary:   request.Temporary,
		Version:     request.Version,
	}

	// Set expiration time if provided
	if request.ExpirationTime > 0 {
		expiresAt := time.UnixMilli(request.ExpirationTime)
		flag.ExpiresAt = &expiresAt
	}

	// Set the value based on the type
	switch request.ValueType {
	case "BOOLEAN":
		var value bool
		if err := json.Unmarshal(request.Value, &value); err != nil {
			http.Error(w, fmt.Sprintf("Failed to parse boolean value: %v", err), http.StatusBadRequest)
			return
		}
		flag.Value = model.NewBooleanValue(value)
	case "STRING":
		var value string
		if err := json.Unmarshal(request.Value, &value); err != nil {
			http.Error(w, fmt.Sprintf("Failed to parse string value: %v", err), http.StatusBadRequest)
			return
		}
		flag.Value = model.NewStringValue(value)
	case "INTEGER":
		var value int64
		if err := json.Unmarshal(request.Value, &value); err != nil {
			http.Error(w, fmt.Sprintf("Failed to parse integer value: %v", err), http.StatusBadRequest)
			return
		}
		flag.Value = model.NewIntegerValue(value)
	case "DOUBLE":
		var value float64
		if err := json.Unmarshal(request.Value, &value); err != nil {
			http.Error(w, fmt.Sprintf("Failed to parse double value: %v", err), http.StatusBadRequest)
			return
		}
		flag.Value = model.NewDoubleValue(value)
	case "JSON":
		var value string
		if err := json.Unmarshal(request.Value, &value); err != nil {
			http.Error(w, fmt.Sprintf("Failed to parse JSON value: %v", err), http.StatusBadRequest)
			return
		}
		flag.Value = model.NewJSONValue(value)
	default:
		http.Error(w, fmt.Sprintf("Unsupported value type: %s", request.ValueType), http.StatusBadRequest)
		return
	}

	// Set the flag
	if err := h.service.SetFlag(r.Context(), flag); err != nil {
		http.Error(w, fmt.Sprintf("Failed to set flag: %v", err), http.StatusInternalServerError)
		return
	}

	// Return success
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(map[string]interface{}{
		"success": true,
		"version": flag.Version,
	})
}

// DeleteFlag deletes a flag
func (h *FeatureFlagHandler) DeleteFlag(w http.ResponseWriter, r *http.Request) {
	// Get the key from the URL
	vars := mux.Vars(r)
	key := vars["key"]

	// Get the namespace from the query
	namespace := r.URL.Query().Get("namespace")

	// Delete the flag
	if err := h.service.DeleteFlag(r.Context(), key, namespace); err != nil {
		http.Error(w, fmt.Sprintf("Failed to delete flag: %v", err), http.StatusInternalServerError)
		return
	}

	// Return success
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(map[string]interface{}{
		"success": true,
	})
}

// EvaluateFlag evaluates a flag with context
func (h *FeatureFlagHandler) EvaluateFlag(w http.ResponseWriter, r *http.Request) {
	// Get the key from the URL
	vars := mux.Vars(r)
	key := vars["key"]

	// Parse the request body
	var request struct {
		Namespace         string            `json:"namespace"`
		UserId            string            `json:"userId"`
		SessionId         string            `json:"sessionId"`
		Attributes        map[string]string `json:"attributes"`
		NumericAttributes map[string]float64 `json:"numericAttributes"`
		BooleanAttributes map[string]bool   `json:"booleanAttributes"`
		ServiceContext    struct {
			ServiceName    string            `json:"serviceName"`
			ServiceVersion string            `json:"serviceVersion"`
			InstanceId     string            `json:"instanceId"`
			Environment    string            `json:"environment"`
			Metrics        map[string]string `json:"metrics"`
		} `json:"serviceContext"`
	}

	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		http.Error(w, fmt.Sprintf("Failed to parse request: %v", err), http.StatusBadRequest)
		return
	}

	// Create the evaluation context
	evalCtx := &model.EvaluationContext{
		UserID:            request.UserId,
		SessionID:         request.SessionId,
		Attributes:        request.Attributes,
		NumericAttributes: request.NumericAttributes,
		BooleanAttributes: request.BooleanAttributes,
		ServiceContext: &model.ServiceContext{
			ServiceName:    request.ServiceContext.ServiceName,
			ServiceVersion: request.ServiceContext.ServiceVersion,
			InstanceID:     request.ServiceContext.InstanceId,
			Environment:    request.ServiceContext.Environment,
			Metrics:        request.ServiceContext.Metrics,
		},
	}

	// Evaluate the flag
	result, err := h.service.EvaluateFlag(r.Context(), key, request.Namespace, evalCtx)
	if err != nil {
		http.Error(w, fmt.Sprintf("Failed to evaluate flag: %v", err), http.StatusInternalServerError)
		return
	}

	// Return the result
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(result)
}

// HealthCheck performs a health check
func (h *FeatureFlagHandler) HealthCheck(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"status":  "UP",
		"version": "1.0.0",
		"details": map[string]string{
			"service": "feature-flag-service",
			"time":    time.Now().Format(time.RFC3339),
		},
	})
}

// parseTagFilter parses a tag filter string (format: key=value)
func parseTagFilter(filter string) (string, string) {
	for i, c := range filter {
		if c == '=' {
			return filter[:i], filter[i+1:]
		}
	}
	return filter, ""
}

