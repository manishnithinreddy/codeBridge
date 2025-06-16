package config

import (
	"fmt"
	"os"
	"strings"
	"time"

	"github.com/spf13/viper"
)

// Config represents the application configuration
type Config struct {
	Server         ServerConfig         `mapstructure:"server"`
	Database       DatabaseConfig       `mapstructure:"database"`
	SessionService SessionServiceConfig `mapstructure:"sessionService"`
}

// ServerConfig represents the server configuration
type ServerConfig struct {
	Port         int           `mapstructure:"port"`
	ReadTimeout  time.Duration `mapstructure:"readTimeout"`
	WriteTimeout time.Duration `mapstructure:"writeTimeout"`
	IdleTimeout  time.Duration `mapstructure:"idleTimeout"`
}

// DatabaseConfig represents the database configuration
type DatabaseConfig struct {
	DefaultType  string        `mapstructure:"defaultType"`
	MaxOpenConns int           `mapstructure:"maxOpenConns"`
	MaxIdleConns int           `mapstructure:"maxIdleConns"`
	ConnMaxLife  time.Duration `mapstructure:"connMaxLife"`
	MySQL        MySQLConfig   `mapstructure:"mysql"`
	Postgres     PostgresConfig `mapstructure:"postgres"`
	SQLite       SQLiteConfig   `mapstructure:"sqlite"`
}

// MySQLConfig represents MySQL configuration
type MySQLConfig struct {
	Host     string `mapstructure:"host"`
	Port     int    `mapstructure:"port"`
	User     string `mapstructure:"user"`
	Password string `mapstructure:"password"`
	DBName   string `mapstructure:"dbName"`
	Params   string `mapstructure:"params"`
}

// PostgresConfig represents PostgreSQL configuration
type PostgresConfig struct {
	Host     string `mapstructure:"host"`
	Port     int    `mapstructure:"port"`
	User     string `mapstructure:"user"`
	Password string `mapstructure:"password"`
	DBName   string `mapstructure:"dbName"`
	SSLMode  string `mapstructure:"sslMode"`
}

// SQLiteConfig represents SQLite configuration
type SQLiteConfig struct {
	Path string `mapstructure:"path"`
}

// SessionServiceConfig represents session service configuration
type SessionServiceConfig struct {
	URL string `mapstructure:"url"`
}

// LoadConfig loads the application configuration
func LoadConfig(path string) (*Config, error) {
	// Set default configuration
	viper.SetDefault("server.port", 8081)
	viper.SetDefault("server.readTimeout", "10s")
	viper.SetDefault("server.writeTimeout", "10s")
	viper.SetDefault("server.idleTimeout", "60s")

	viper.SetDefault("database.defaultType", "sqlite")
	viper.SetDefault("database.maxOpenConns", 10)
	viper.SetDefault("database.maxIdleConns", 5)
	viper.SetDefault("database.connMaxLife", "5m")

	viper.SetDefault("database.mysql.host", "localhost")
	viper.SetDefault("database.mysql.port", 3306)
	viper.SetDefault("database.mysql.user", "root")
	viper.SetDefault("database.mysql.password", "")
	viper.SetDefault("database.mysql.dbName", "codebridge")
	viper.SetDefault("database.mysql.params", "parseTime=true&charset=utf8mb4&collation=utf8mb4_unicode_ci")

	viper.SetDefault("database.postgres.host", "localhost")
	viper.SetDefault("database.postgres.port", 5432)
	viper.SetDefault("database.postgres.user", "postgres")
	viper.SetDefault("database.postgres.password", "")
	viper.SetDefault("database.postgres.dbName", "codebridge")
	viper.SetDefault("database.postgres.sslMode", "disable")

	viper.SetDefault("database.sqlite.path", "codebridge.db")

	viper.SetDefault("sessionService.url", "http://localhost:8080/api")

	// Set configuration file
	viper.SetConfigName("config")
	viper.SetConfigType("yaml")
	viper.AddConfigPath(path)
	viper.AddConfigPath(".")

	// Read configuration file
	if err := viper.ReadInConfig(); err != nil {
		if _, ok := err.(viper.ConfigFileNotFoundError); !ok {
			return nil, fmt.Errorf("failed to read config file: %w", err)
		}
	}

	// Override configuration with environment variables
	viper.SetEnvPrefix("DB")
	viper.SetEnvKeyReplacer(strings.NewReplacer(".", "_"))
	viper.AutomaticEnv()

	// Parse configuration
	var config Config
	if err := viper.Unmarshal(&config); err != nil {
		return nil, fmt.Errorf("failed to parse config: %w", err)
	}

	return &config, nil
}

// GetConfig returns the application configuration
func GetConfig() (*Config, error) {
	// Get configuration path from environment variable
	configPath := os.Getenv("CONFIG_PATH")
	if configPath == "" {
		configPath = "config"
	}

	// Load configuration
	return LoadConfig(configPath)
}

