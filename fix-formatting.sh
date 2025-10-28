#!/bin/bash

# Fix Kotlin Formatting Script
# Run this from the project root directory

echo "=== Running ktlintFormat to auto-fix issues ==="
./gradlew ktlintFormat

# Or if you don't have gradlew wrapper:
# gradle ktlintFormat

echo ""
echo "=== Checking if all issues are fixed ==="
./gradlew ktlintCheck

# Or if you don't have gradlew wrapper:
# gradle ktlintCheck

echo ""
echo "=== Done! ==="
echo "If there are still issues, check the output above."
echo "You can also run individual tasks:"
echo "  ./gradlew :app:ktlintFormat          - Format Kotlin source files"
echo "  ./gradlew :app:ktlintKotlinScriptFormat - Format Kotlin script files (.kts)"
echo "  ./gradlew :app:ktlintCheck            - Check formatting"
