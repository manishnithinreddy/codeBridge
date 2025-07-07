package main

import (
	"context"
	"fmt"
	
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/codebridge/db-service/internal/api"
	"github.com/codebridge/db-service/internal/config"
	"github.com/codebridge/db-service/internal/database"
	"github.com/codebridge/db-service/internal/service"
	"github.com/codebridge/db-service/pkg/logging"
)

func main() {
	// Create logger
	logger := logging.NewLogger()
	logger.Info("Starting DB service...")

	// Load configuration
	cfg, err := config.GetConfig()
	if err != nil {
		logger.Error("Failed to load configuration", "error", err)
		os.Exit(1)
	}

	// Create database manager
	dbManager, err := database.NewManager(cfg.Database, logger)
	if err != nil {
		logger.Error("Failed to create database manager", "error", err)
		os.Exit(1)
	}
	defer dbManager.Close()

	// Create database service
	dbService := service.NewDBService(dbManager, cfg.SessionService, logger)

	// Create HTTP router
	router := api.NewRouter(dbService, cfg, logger)

	// Create HTTP server
	server := &http.Server{
		Addr:         fmt.Sprintf(":%d", cfg.Server.Port),
		Handler:      router,
		ReadTimeout:  cfg.Server.ReadTimeout,
		WriteTimeout: cfg.Server.WriteTimeout,
		IdleTimeout:  cfg.Server.IdleTimeout,
	}

	// Start HTTP server in a goroutine
	go func() {
		logger.Info("Starting HTTP server", "port", cfg.Server.Port)
		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			logger.Error("Failed to start HTTP server", "error", err)
			os.Exit(1)
		}
	}()

	// Wait for interrupt signal to gracefully shutdown the server
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	logger.Info("Shutting down DB service...")

	// Create a deadline for the shutdown
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	// Shutdown HTTP server
	if err := server.Shutdown(ctx); err != nil {
		logger.Error("Failed to shutdown HTTP server", "error", err)
	}

	logger.Info("DB service stopped")
}

