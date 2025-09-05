#!/bin/bash

# Install Git pre-commit hook for encoding validation

HOOK_DIR=".git/hooks"
HOOK_FILE="$HOOK_DIR/pre-commit"

# Create hooks directory if it doesn't exist
mkdir -p "$HOOK_DIR"

# Create pre-commit hook
cat > "$HOOK_FILE" << 'EOF'
#!/bin/bash

# Pre-commit hook to check for non-UTF-8 files
# Uses demojibake CLI to detect encoding issues

# Find demojibake binary
DEMOJIBAKE=""
if [ -f "./demojibake" ]; then
    DEMOJIBAKE="./demojibake"
elif [ -f "./dist/demojibake" ]; then
    DEMOJIBAKE="./dist/demojibake"
elif command -v demojibake >/dev/null 2>&1; then
    DEMOJIBAKE="demojibake"
else
    echo "Warning: demojibake binary not found, skipping encoding check"
    exit 0
fi

# Get list of staged files
STAGED_FILES=$(git diff --cached --name-only --diff-filter=ACM)

if [ -z "$STAGED_FILES" ]; then
    exit 0
fi

# Create temporary file with staged files
TEMP_FILE=$(mktemp)
echo "$STAGED_FILES" > "$TEMP_FILE"

# Check encoding of staged files
echo "Checking encoding of staged files..."
if ! $DEMOJIBAKE -path "$TEMP_FILE" -detect -fail-if-not-utf8 -ext ".txt,.md,.java,.xml,.properties,.csv,.html,.js,.ts,.go,.py,.rb,.php,.c,.cpp,.h,.hpp" 2>/dev/null; then
    echo ""
    echo "❌ Commit blocked: Non-UTF-8 files detected in staged changes"
    echo "Run 'demojibake -path . -in-place -backup-suffix .bak' to fix encoding issues"
    echo "Or use 'git commit --no-verify' to bypass this check"
    rm -f "$TEMP_FILE"
    exit 1
fi

rm -f "$TEMP_FILE"
echo "✅ All staged files are UTF-8 encoded"
exit 0
EOF

# Make hook executable
chmod +x "$HOOK_FILE"

echo "✅ Pre-commit hook installed successfully"
echo "The hook will check for non-UTF-8 files before each commit"
echo "To bypass the check, use: git commit --no-verify"