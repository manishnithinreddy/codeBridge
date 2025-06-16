package model

import (
	"time"
)

// FeatureFlag represents a feature flag
type FeatureFlag struct {
	Key         string            `json:"key"`
	Namespace   string            `json:"namespace"`
	Value       *FlagValue        `json:"value"`
	Description string            `json:"description,omitempty"`
	Tags        map[string]string `json:"tags,omitempty"`
	Temporary   bool              `json:"temporary,omitempty"`
	ExpiresAt   *time.Time        `json:"expiresAt,omitempty"`
	Version     string            `json:"version,omitempty"`
	CreatedAt   time.Time         `json:"createdAt"`
	UpdatedAt   time.Time         `json:"updatedAt"`
	CreatedBy   string            `json:"createdBy,omitempty"`
	UpdatedBy   string            `json:"updatedBy,omitempty"`
	Rules       []*FlagRule       `json:"rules,omitempty"`
}

// NewFeatureFlag creates a new feature flag
func NewFeatureFlag(key, namespace string, value *FlagValue) *FeatureFlag {
	now := time.Now()
	return &FeatureFlag{
		Key:       key,
		Namespace: namespace,
		Value:     value,
		CreatedAt: now,
		UpdatedAt: now,
		Tags:      make(map[string]string),
	}
}

// IsExpired checks if the flag is expired
func (f *FeatureFlag) IsExpired() bool {
	return f.Temporary && f.ExpiresAt != nil && time.Now().After(*f.ExpiresAt)
}

// SetExpiration sets the expiration time for a temporary flag
func (f *FeatureFlag) SetExpiration(duration time.Duration) {
	f.Temporary = true
	expiresAt := time.Now().Add(duration)
	f.ExpiresAt = &expiresAt
}

// AddTag adds a tag to the flag
func (f *FeatureFlag) AddTag(key, value string) {
	if f.Tags == nil {
		f.Tags = make(map[string]string)
	}
	f.Tags[key] = value
}

// RemoveTag removes a tag from the flag
func (f *FeatureFlag) RemoveTag(key string) {
	if f.Tags != nil {
		delete(f.Tags, key)
	}
}

// AddRule adds a rule to the flag
func (f *FeatureFlag) AddRule(rule *FlagRule) {
	if f.Rules == nil {
		f.Rules = make([]*FlagRule, 0)
	}
	f.Rules = append(f.Rules, rule)
}

// RemoveRule removes a rule from the flag
func (f *FeatureFlag) RemoveRule(ruleID string) {
	if f.Rules == nil {
		return
	}
	
	for i, rule := range f.Rules {
		if rule.ID == ruleID {
			f.Rules = append(f.Rules[:i], f.Rules[i+1:]...)
			return
		}
	}
}

