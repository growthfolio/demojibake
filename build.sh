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

# Build character analysis engine with optimizations
echo "🔧 Building character analysis engine..."
cd character_analysis_engine
chmod +x build.sh
./build.sh
cd ..

echo "✅ Native libraries built successfully"

# Build desktop workbench application
echo "🎨 Building desktop workbench application..."
cd desktop_workbench

# Clean and compile
mvn clean compile -q

echo "✅ JavaFX application compiled successfully"

# Package application
echo "📦 Packaging application..."
mvn package -q

echo "🎯 Running application..."
# Set library path and run
export LD_LIBRARY_PATH="../native_libraries/current:$LD_LIBRARY_PATH"
mvn javafx:run -Djava.library.path="../native_libraries/current"

echo "🏁 Build completed! Use 'cd desktop_workbench && mvn javafx:run' to run the application."