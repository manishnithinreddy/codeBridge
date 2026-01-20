package grpc

import (
	"context"
	"fmt"
	"time"

	"github.com/codebridge/feature-flag-service/internal/model"
	"github.com/codebridge/feature-flag-service/internal/service"
)

// FeatureFlagGrpcService implements the gRPC service for feature flags
type FeatureFlagGrpcService struct {
	UnimplementedFeatureFlagServiceServer
	service service.FeatureFlagService
}

// NewFeatureFlagGrpcService creates a new gRPC service for feature flags
func NewFeatureFlagGrpcService(service service.FeatureFlagService) *FeatureFlagGrpcService {
	return &FeatureFlagGrpcService{
		service: service,
	}
}

// GetFlag gets a flag by key
func (s *FeatureFlagGrpcService) GetFlag(ctx context.Context, req *FlagRequest) (*FlagResponse, error) {
	// Get the flag from the service
	flag, err := s.service.GetFlag(ctx, req.FlagKey, req.Namespace)
	if err != nil {
		return nil, fmt.Errorf("failed to get flag: %w", err)
	}

	// If flag not found, return empty response
	if flag == nil {
		return &FlagResponse{
			FlagKey: req.FlagKey,
			Exists:  false,
		}, nil
	}

	// Convert to gRPC response
	response := &FlagResponse{
		FlagKey: flag.Key,
		Exists:  true,
		Metadata: &FlagMetadata{
			Description: flag.Description,
			CreatedAt:   flag.CreatedAt.UnixMilli(),
			UpdatedAt:   flag.UpdatedAt.UnixMilli(),
			CreatedBy:   flag.CreatedBy,
			UpdatedBy:   flag.UpdatedBy,
			Version:     flag.Version,
			Tags:        flag.Tags,
		},
	}

	// Set the value based on the type
	if flag.Value != nil {
		switch flag.Value.Type {
		case model.ValueTypeBoolean:
			boolVal, _ := flag.Value.GetBooleanValue()
			response.Value = boolVal
		case model.ValueTypeString:
			strVal, _ := flag.Value.GetStringValue()
			response.Value = strVal
		case model.ValueTypeInteger:
			intVal, _ := flag.Value.GetIntegerValue()
			response.Value = intVal
		case model.ValueTypeDouble:
			doubleVal, _ := flag.Value.GetDoubleValue()
			response.Value = doubleVal
		case model.ValueTypeJSON:
			jsonVal, _ := flag.Value.GetJSONValue()
			response.Value = jsonVal
		}
	}

	return response, nil
}

// GetFlags gets multiple flags by keys
func (s *FeatureFlagGrpcService) GetFlags(ctx context.Context, req *MultiFlagRequest) (*MultiFlagResponse, error) {
	// Get the flags from the service
	flags, err := s.service.GetFlags(ctx, req.FlagKeys, req.Namespace)
	if err != nil {
		return nil, fmt.Errorf("failed to get flags: %w", err)
	}

	// Convert to gRPC response
	responses := make([]*FlagResponse, 0, len(flags))
	for _, flag := range flags {
		response := &FlagResponse{
			FlagKey: flag.Key,
			Exists:  true,
			Metadata: &FlagMetadata{
				Description: flag.Description,
				CreatedAt:   flag.CreatedAt.UnixMilli(),
				UpdatedAt:   flag.UpdatedAt.UnixMilli(),
				CreatedBy:   flag.CreatedBy,
				UpdatedBy:   flag.UpdatedBy,
				Version:     flag.Version,
				Tags:        flag.Tags,
			},
		}

		// Set the value based on the type
		if flag.Value != nil {
			switch flag.Value.Type {
			case model.ValueTypeBoolean:
				boolVal, _ := flag.Value.GetBooleanValue()
				response.Value = boolVal
			case model.ValueTypeString:
				strVal, _ := flag.Value.GetStringValue()
				response.Value = strVal
			case model.ValueTypeInteger:
				intVal, _ := flag.Value.GetIntegerValue()
				response.Value = intVal
			case model.ValueTypeDouble:
				doubleVal, _ := flag.Value.GetDoubleValue()
				response.Value = doubleVal
			case model.ValueTypeJSON:
				jsonVal, _ := flag.Value.GetJSONValue()
				response.Value = jsonVal
			}
		}

		responses = append(responses, response)
	}

	return &MultiFlagResponse{
		Flags: responses,
	}, nil
}

// EvaluateFlag evaluates a flag with context
func (s *FeatureFlagGrpcService) EvaluateFlag(ctx context.Context, req *FlagEvaluationRequest) (*FlagEvaluationResponse, error) {
	// Convert gRPC context to model context
	evalCtx := &model.EvaluationContext{
		UserID:            req.Context.UserId,
		SessionID:         req.Context.SessionId,
		Attributes:        req.Context.Attributes,
		NumericAttributes: req.Context.NumericAttributes,
		BooleanAttributes: req.Context.BooleanAttributes,
	}

	if req.Context.ServiceContext != nil {
		evalCtx.ServiceContext = &model.ServiceContext{
			ServiceName:    req.Context.ServiceContext.ServiceName,
			ServiceVersion: req.Context.ServiceContext.ServiceVersion,
			InstanceID:     req.Context.ServiceContext.InstanceId,
			Environment:    req.Context.ServiceContext.Environment,
			Metrics:        req.Context.ServiceContext.Metrics,
		}
	}

	// Evaluate the flag
	result, err := s.service.EvaluateFlag(ctx, req.FlagKey, req.Namespace, evalCtx)
	if err != nil {
		return nil, fmt.Errorf("failed to evaluate flag: %w", err)
	}

	// Convert to gRPC response
	response := &FlagEvaluationResponse{
		FlagKey:     result.FlagKey,
		VariationId: result.VariationID,
		RuleId:      result.RuleID,
	}

	// Set the reason
	if result.Reason != nil {
		response.Reason = &EvaluationReason{
			Type:         string(result.Reason.Type),
			RuleId:       result.Reason.RuleID,
			Description:  result.Reason.Description,
			ErrorMessage: result.Reason.ErrorMessage,
		}
	}

	// Set the value based on the type
	if result.Value != nil {
		switch result.Value.Type {
		case model.ValueTypeBoolean:
			boolVal, _ := result.Value.GetBooleanValue()
			response.Value = boolVal
		case model.ValueTypeString:
			strVal, _ := result.Value.GetStringValue()
			response.Value = strVal
		case model.ValueTypeInteger:
			intVal, _ := result.Value.GetIntegerValue()
			response.Value = intVal
		case model.ValueTypeDouble:
			doubleVal, _ := result.Value.GetDoubleValue()
			response.Value = doubleVal
		case model.ValueTypeJSON:
			jsonVal, _ := result.Value.GetJSONValue()
			response.Value = jsonVal
		}
	}

	return response, nil
}

// SetFlag sets a flag
func (s *FeatureFlagGrpcService) SetFlag(ctx context.Context, req *SetFlagRequest) (*SetFlagResponse, error) {
	// Create a new flag
	flag := &model.FeatureFlag{
		Key:         req.FlagKey,
		Namespace:   req.Namespace,
		Description: req.Description,
		Tags:        req.Tags,
		Temporary:   req.Temporary,
		Version:     req.Version,
	}

	// Set expiration time if provided
	if req.ExpirationTime > 0 {
		expiresAt := time.UnixMilli(req.ExpirationTime)
		flag.ExpiresAt = &expiresAt
	}

	// Set the value based on the type
	switch v := req.Value.(type) {
	case bool:
		flag.Value = model.NewBooleanValue(v)
	case string:
		flag.Value = model.NewStringValue(v)
	case int64:
		flag.Value = model.NewIntegerValue(v)
	case float64:
		flag.Value = model.NewDoubleValue(v)
	case map[string]interface{}:
		// Convert to JSON
		flag.Value = model.NewJSONValue(fmt.Sprintf("%v", v))
	default:
		return nil, fmt.Errorf("unsupported value type: %T", req.Value)
	}

	// Set the flag
	if err := s.service.SetFlag(ctx, flag); err != nil {
		return nil, fmt.Errorf("failed to set flag: %w", err)
	}

	return &SetFlagResponse{
		Success: true,
		Version: flag.Version,
	}, nil
}

// StreamFlagUpdates streams flag updates
func (s *FeatureFlagGrpcService) StreamFlagUpdates(req *FlagStreamRequest, stream FeatureFlagService_StreamFlagUpdatesServer) error {
	ctx := stream.Context()

	// Create a channel for flag updates
	updateCh := make(chan *model.FeatureFlag, 100)

	// Subscribe to flag updates
	if err := s.service.Subscribe(ctx, func(flag *model.FeatureFlag) {
		// Check if the flag matches the request
		if req.Namespace != "" && flag.Namespace != req.Namespace {
			return
		}

		if len(req.FlagKeys) > 0 {
			found := false
			for _, key := range req.FlagKeys {
				if key == flag.Key {
					found = true
					break
				}
			}
			if !found {
				return
			}
		}

		// Send the flag update
		select {
		case updateCh <- flag:
			// Update sent
		default:
			// Channel full, drop the update
		}
	}); err != nil {
		return fmt.Errorf("failed to subscribe to flag updates: %w", err)
	}

	// Unsubscribe when done
	defer s.service.Unsubscribe(ctx)

	// Stream updates
	for {
		select {
		case <-ctx.Done():
			return ctx.Err()
		case flag := <-updateCh:
			// Create the update event
			event := &FlagUpdateEvent{
				EventType: "UPDATED",
				FlagKey:   flag.Key,
				Namespace: flag.Namespace,
				Version:   flag.Version,
				Timestamp: time.Now().UnixMilli(),
			}

			// Set the value based on the type
			if flag.Value != nil {
				switch flag.Value.Type {
				case model.ValueTypeBoolean:
					boolVal, _ := flag.Value.GetBooleanValue()
					event.Value = boolVal
				case model.ValueTypeString:
					strVal, _ := flag.Value.GetStringValue()
					event.Value = strVal
				case model.ValueTypeInteger:
					intVal, _ := flag.Value.GetIntegerValue()
					event.Value = intVal
				case model.ValueTypeDouble:
					doubleVal, _ := flag.Value.GetDoubleValue()
					event.Value = doubleVal
				case model.ValueTypeJSON:
					jsonVal, _ := flag.Value.GetJSONValue()
					event.Value = jsonVal
				}
			} else {
				// No value means the flag was deleted
				event.EventType = "DELETED"
			}

			// Send the event
			if err := stream.Send(event); err != nil {
				return fmt.Errorf("failed to send flag update: %w", err)
			}
		}
	}
}

// ListFlags lists flags
func (s *FeatureFlagGrpcService) ListFlags(ctx context.Context, req *ListFlagsRequest) (*ListFlagsResponse, error) {
	// List flags from the service
	var flags []*model.FeatureFlag
	var err error

	if req.TagFilter != "" {
		// Parse tag filter (format: key=value)
		tagKey, tagValue := parseTagFilter(req.TagFilter)
		flags, err = s.service.ListFlagsByTag(ctx, req.Namespace, tagKey, tagValue)
	} else {
		flags, err = s.service.ListFlags(ctx, req.Namespace, req.Prefix)
	}

	if err != nil {
		return nil, fmt.Errorf("failed to list flags: %w", err)
	}

	// Convert to gRPC response
	summaries := make([]*FlagSummary, 0, len(flags))
	for _, flag := range flags {
		summary := &FlagSummary{
			FlagKey:     flag.Key,
			Namespace:   flag.Namespace,
			Description: flag.Description,
			UpdatedAt:   flag.UpdatedAt.UnixMilli(),
			Temporary:   flag.Temporary,
			Tags:        flag.Tags,
		}

		if flag.ExpiresAt != nil {
			summary.ExpirationTime = flag.ExpiresAt.UnixMilli()
		}

		if flag.Value != nil {
			summary.ValueType = string(flag.Value.Type)
		}

		summaries = append(summaries, summary)
	}

	return &ListFlagsResponse{
		Flags:      summaries,
		TotalCount: int32(len(summaries)),
	}, nil
}

// HealthCheck performs a health check
func (s *FeatureFlagGrpcService) HealthCheck(ctx context.Context, req *HealthCheckRequest) (*HealthCheckResponse, error) {
	return &HealthCheckResponse{
		Status:  "SERVING",
		Version: "1.0.0",
		Details: map[string]string{
			"service": "feature-flag-service",
			"time":    time.Now().Format(time.RFC3339),
		},
	}, nil
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

// UnimplementedFeatureFlagServiceServer is a placeholder for the generated code
type UnimplementedFeatureFlagServiceServer struct {
}

