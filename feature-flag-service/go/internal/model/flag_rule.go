package model

import (
	"fmt"
	"regexp"
	"strconv"
	"strings"
)

// FlagRule represents a rule for evaluating a feature flag
type FlagRule struct {
	ID          string     `json:"id"`
	Name        string     `json:"name,omitempty"`
	Description string     `json:"description,omitempty"`
	Priority    int        `json:"priority"`
	Conditions  []Condition `json:"conditions"`
	Value       *FlagValue `json:"value"`
	Variations  []Variation `json:"variations,omitempty"`
}

// Condition represents a condition for a flag rule
type Condition struct {
	Attribute string      `json:"attribute"`
	Operator  string      `json:"operator"`
	Value     interface{} `json:"value"`
}

// Variation represents a variation of a flag value
type Variation struct {
	ID          string     `json:"id"`
	Value       *FlagValue `json:"value"`
	Weight      int        `json:"weight"`
	Description string     `json:"description,omitempty"`
}

// Operators
const (
	OperatorEquals              = "equals"
	OperatorNotEquals           = "notEquals"
	OperatorGreaterThan         = "greaterThan"
	OperatorGreaterThanOrEquals = "greaterThanOrEquals"
	OperatorLessThan            = "lessThan"
	OperatorLessThanOrEquals    = "lessThanOrEquals"
	OperatorContains            = "contains"
	OperatorNotContains         = "notContains"
	OperatorStartsWith          = "startsWith"
	OperatorEndsWith            = "endsWith"
	OperatorMatches             = "matches"
	OperatorIn                  = "in"
	OperatorNotIn               = "notIn"
)

// NewFlagRule creates a new flag rule
func NewFlagRule(id, name string, priority int, value *FlagValue) *FlagRule {
	return &FlagRule{
		ID:         id,
		Name:       name,
		Priority:   priority,
		Value:      value,
		Conditions: make([]Condition, 0),
		Variations: make([]Variation, 0),
	}
}

// AddCondition adds a condition to the rule
func (r *FlagRule) AddCondition(attribute, operator string, value interface{}) {
	r.Conditions = append(r.Conditions, Condition{
		Attribute: attribute,
		Operator:  operator,
		Value:     value,
	})
}

// AddVariation adds a variation to the rule
func (r *FlagRule) AddVariation(id string, value *FlagValue, weight int) {
	r.Variations = append(r.Variations, Variation{
		ID:     id,
		Value:  value,
		Weight: weight,
	})
}

// Evaluate evaluates the rule against the given context
func (r *FlagRule) Evaluate(context map[string]interface{}) (bool, error) {
	// If there are no conditions, the rule matches
	if len(r.Conditions) == 0 {
		return true, nil
	}

	// All conditions must match
	for _, condition := range r.Conditions {
		match, err := evaluateCondition(condition, context)
		if err != nil {
			return false, err
		}
		if !match {
			return false, nil
		}
	}

	return true, nil
}

// evaluateCondition evaluates a single condition against the context
func evaluateCondition(condition Condition, context map[string]interface{}) (bool, error) {
	// Get the attribute value from the context
	attrValue, exists := getAttributeValue(condition.Attribute, context)
	if !exists {
		// If the attribute doesn't exist, the condition doesn't match
		return false, nil
	}

	// Evaluate the condition based on the operator
	switch condition.Operator {
	case OperatorEquals:
		return equals(attrValue, condition.Value)
	case OperatorNotEquals:
		eq, err := equals(attrValue, condition.Value)
		return !eq, err
	case OperatorGreaterThan:
		return greaterThan(attrValue, condition.Value)
	case OperatorGreaterThanOrEquals:
		return greaterThanOrEquals(attrValue, condition.Value)
	case OperatorLessThan:
		return lessThan(attrValue, condition.Value)
	case OperatorLessThanOrEquals:
		return lessThanOrEquals(attrValue, condition.Value)
	case OperatorContains:
		return contains(attrValue, condition.Value)
	case OperatorNotContains:
		c, err := contains(attrValue, condition.Value)
		return !c, err
	case OperatorStartsWith:
		return startsWith(attrValue, condition.Value)
	case OperatorEndsWith:
		return endsWith(attrValue, condition.Value)
	case OperatorMatches:
		return matches(attrValue, condition.Value)
	case OperatorIn:
		return in(attrValue, condition.Value)
	case OperatorNotIn:
		i, err := in(attrValue, condition.Value)
		return !i, err
	default:
		return false, fmt.Errorf("unknown operator: %s", condition.Operator)
	}
}

// getAttributeValue gets the value of an attribute from the context
// Supports nested attributes with dot notation (e.g., "user.id")
func getAttributeValue(attribute string, context map[string]interface{}) (interface{}, bool) {
	parts := strings.Split(attribute, ".")
	current := context

	// Navigate through nested objects
	for i, part := range parts {
		if i == len(parts)-1 {
			// Last part, get the value
			val, exists := current[part]
			return val, exists
		}

		// Not the last part, navigate to the next level
		next, exists := current[part]
		if !exists {
			return nil, false
		}

		// Check if the next level is a map
		nextMap, ok := next.(map[string]interface{})
		if !ok {
			return nil, false
		}

		current = nextMap
	}

	return nil, false
}

// equals checks if two values are equal
func equals(a, b interface{}) (bool, error) {
	// Convert to comparable types
	aStr := fmt.Sprintf("%v", a)
	bStr := fmt.Sprintf("%v", b)
	return aStr == bStr, nil
}

// greaterThan checks if a > b
func greaterThan(a, b interface{}) (bool, error) {
	// Try to convert to numbers
	aFloat, aErr := toFloat64(a)
	bFloat, bErr := toFloat64(b)

	if aErr == nil && bErr == nil {
		// Both are numbers
		return aFloat > bFloat, nil
	}

	// Compare as strings
	aStr := fmt.Sprintf("%v", a)
	bStr := fmt.Sprintf("%v", b)
	return aStr > bStr, nil
}

// greaterThanOrEquals checks if a >= b
func greaterThanOrEquals(a, b interface{}) (bool, error) {
	// Try to convert to numbers
	aFloat, aErr := toFloat64(a)
	bFloat, bErr := toFloat64(b)

	if aErr == nil && bErr == nil {
		// Both are numbers
		return aFloat >= bFloat, nil
	}

	// Compare as strings
	aStr := fmt.Sprintf("%v", a)
	bStr := fmt.Sprintf("%v", b)
	return aStr >= bStr, nil
}

// lessThan checks if a < b
func lessThan(a, b interface{}) (bool, error) {
	// Try to convert to numbers
	aFloat, aErr := toFloat64(a)
	bFloat, bErr := toFloat64(b)

	if aErr == nil && bErr == nil {
		// Both are numbers
		return aFloat < bFloat, nil
	}

	// Compare as strings
	aStr := fmt.Sprintf("%v", a)
	bStr := fmt.Sprintf("%v", b)
	return aStr < bStr, nil
}

// lessThanOrEquals checks if a <= b
func lessThanOrEquals(a, b interface{}) (bool, error) {
	// Try to convert to numbers
	aFloat, aErr := toFloat64(a)
	bFloat, bErr := toFloat64(b)

	if aErr == nil && bErr == nil {
		// Both are numbers
		return aFloat <= bFloat, nil
	}

	// Compare as strings
	aStr := fmt.Sprintf("%v", a)
	bStr := fmt.Sprintf("%v", b)
	return aStr <= bStr, nil
}

// contains checks if a contains b
func contains(a, b interface{}) (bool, error) {
	aStr := fmt.Sprintf("%v", a)
	bStr := fmt.Sprintf("%v", b)
	return strings.Contains(aStr, bStr), nil
}

// startsWith checks if a starts with b
func startsWith(a, b interface{}) (bool, error) {
	aStr := fmt.Sprintf("%v", a)
	bStr := fmt.Sprintf("%v", b)
	return strings.HasPrefix(aStr, bStr), nil
}

// endsWith checks if a ends with b
func endsWith(a, b interface{}) (bool, error) {
	aStr := fmt.Sprintf("%v", a)
	bStr := fmt.Sprintf("%v", b)
	return strings.HasSuffix(aStr, bStr), nil
}

// matches checks if a matches the regex pattern b
func matches(a, b interface{}) (bool, error) {
	aStr := fmt.Sprintf("%v", a)
	bStr := fmt.Sprintf("%v", b)

	// Compile the regex pattern
	pattern, err := regexp.Compile(bStr)
	if err != nil {
		return false, fmt.Errorf("invalid regex pattern: %v", err)
	}

	return pattern.MatchString(aStr), nil
}

// in checks if a is in the list b
func in(a, b interface{}) (bool, error) {
	// Convert a to string for comparison
	aStr := fmt.Sprintf("%v", a)

	// Check if b is a slice
	switch bVal := b.(type) {
	case []interface{}:
		for _, item := range bVal {
			itemStr := fmt.Sprintf("%v", item)
			if aStr == itemStr {
				return true, nil
			}
		}
		return false, nil
	case string:
		// Split the string by comma
		items := strings.Split(bVal, ",")
		for _, item := range items {
			if aStr == strings.TrimSpace(item) {
				return true, nil
			}
		}
		return false, nil
	default:
		return false, fmt.Errorf("expected a list for 'in' operator, got %T", b)
	}
}

// toFloat64 converts a value to float64
func toFloat64(val interface{}) (float64, error) {
	switch v := val.(type) {
	case float64:
		return v, nil
	case float32:
		return float64(v), nil
	case int:
		return float64(v), nil
	case int64:
		return float64(v), nil
	case int32:
		return float64(v), nil
	case string:
		return strconv.ParseFloat(v, 64)
	default:
		return 0, fmt.Errorf("cannot convert %T to float64", val)
	}
}

