#!/bin/bash

set -e

echo "🔧 Building native libraries for all platforms..."

# Create directories
mkdir -p ../lib/{windows/amd64,macos/{amd64,arm64},linux/amd64,current}

# Build for current platform (for development/testing)
echo "Building for current platform..."
go build -buildmode=c-shared -ldflags="-s -w" -o ../lib/current/libdemojibake.so .

# Cross-compile with CGO for all target platforms
echo "Cross-compiling for Windows amd64..."
GOOS=windows GOARCH=amd64 CGO_ENABLED=1 go build -buildmode=c-shared -ldflags="-s -w" -o ../lib/windows/amd64/demojibake.dll . 2>/dev/null || echo "Windows build requires CGO setup"

echo "Cross-compiling for macOS amd64..."
GOOS=darwin GOARCH=amd64 CGO_ENABLED=1 go build -buildmode=c-shared -ldflags="-s -w" -o ../lib/macos/amd64/libdemojibake.dylib . 2>/dev/null || echo "macOS build requires CGO setup"

echo "Cross-compiling for macOS arm64..."
GOOS=darwin GOARCH=arm64 CGO_ENABLED=1 go build -buildmode=c-shared -ldflags="-s -w" -o ../lib/macos/arm64/libdemojibake.dylib . 2>/dev/null || echo "macOS arm64 build requires CGO setup"

echo "Cross-compiling for Linux amd64..."
GOOS=linux GOARCH=amd64 CGO_ENABLED=1 go build -buildmode=c-shared -ldflags="-s -w" -o ../lib/linux/amd64/libdemojibake.so . 2>/dev/null || echo "Linux build requires CGO setup"

echo "✅ Native libraries build completed"
echo "📊 Library sizes:"
du -h ../lib/*/* 2>/dev/null || echo "No libraries found"