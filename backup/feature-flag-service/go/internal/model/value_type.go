package model

// ValueType represents the type of a feature flag value
type ValueType string

const (
	// ValueTypeBoolean represents a boolean value
	ValueTypeBoolean ValueType = "BOOLEAN"
	
	// ValueTypeString represents a string value
	ValueTypeString ValueType = "STRING"
	
	// ValueTypeInteger represents an integer value
	ValueTypeInteger ValueType = "INTEGER"
	
	// ValueTypeDouble represents a double value
	ValueTypeDouble ValueType = "DOUBLE"
	
	// ValueTypeJSON represents a JSON value
	ValueTypeJSON ValueType = "JSON"
)

