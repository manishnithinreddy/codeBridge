package model

// EvaluationContext represents the context for evaluating a feature flag
type EvaluationContext struct {
	UserID            string                 `json:"userId,omitempty"`
	SessionID         string                 `json:"sessionId,omitempty"`
	Attributes        map[string]string      `json:"attributes,omitempty"`
	NumericAttributes map[string]float64     `json:"numericAttributes,omitempty"`
	BooleanAttributes map[string]bool        `json:"booleanAttributes,omitempty"`
	ServiceContext    *ServiceContext        `json:"serviceContext,omitempty"`
}

// ServiceContext represents the service context for evaluation
type ServiceContext struct {
	ServiceName    string            `json:"serviceName,omitempty"`
	ServiceVersion string            `json:"serviceVersion,omitempty"`
	InstanceID     string            `json:"instanceId,omitempty"`
	Environment    string            `json:"environment,omitempty"`
	Metrics        map[string]string `json:"metrics,omitempty"`
}

// NewEvaluationContext creates a new evaluation context
func NewEvaluationContext() *EvaluationContext {
	return &EvaluationContext{
		Attributes:        make(map[string]string),
		NumericAttributes: make(map[string]float64),
		BooleanAttributes: make(map[string]bool),
	}
}

// NewServiceContext creates a new service context
func NewServiceContext() *ServiceContext {
	return &ServiceContext{
		Metrics: make(map[string]string),
	}
}

// ToMap converts the evaluation context to a map for rule evaluation
func (c *EvaluationContext) ToMap() map[string]interface{} {
	result := make(map[string]interface{})

	// Add user and session IDs
	if c.UserID != "" {
		result["userId"] = c.UserID
	}
	if c.SessionID != "" {
		result["sessionId"] = c.SessionID
	}

	// Add attributes
	for k, v := range c.Attributes {
		result[k] = v
	}

	// Add numeric attributes
	for k, v := range c.NumericAttributes {
		result[k] = v
	}

	// Add boolean attributes
	for k, v := range c.BooleanAttributes {
		result[k] = v
	}

	// Add service context
	if c.ServiceContext != nil {
		service := make(map[string]interface{})
		
		if c.ServiceContext.ServiceName != "" {
			service["name"] = c.ServiceContext.ServiceName
		}
		if c.ServiceContext.ServiceVersion != "" {
			service["version"] = c.ServiceContext.ServiceVersion
		}
		if c.ServiceContext.InstanceID != "" {
			service["instanceId"] = c.ServiceContext.InstanceID
		}
		if c.ServiceContext.Environment != "" {
			service["environment"] = c.ServiceContext.Environment
		}
		
		// Add metrics
		metrics := make(map[string]interface{})
		for k, v := range c.ServiceContext.Metrics {
			metrics[k] = v
		}
		
		if len(metrics) > 0 {
			service["metrics"] = metrics
		}
		
		result["service"] = service
	}

	return result
}

// SetAttribute sets a string attribute
func (c *EvaluationContext) SetAttribute(key, value string) {
	c.Attributes[key] = value
}

// SetNumericAttribute sets a numeric attribute
func (c *EvaluationContext) SetNumericAttribute(key string, value float64) {
	c.NumericAttributes[key] = value
}

// SetBooleanAttribute sets a boolean attribute
func (c *EvaluationContext) SetBooleanAttribute(key string, value bool) {
	c.BooleanAttributes[key] = value
}

// SetServiceContext sets the service context
func (c *EvaluationContext) SetServiceContext(serviceContext *ServiceContext) {
	c.ServiceContext = serviceContext
}

// SetServiceMetric sets a service metric
func (s *ServiceContext) SetMetric(key, value string) {
	s.Metrics[key] = value
}

