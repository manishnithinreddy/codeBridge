package config

import (
	"fmt"
	"time"

	"github.com/spf13/viper"
)

// Config holds all configuration for the service
type Config struct {
	Server ServerConfig
	Redis  RedisConfig
	JWT    JWTConfig
}

// ServerConfig holds HTTP server configuration
type ServerConfig struct {
	Port         int
	ReadTimeout  time.Duration
	WriteTimeout time.Duration
	IdleTimeout  time.Duration
}

// RedisConfig holds Redis connection configuration
type RedisConfig struct {
	Host     string
	Port     int
	Password string
	DB       int
	PoolSize int
}

// JWTConfig holds JWT configuration
type JWTConfig struct {
	Secret           string
	ExpirationHours  int
	RefreshSecret    string
	RefreshExpHours  int
	Issuer           string
	AccessTokenName  string
	RefreshTokenName string
}

// LoadConfig loads configuration from file and environment variables
func LoadConfig() (*Config, error) {
	viper.SetConfigName("config")
	viper.SetConfigType("yaml")
	viper.AddConfigPath(".")
	viper.AddConfigPath("./config")
	viper.AddConfigPath("/etc/codebridge/session-service")

	// Set defaults
	setDefaults()

	// Read config file
	if err := viper.ReadInConfig(); err != nil {
		if _, ok := err.(viper.ConfigFileNotFoundError); !ok {
			return nil, fmt.Errorf("error reading config file: %w", err)
		}
		// Config file not found, will use defaults and env vars
	}

	// Override with environment variables
	viper.SetEnvPrefix("SESSION")
	viper.AutomaticEnv()

	var config Config
	if err := viper.Unmarshal(&config); err != nil {
		return nil, fmt.Errorf("unable to decode config into struct: %w", err)
	}

	// Validate config
	if err := validateConfig(&config); err != nil {
		return nil, err
	}

	return &config, nil
}

// setDefaults sets default values for configuration
func setDefaults() {
	// Server defaults
	viper.SetDefault("server.port", 8080)
	viper.SetDefault("server.readTimeout", 10*time.Second)
	viper.SetDefault("server.writeTimeout", 10*time.Second)
	viper.SetDefault("server.idleTimeout", 60*time.Second)

	// Redis defaults
	viper.SetDefault("redis.host", "localhost")
	viper.SetDefault("redis.port", 6379)
	viper.SetDefault("redis.password", "")
	viper.SetDefault("redis.db", 0)
	viper.SetDefault("redis.poolSize", 10)

	// JWT defaults
	viper.SetDefault("jwt.expirationHours", 24)
	viper.SetDefault("jwt.refreshExpHours", 168) // 7 days
	viper.SetDefault("jwt.issuer", "codebridge-session-service")
	viper.SetDefault("jwt.accessTokenName", "access_token")
	viper.SetDefault("jwt.refreshTokenName", "refresh_token")
}

// validateConfig validates the configuration
func validateConfig(config *Config) error {
	if config.JWT.Secret == "" {
		return fmt.Errorf("JWT secret is required")
	}
	if config.JWT.RefreshSecret == "" {
		return fmt.Errorf("JWT refresh secret is required")
	}
	return nil
}

