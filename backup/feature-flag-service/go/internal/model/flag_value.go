package model

import (
	"encoding/json"
	"fmt"
)

// FlagValue represents a feature flag value with its type
type FlagValue struct {
	Type  ValueType   `json:"type"`
	Value interface{} `json:"value"`
}

// NewBooleanValue creates a new boolean flag value
func NewBooleanValue(value bool) *FlagValue {
	return &FlagValue{
		Type:  ValueTypeBoolean,
		Value: value,
	}
}

// NewStringValue creates a new string flag value
func NewStringValue(value string) *FlagValue {
	return &FlagValue{
		Type:  ValueTypeString,
		Value: value,
	}
}

// NewIntegerValue creates a new integer flag value
func NewIntegerValue(value int64) *FlagValue {
	return &FlagValue{
		Type:  ValueTypeInteger,
		Value: value,
	}
}

// NewDoubleValue creates a new double flag value
func NewDoubleValue(value float64) *FlagValue {
	return &FlagValue{
		Type:  ValueTypeDouble,
		Value: value,
	}
}

// NewJSONValue creates a new JSON flag value
func NewJSONValue(value string) *FlagValue {
	return &FlagValue{
		Type:  ValueTypeJSON,
		Value: value,
	}
}

// GetBooleanValue returns the boolean value
func (v *FlagValue) GetBooleanValue() (bool, error) {
	if v.Type != ValueTypeBoolean {
		return false, fmt.Errorf("flag value is not a boolean")
	}
	
	// Handle different types of boolean values
	switch val := v.Value.(type) {
	case bool:
		return val, nil
	case string:
		if val == "true" {
			return true, nil
		} else if val == "false" {
			return false, nil
		}
		return false, fmt.Errorf("invalid boolean string: %s", val)
	default:
		return false, fmt.Errorf("cannot convert %T to boolean", v.Value)
	}
}

// GetStringValue returns the string value
func (v *FlagValue) GetStringValue() (string, error) {
	if v.Type != ValueTypeString {
		return "", fmt.Errorf("flag value is not a string")
	}
	
	// Handle different types of string values
	switch val := v.Value.(type) {
	case string:
		return val, nil
	default:
		return fmt.Sprintf("%v", val), nil
	}
}

// GetIntegerValue returns the integer value
func (v *FlagValue) GetIntegerValue() (int64, error) {
	if v.Type != ValueTypeInteger {
		return 0, fmt.Errorf("flag value is not an integer")
	}
	
	// Handle different types of integer values
	switch val := v.Value.(type) {
	case int:
		return int64(val), nil
	case int32:
		return int64(val), nil
	case int64:
		return val, nil
	case float64:
		return int64(val), nil
	case string:
		var i int64
		if _, err := fmt.Sscanf(val, "%d", &i); err != nil {
			return 0, fmt.Errorf("cannot convert string to integer: %v", err)
		}
		return i, nil
	default:
		return 0, fmt.Errorf("cannot convert %T to integer", v.Value)
	}
}

// GetDoubleValue returns the double value
func (v *FlagValue) GetDoubleValue() (float64, error) {
	if v.Type != ValueTypeDouble {
		return 0, fmt.Errorf("flag value is not a double")
	}
	
	// Handle different types of double values
	switch val := v.Value.(type) {
	case float32:
		return float64(val), nil
	case float64:
		return val, nil
	case int:
		return float64(val), nil
	case int64:
		return float64(val), nil
	case string:
		var d float64
		if _, err := fmt.Sscanf(val, "%f", &d); err != nil {
			return 0, fmt.Errorf("cannot convert string to double: %v", err)
		}
		return d, nil
	default:
		return 0, fmt.Errorf("cannot convert %T to double", v.Value)
	}
}

// GetJSONValue returns the JSON value
func (v *FlagValue) GetJSONValue() (string, error) {
	if v.Type != ValueTypeJSON {
		return "", fmt.Errorf("flag value is not a JSON")
	}
	
	// Handle different types of JSON values
	switch val := v.Value.(type) {
	case string:
		// Validate that it's valid JSON
		var js interface{}
		if err := json.Unmarshal([]byte(val), &js); err != nil {
			return "", fmt.Errorf("invalid JSON string: %v", err)
		}
		return val, nil
	case map[string]interface{}, []interface{}:
		// Convert to JSON string
		bytes, err := json.Marshal(val)
		if err != nil {
			return "", fmt.Errorf("cannot convert to JSON: %v", err)
		}
		return string(bytes), nil
	default:
		return "", fmt.Errorf("cannot convert %T to JSON", v.Value)
	}
}

// MarshalJSON implements the json.Marshaler interface
func (v *FlagValue) MarshalJSON() ([]byte, error) {
	return json.Marshal(struct {
		Type  ValueType   `json:"type"`
		Value interface{} `json:"value"`
	}{
		Type:  v.Type,
		Value: v.Value,
	})
}

// UnmarshalJSON implements the json.Unmarshaler interface
func (v *FlagValue) UnmarshalJSON(data []byte) error {
	var raw struct {
		Type  ValueType        `json:"type"`
		Value json.RawMessage  `json:"value"`
	}
	
	if err := json.Unmarshal(data, &raw); err != nil {
		return err
	}
	
	v.Type = raw.Type
	
	// Unmarshal the value based on the type
	switch raw.Type {
	case ValueTypeBoolean:
		var val bool
		if err := json.Unmarshal(raw.Value, &val); err != nil {
			return err
		}
		v.Value = val
	case ValueTypeString:
		var val string
		if err := json.Unmarshal(raw.Value, &val); err != nil {
			return err
		}
		v.Value = val
	case ValueTypeInteger:
		var val int64
		if err := json.Unmarshal(raw.Value, &val); err != nil {
			return err
		}
		v.Value = val
	case ValueTypeDouble:
		var val float64
		if err := json.Unmarshal(raw.Value, &val); err != nil {
			return err
		}
		v.Value = val
	case ValueTypeJSON:
		var val string
		if err := json.Unmarshal(raw.Value, &val); err != nil {
			return err
		}
		v.Value = val
	default:
		return fmt.Errorf("unknown value type: %s", raw.Type)
	}
	
	return nil
}

