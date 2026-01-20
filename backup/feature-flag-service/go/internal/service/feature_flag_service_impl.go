package service

import (
	"context"
	"fmt"
	"time"

	"github.com/codebridge/feature-flag-service/internal/model"
	"github.com/codebridge/feature-flag-service/internal/repository"
)

// FeatureFlagServiceImpl implements FeatureFlagService
type FeatureFlagServiceImpl struct {
	repo            repository.FeatureFlagRepository
	defaultNamespace string
}

// NewFeatureFlagService creates a new feature flag service
func NewFeatureFlagService(repo repository.FeatureFlagRepository, defaultNamespace string) *FeatureFlagServiceImpl {
	return &FeatureFlagServiceImpl{
		repo:            repo,
		defaultNamespace: defaultNamespace,
	}
}

// GetFlag gets a flag by key and namespace
func (s *FeatureFlagServiceImpl) GetFlag(ctx context.Context, key, namespace string) (*model.FeatureFlag, error) {
	// Use default namespace if not provided
	if namespace == "" {
		namespace = s.defaultNamespace
	}

	return s.repo.GetFlag(ctx, key, namespace)
}

// GetFlags gets multiple flags by keys and namespace
func (s *FeatureFlagServiceImpl) GetFlags(ctx context.Context, keys []string, namespace string) ([]*model.FeatureFlag, error) {
	// Use default namespace if not provided
	if namespace == "" {
		namespace = s.defaultNamespace
	}

	return s.repo.GetFlags(ctx, keys, namespace)
}

// SetFlag sets a flag
func (s *FeatureFlagServiceImpl) SetFlag(ctx context.Context, flag *model.FeatureFlag) error {
	// Use default namespace if not provided
	if flag.Namespace == "" {
		flag.Namespace = s.defaultNamespace
	}

	// Set created time if not set
	if flag.CreatedAt.IsZero() {
		flag.CreatedAt = time.Now()
	}

	// Set updated time
	flag.UpdatedAt = time.Now()

	return s.repo.SetFlag(ctx, flag)
}

// DeleteFlag deletes a flag
func (s *FeatureFlagServiceImpl) DeleteFlag(ctx context.Context, key, namespace string) error {
	// Use default namespace if not provided
	if namespace == "" {
		namespace = s.defaultNamespace
	}

	return s.repo.DeleteFlag(ctx, key, namespace)
}

// ListFlags lists flags by namespace and optional prefix
func (s *FeatureFlagServiceImpl) ListFlags(ctx context.Context, namespace, prefix string) ([]*model.FeatureFlag, error) {
	// Use default namespace if not provided
	if namespace == "" {
		namespace = s.defaultNamespace
	}

	return s.repo.ListFlags(ctx, namespace, prefix)
}

// ListFlagsByTag lists flags by tag
func (s *FeatureFlagServiceImpl) ListFlagsByTag(ctx context.Context, namespace, tagKey, tagValue string) ([]*model.FeatureFlag, error) {
	// Use default namespace if not provided
	if namespace == "" {
		namespace = s.defaultNamespace
	}

	return s.repo.ListFlagsByTag(ctx, namespace, tagKey, tagValue)
}

// EvaluateFlag evaluates a flag with context
func (s *FeatureFlagServiceImpl) EvaluateFlag(ctx context.Context, key, namespace string, evalCtx *model.EvaluationContext) (*model.EvaluationResult, error) {
	// Use default namespace if not provided
	if namespace == "" {
		namespace = s.defaultNamespace
	}

	// Get the flag
	flag, err := s.repo.GetFlag(ctx, key, namespace)
	if err != nil {
		return nil, fmt.Errorf("failed to get flag: %w", err)
	}

	// If flag not found, return default result
	if flag == nil {
		return &model.EvaluationResult{
			FlagKey: key,
			Value:   nil,
			Reason: &model.EvaluationReason{
				Type:        model.ReasonTypeDefault,
				Description: "Flag not found",
			},
		}, nil
	}

	// If flag is expired, return default result
	if flag.IsExpired() {
		return &model.EvaluationResult{
			FlagKey: key,
			Value:   nil,
			Reason: &model.EvaluationReason{
				Type:        model.ReasonTypeDisabled,
				Description: "Flag is expired",
			},
		}, nil
	}

	// If no rules, return the flag value
	if len(flag.Rules) == 0 {
		return &model.EvaluationResult{
			FlagKey: key,
			Value:   flag.Value,
			Reason: &model.EvaluationReason{
				Type:        model.ReasonTypeFallthrough,
				Description: "No rules to evaluate",
			},
		}, nil
	}

	// Convert evaluation context to map
	ctxMap := evalCtx.ToMap()

	// Evaluate rules
	for _, rule := range flag.Rules {
		match, err := rule.Evaluate(ctxMap)
		if err != nil {
			return nil, fmt.Errorf("failed to evaluate rule: %w", err)
		}

		if match {
			// Rule matched, return the rule's value
			return &model.EvaluationResult{
				FlagKey:     key,
				Value:       rule.Value,
				RuleID:      rule.ID,
				VariationID: "", // No variation ID for direct rule match
				Reason: &model.EvaluationReason{
					Type:        model.ReasonTypeRuleMatch,
					RuleID:      rule.ID,
					Description: fmt.Sprintf("Rule '%s' matched", rule.Name),
				},
			}, nil
		}
	}

	// No rules matched, return the flag value
	return &model.EvaluationResult{
		FlagKey: key,
		Value:   flag.Value,
		Reason: &model.EvaluationReason{
			Type:        model.ReasonTypeFallthrough,
			Description: "No rules matched",
		},
	}, nil
}

// Subscribe subscribes to flag updates
func (s *FeatureFlagServiceImpl) Subscribe(ctx context.Context, callback func(*model.FeatureFlag)) error {
	return s.repo.Subscribe(ctx, callback)
}

// Unsubscribe unsubscribes from flag updates
func (s *FeatureFlagServiceImpl) Unsubscribe(ctx context.Context) error {
	return s.repo.Unsubscribe(ctx)
}

