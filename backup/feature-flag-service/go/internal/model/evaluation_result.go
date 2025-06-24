package model

// EvaluationResult represents the result of evaluating a feature flag
type EvaluationResult struct {
	FlagKey     string          `json:"flagKey"`
	Value       *FlagValue      `json:"value"`
	VariationID string          `json:"variationId,omitempty"`
	RuleID      string          `json:"ruleId,omitempty"`
	Reason      *EvaluationReason `json:"reason,omitempty"`
}

// EvaluationReason represents the reason for the evaluation result
type EvaluationReason struct {
	Type         ReasonType `json:"type"`
	RuleID       string     `json:"ruleId,omitempty"`
	Description  string     `json:"description,omitempty"`
	ErrorMessage string     `json:"errorMessage,omitempty"`
}

// ReasonType represents the type of evaluation reason
type ReasonType string

const (
	// ReasonTypeDefault indicates the default value was used
	ReasonTypeDefault ReasonType = "DEFAULT"
	
	// ReasonTypeRuleMatch indicates a rule matched
	ReasonTypeRuleMatch ReasonType = "RULE_MATCH"
	
	// ReasonTypePrerequisiteFailed indicates a prerequisite failed
	ReasonTypePrerequisiteFailed ReasonType = "PREREQUISITE_FAILED"
	
	// ReasonTypeError indicates an error occurred
	ReasonTypeError ReasonType = "ERROR"
	
	// ReasonTypeDisabled indicates the flag is disabled
	ReasonTypeDisabled ReasonType = "DISABLED"
	
	// ReasonTypeFallthrough indicates the fallthrough value was used
	ReasonTypeFallthrough ReasonType = "FALLTHROUGH"
	
	// ReasonTypeTargetMatch indicates a target match
	ReasonTypeTargetMatch ReasonType = "TARGET_MATCH"
)

// NewEvaluationResult creates a new evaluation result
func NewEvaluationResult(flagKey string, value *FlagValue) *EvaluationResult {
	return &EvaluationResult{
		FlagKey: flagKey,
		Value:   value,
	}
}

// WithVariation sets the variation ID
func (r *EvaluationResult) WithVariation(variationID string) *EvaluationResult {
	r.VariationID = variationID
	return r
}

// WithRule sets the rule ID
func (r *EvaluationResult) WithRule(ruleID string) *EvaluationResult {
	r.RuleID = ruleID
	return r
}

// WithReason sets the evaluation reason
func (r *EvaluationResult) WithReason(reasonType ReasonType, description string) *EvaluationResult {
	r.Reason = &EvaluationReason{
		Type:        reasonType,
		Description: description,
	}
	return r
}

// WithError sets an error reason
func (r *EvaluationResult) WithError(errorMessage string) *EvaluationResult {
	r.Reason = &EvaluationReason{
		Type:         ReasonTypeError,
		ErrorMessage: errorMessage,
	}
	return r
}

// WithRuleMatch sets a rule match reason
func (r *EvaluationResult) WithRuleMatch(ruleID, description string) *EvaluationResult {
	r.Reason = &EvaluationReason{
		Type:        ReasonTypeRuleMatch,
		RuleID:      ruleID,
		Description: description,
	}
	return r
}

