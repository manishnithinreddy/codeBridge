package logging

import (
	"log/slog"
	"os"
)

// Logger is a wrapper around slog.Logger
type Logger struct {
	*slog.Logger
}

// NewLogger creates a new logger
func NewLogger() *Logger {
	// Create a JSON handler for structured logging
	handler := slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{
		Level: slog.LevelInfo,
	})

	// Create a logger with the handler
	logger := slog.New(handler)

	return &Logger{
		Logger: logger,
	}
}

// WithField adds a field to the logger
func (l *Logger) WithField(key string, value interface{}) *Logger {
	return &Logger{
		Logger: l.Logger.With(key, value),
	}
}

// WithFields adds multiple fields to the logger
func (l *Logger) WithFields(fields map[string]interface{}) *Logger {
	attrs := make([]interface{}, 0, len(fields)*2)
	for k, v := range fields {
		attrs = append(attrs, k, v)
	}
	return &Logger{
		Logger: l.Logger.With(attrs...),
	}
}

// WithError adds an error field to the logger
func (l *Logger) WithError(err error) *Logger {
	return &Logger{
		Logger: l.Logger.With("error", err.Error()),
	}
}

