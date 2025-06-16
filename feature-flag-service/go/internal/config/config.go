package config

import (
	"github.com/spf13/viper"
)

// Config represents the application configuration
type Config struct {
	Server struct {
		Port        int    `mapstructure:"port"`
		ContextPath string `mapstructure:"context-path"`
	} `mapstructure:"server"`

	Redis struct {
		Host     string `mapstructure:"host"`
		Port     int    `mapstructure:"port"`
		Password string `mapstructure:"password"`
		Database int    `mapstructure:"database"`
		Timeout  int    `mapstructure:"timeout"`
		Pool     struct {
			MaxActive int `mapstructure:"max-active"`
			MaxIdle   int `mapstructure:"max-idle"`
			MinIdle   int `mapstructure:"min-idle"`
			MaxWait   int `mapstructure:"max-wait"`
		} `mapstructure:"pool"`
	} `mapstructure:"redis"`

	GRPC struct {
		Server struct {
			Port int `mapstructure:"port"`
		} `mapstructure:"server"`
	} `mapstructure:"grpc"`

	FeatureFlag struct {
		Cache struct {
			TTLSeconds int `mapstructure:"ttl-seconds"`
		} `mapstructure:"cache"`
		Defaults struct {
			Namespace string `mapstructure:"namespace"`
		} `mapstructure:"defaults"`
		PubSub struct {
			Channel string `mapstructure:"channel"`
		} `mapstructure:"pubsub"`
	} `mapstructure:"feature-flag"`
}

// LoadConfig loads the configuration from environment variables and config files
func LoadConfig() (*Config, error) {
	viper.SetConfigName("application")
	viper.SetConfigType("yaml")
	viper.AddConfigPath(".")
	viper.AddConfigPath("./config")
	viper.AddConfigPath("/etc/feature-flag-service")

	// Set default values
	viper.SetDefault("server.port", 8090)
	viper.SetDefault("server.context-path", "/feature-flag")
	viper.SetDefault("redis.host", "localhost")
	viper.SetDefault("redis.port", 6379)
	viper.SetDefault("redis.database", 0)
	viper.SetDefault("redis.timeout", 2000)
	viper.SetDefault("redis.pool.max-active", 8)
	viper.SetDefault("redis.pool.max-idle", 8)
	viper.SetDefault("redis.pool.min-idle", 2)
	viper.SetDefault("redis.pool.max-wait", -1)
	viper.SetDefault("grpc.server.port", 9090)
	viper.SetDefault("feature-flag.cache.ttl-seconds", 60)
	viper.SetDefault("feature-flag.defaults.namespace", "default")
	viper.SetDefault("feature-flag.pubsub.channel", "feature-flag-updates")

	// Enable environment variable overrides
	viper.AutomaticEnv()
	viper.SetEnvPrefix("FF")

	// Read configuration
	if err := viper.ReadInConfig(); err != nil {
		// It's okay if config file is not found, we'll use defaults and env vars
		if _, ok := err.(viper.ConfigFileNotFoundError); !ok {
			return nil, err
		}
	}

	var config Config
	if err := viper.Unmarshal(&config); err != nil {
		return nil, err
	}

	return &config, nil
}

