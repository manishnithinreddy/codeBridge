FROM golang:1.21-alpine AS builder

WORKDIR /app

# Copy go.mod and go.sum
COPY go.mod ./
COPY go.sum ./

# Download dependencies
RUN go mod download

# Copy source code
COPY . .

# Build the application
RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o feature-flag-service ./cmd/server

# Use a minimal alpine image
FROM alpine:3.18

WORKDIR /app

# Copy the binary from the builder stage
COPY --from=builder /app/feature-flag-service .
COPY --from=builder /app/config ./config

# Expose ports
EXPOSE 8090
EXPOSE 9090

# Run the application
CMD ["./feature-flag-service"]

