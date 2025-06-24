package main

import (
	"context"
	"fmt"
	"log"
	"net"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/codebridge/feature-flag-service/internal/api"
	"github.com/codebridge/feature-flag-service/internal/config"
	"github.com/codebridge/feature-flag-service/internal/grpc"
	"github.com/codebridge/feature-flag-service/internal/repository"
	"github.com/codebridge/feature-flag-service/internal/service"
	"github.com/gorilla/mux"
	goredis "github.com/go-redis/redis/v8"
	ggrpc "google.golang.org/grpc"
	"google.golang.org/grpc/reflection"
)

func main() {
	// Load configuration
	cfg, err := config.LoadConfig()
	if err != nil {
		log.Fatalf("Failed to load configuration: %v", err)
	}

	// Set up context with cancellation
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	// Initialize Redis client
	redisClient := goredis.NewClient(&goredis.Options{
		Addr:     fmt.Sprintf("%s:%d", cfg.Redis.Host, cfg.Redis.Port),
		Password: cfg.Redis.Password,
		DB:       cfg.Redis.Database,
	})

	// Ping Redis to verify connection
	if _, err := redisClient.Ping(ctx).Result(); err != nil {
		log.Fatalf("Failed to connect to Redis: %v", err)
	}
	log.Println("Connected to Redis successfully")

	// Initialize repository
	repo := repository.NewRedisFeatureFlagRepository(redisClient, cfg.FeatureFlag.Cache.TTLSeconds)

	// Initialize service
	flagService := service.NewFeatureFlagService(repo, cfg.FeatureFlag.Defaults.Namespace)

	// Initialize gRPC server
	grpcServer := ggrpc.NewServer()
	grpcService := grpc.NewFeatureFlagGrpcService(flagService)
	grpc.RegisterFeatureFlagServiceServer(grpcServer, grpcService)
	reflection.Register(grpcServer)

	// Start gRPC server
	grpcListener, err := net.Listen("tcp", fmt.Sprintf(":%d", cfg.GRPC.Server.Port))
	if err != nil {
		log.Fatalf("Failed to listen for gRPC: %v", err)
	}

	go func() {
		log.Printf("Starting gRPC server on port %d", cfg.GRPC.Server.Port)
		if err := grpcServer.Serve(grpcListener); err != nil {
			log.Fatalf("Failed to serve gRPC: %v", err)
		}
	}()

	// Initialize HTTP server
	router := mux.NewRouter()
	apiHandler := api.NewFeatureFlagHandler(flagService)
	apiHandler.RegisterRoutes(router)

	httpServer := &http.Server{
		Addr:    fmt.Sprintf(":%d", cfg.Server.Port),
		Handler: router,
	}

	// Start HTTP server
	go func() {
		log.Printf("Starting HTTP server on port %d", cfg.Server.Port)
		if err := httpServer.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("Failed to serve HTTP: %v", err)
		}
	}()

	// Set up graceful shutdown
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit
	log.Println("Shutting down servers...")

	// Shutdown HTTP server
	shutdownCtx, shutdownCancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer shutdownCancel()

	if err := httpServer.Shutdown(shutdownCtx); err != nil {
		log.Fatalf("HTTP server shutdown failed: %v", err)
	}

	// Shutdown gRPC server
	grpcServer.GracefulStop()

	// Close Redis connection
	if err := redisClient.Close(); err != nil {
		log.Fatalf("Redis connection close failed: %v", err)
	}

	log.Println("Servers exited properly")
}

