#!/bin/bash

set -e

echo "🔧 Building native libraries for all platforms..."

# Create native library directories
mkdir -p ../native_libraries/{windows/amd64,macos/{amd64,arm64},linux/amd64,current}

# Build for current platform (for development/testing)
echo "Building character encoding engine for current platform..."
go build -buildmode=c-shared -ldflags="-s -w" -o ../native_libraries/current/libcharacter_encoding_engine.so .

# Cross-compile with CGO for all target platforms
echo "Cross-compiling for Windows amd64..."
GOOS=windows GOARCH=amd64 CGO_ENABLED=1 go build -buildmode=c-shared -ldflags="-s -w" -o ../native_libraries/windows/amd64/character_encoding_engine.dll . 2>/dev/null || echo "Windows build requires CGO setup"

echo "Cross-compiling for macOS amd64..."
GOOS=darwin GOARCH=amd64 CGO_ENABLED=1 go build -buildmode=c-shared -ldflags="-s -w" -o ../native_libraries/macos/amd64/libcharacter_encoding_engine.dylib . 2>/dev/null || echo "macOS build requires CGO setup"

echo "Cross-compiling for macOS arm64..."
GOOS=darwin GOARCH=arm64 CGO_ENABLED=1 go build -buildmode=c-shared -ldflags="-s -w" -o ../native_libraries/macos/arm64/libcharacter_encoding_engine.dylib . 2>/dev/null || echo "macOS arm64 build requires CGO setup"

echo "Cross-compiling for Linux amd64..."
GOOS=linux GOARCH=amd64 CGO_ENABLED=1 go build -buildmode=c-shared -ldflags="-s -w" -o ../native_libraries/linux/amd64/libcharacter_encoding_engine.so . 2>/dev/null || echo "Linux build requires CGO setup"

echo "✅ Native libraries build completed"
echo "📊 Native library sizes:"
du -h ../native_libraries/*/* 2>/dev/null || echo "No native libraries found"