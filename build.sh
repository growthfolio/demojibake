#!/bin/bash

set -e

echo "🚀 Building Demojibakelizador Enterprise Desktop..."

# Check prerequisites
echo "📋 Checking prerequisites..."
command -v go >/dev/null 2>&1 || { echo "❌ Go is required but not installed."; exit 1; }
command -v mvn >/dev/null 2>&1 || { echo "❌ Maven is required but not installed."; exit 1; }
command -v java >/dev/null 2>&1 || { echo "❌ Java is required but not installed."; exit 1; }

# Verify Java version
JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt "21" ]; then
    echo "❌ Java 21+ is required. Found: $JAVA_VERSION"
    exit 1
fi

echo "✅ Prerequisites check passed"

# Build Go core with optimizations
echo "🔧 Building native libraries..."
cd core
chmod +x build.sh
./build.sh
cd ..

echo "✅ Native libraries built successfully"

# Build JavaFX application
echo "🎨 Building JavaFX application..."
cd gui

# Clean and compile
mvn clean compile -q

echo "✅ JavaFX application compiled successfully"

# Package application
echo "📦 Packaging application..."
mvn package -q

echo "🎯 Running application..."
# Run with optimized JVM flags
mvn javafx:run \
    -Djava.library.path="../lib/current" \
    -Djavafx.args="--add-opens javafx.controls/javafx.scene.control.skin=ALL-UNNAMED" \
    -Xmx2G -Xms512M \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=20 \
    -Dprism.order=d3d,sw \
    -Djavafx.animation.fullspeed=true

echo "🏁 Build and execution completed!"