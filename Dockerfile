# Multi-stage build for Demojibakelizador CLI
FROM golang:1.22-alpine AS builder

WORKDIR /app

# Install build dependencies
RUN apk add --no-cache git

# Copy go mod files
COPY go.mod go.sum ./
RUN go mod download

# Copy source code
COPY . .

# Build CLI binary
RUN CGO_ENABLED=0 GOOS=linux go build -ldflags "-s -w" -o demojibake ./cmd/demojibake

# Final stage
FROM alpine:latest

# Install ca-certificates for HTTPS
RUN apk --no-cache add ca-certificates

WORKDIR /root/

# Copy binary from builder
COPY --from=builder /app/demojibake .

# Set executable permissions
RUN chmod +x demojibake

# Default entrypoint
ENTRYPOINT ["./demojibake"]

# Default command shows help
CMD ["-h"]