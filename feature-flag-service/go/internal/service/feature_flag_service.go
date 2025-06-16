package service

import (
	"context"

	"github.com/codebridge/feature-flag-service/internal/model"
)

// FeatureFlagService defines the interface for feature flag operations
type FeatureFlagService interface {
	// GetFlag gets a flag by key and namespace
	GetFlag(ctx context.Context, key, namespace string) (*model.FeatureFlag, error)
	
	// GetFlags gets multiple flags by keys and namespace
	GetFlags(ctx context.Context, keys []string, namespace string) ([]*model.FeatureFlag, error)
	
	// SetFlag sets a flag
	SetFlag(ctx context.Context, flag *model.FeatureFlag) error
	
	// DeleteFlag deletes a flag
	DeleteFlag(ctx context.Context, key, namespace string) error
	
	// ListFlags lists flags by namespace and optional prefix
	ListFlags(ctx context.Context, namespace, prefix string) ([]*model.FeatureFlag, error)
	
	// ListFlagsByTag lists flags by tag
	ListFlagsByTag(ctx context.Context, namespace, tagKey, tagValue string) ([]*model.FeatureFlag, error)
	
	// EvaluateFlag evaluates a flag with context
	EvaluateFlag(ctx context.Context, key, namespace string, evalCtx *model.EvaluationContext) (*model.EvaluationResult, error)
	
	// Subscribe subscribes to flag updates
	Subscribe(ctx context.Context, callback func(*model.FeatureFlag)) error
	
	// Unsubscribe unsubscribes from flag updates
	Unsubscribe(ctx context.Context) error
}

