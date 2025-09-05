# Makefile for Demojibakelizador

BINARY_CLI=demojibake
BINARY_GUI=demojibake-gui
VERSION=1.0.0
BUILD_DIR=dist
PLATFORMS=linux/amd64 linux/arm64 windows/amd64 darwin/amd64 darwin/arm64

.PHONY: build build-all run-cli run-gui lint package clean

# Build for current platform
build:
	@echo "Building for current platform..."
	go build -ldflags "-X main.version=$(VERSION)" -o $(BUILD_DIR)/$(BINARY_CLI) ./cmd/demojibake
	go build -ldflags "-X main.version=$(VERSION)" -o $(BUILD_DIR)/$(BINARY_GUI) ./cmd/demojibake-gui

# Build for all platforms
build-all: clean
	@echo "Building for all platforms..."
	@mkdir -p $(BUILD_DIR)
	@for platform in $(PLATFORMS); do \
		GOOS=$$(echo $$platform | cut -d'/' -f1); \
		GOARCH=$$(echo $$platform | cut -d'/' -f2); \
		CLI_OUTPUT=$(BUILD_DIR)/$(BINARY_CLI)-$$GOOS-$$GOARCH; \
		GUI_OUTPUT=$(BUILD_DIR)/$(BINARY_GUI)-$$GOOS-$$GOARCH; \
		if [ "$$GOOS" = "windows" ]; then \
			CLI_OUTPUT=$$CLI_OUTPUT.exe; \
			GUI_OUTPUT=$$GUI_OUTPUT.exe; \
		fi; \
		echo "Building $$GOOS/$$GOARCH..."; \
		GOOS=$$GOOS GOARCH=$$GOARCH go build -ldflags "-X main.version=$(VERSION)" -o $$CLI_OUTPUT ./cmd/demojibake; \
		GOOS=$$GOOS GOARCH=$$GOARCH go build -ldflags "-X main.version=$(VERSION)" -o $$GUI_OUTPUT ./cmd/demojibake-gui; \
	done

# Run CLI
run-cli:
	go run ./cmd/demojibake $(ARGS)

# Run GUI
run-gui:
	go run ./cmd/demojibake-gui

# Lint and format
lint:
	go vet ./...
	gofmt -s -w .
	go mod tidy

# Package distribution
package: build-all
	@echo "Creating distribution packages..."
	@cp README.md $(BUILD_DIR)/
	@cp LICENSE $(BUILD_DIR)/
	@echo "Distribution ready in $(BUILD_DIR)/"

# Clean build artifacts
clean:
	rm -rf $(BUILD_DIR)

# Install dependencies
deps:
	go mod download
	go mod verify

# Test (placeholder - no unit tests as per requirements)
test:
	@echo "No unit tests implemented as per requirements"

# Docker build
docker-build:
	docker build -t demojibake:$(VERSION) .

# Install hooks
install-hooks:
	./scripts/install_hooks.sh

# Build VS Code extension
build-extension:
	@echo "Building VS Code extension..."
	powershell -ExecutionPolicy Bypass -File build-extension.ps1

# Test VS Code extension
test-extension: build build-extension
	@echo "Testing VS Code extension..."
	powershell -ExecutionPolicy Bypass -File test-extension.ps1

# Package everything (CLI + GUI + Extension)
package-all: build-all build-extension
	@echo "Creating complete distribution..."
	@cp README.md $(BUILD_DIR)/
	@cp LICENSE $(BUILD_DIR)/
	@cp vscode-extension/README.md $(BUILD_DIR)/EXTENSION-README.md
	@echo "Complete distribution ready in $(BUILD_DIR)/"