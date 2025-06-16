package main

import (
	"context"
	"fmt"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/codebridge/session-service/internal/api"
	"github.com/codebridge/session-service/internal/config"
	"github.com/codebridge/session-service/internal/repository"
	"github.com/codebridge/session-service/internal/service"
	"github.com/codebridge/session-service/pkg/logging"
)

func main() {
	// Initialize logger
	logger := logging.NewLogger()
	logger.Info("Starting session service...")

	// Load configuration
	cfg, err := config.LoadConfig()
	if err != nil {
		logger.Fatal("Failed to load configuration", "error", err)
	}

	// Initialize Redis repository
	repo, err := repository.NewRedisRepository(cfg.Redis)
	if err != nil {
		logger.Fatal("Failed to initialize Redis repository", "error", err)
	}
	defer repo.Close()

	// Initialize session service
	sessionService := service.NewSessionService(repo, cfg.JWT, logger)

	// Initialize HTTP server with routes
	router := api.NewRouter(sessionService, cfg, logger)
	server := &http.Server{
		Addr:    fmt.Sprintf(":%d", cfg.Server.Port),
		Handler: router,
	}

	// Start server in a goroutine
	go func() {
		logger.Info("Starting HTTP server", "port", cfg.Server.Port)
		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			logger.Fatal("Failed to start server", "error", err)
		}
	}()

	// Wait for interrupt signal to gracefully shutdown the server
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	logger.Info("Shutting down server...")

	// Create a deadline for server shutdown
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	// Attempt graceful shutdown
	if err := server.Shutdown(ctx); err != nil {
		logger.Fatal("Server forced to shutdown", "error", err)
	}

	logger.Info("Server exited gracefully")
}

