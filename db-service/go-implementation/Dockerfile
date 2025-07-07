FROM golang:1.21-alpine AS builder

WORKDIR /app

# Copy go.mod and go.sum files
COPY go.mod go.sum* ./

# Download dependencies
RUN go mod download

# Copy source code
COPY . .

# Build the application
RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o db-service ./cmd/server

# Use a minimal alpine image for the final stage
FROM alpine:3.18

WORKDIR /app

# Install CA certificates for HTTPS
RUN apk --no-cache add ca-certificates

# Copy the binary from the builder stage
COPY --from=builder /app/db-service .

# Copy configuration
COPY --from=builder /app/config ./config

# Expose port
EXPOSE 8081

# Run the application
CMD ["./db-service"]
