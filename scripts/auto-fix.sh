#!/bin/bash

# Auto-Fix Script for Gasolinera JSM
# This script automatically fixes common code quality issues

set -e

echo "🔧 Starting Auto-Fix for Gasolinera JSM..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if gradlew exists and is executable
if [ ! -f "./gradlew" ]; then
    print_error "gradlew not found in current directory"
    exit 1
fi

if [ ! -x "./gradlew" ]; then
    print_status "Making gradlew executable..."
    chmod +x ./gradlew
fi

echo ""
print_status "=== KOTLIN CODE FORMATTING ==="

# Auto-format Kotlin code with ktlint
print_status "Running ktlint auto-format..."
if ./gradlew ktlintFormat --continue; then
    print_success "Kotlin code formatted successfully"
else
    print_warning "Some formatting issues could not be auto-fixed"
fi

echo ""
print_status "=== IMPORT OPTIMIZATION ==="

# Optimize imports (if available)
print_status "Optimizing imports..."
if ./gradlew optimizeImports --continue 2>/dev/null; then
    print_success "Imports optimized successfully"
else
    print_warning "Import optimization not available or failed"
fi

echo ""
print_status "=== DETEKT AUTO-CORRECTIONS ==="

# Run detekt with auto-correct
print_status "Running detekt auto-correct..."
if ./gradlew detekt --auto-correct --continue; then
    print_success "Detekt auto-corrections applied"
else
    print_warning "Some detekt issues could not be auto-fixed"
fi

echo ""
print_status "=== GRADLE WRAPPER UPDATE ==="

# Update Gradle wrapper if needed
print_status "Checking Gradle wrapper..."
if ./gradlew wrapper --gradle-version=8.8 --continue; then
    print_success "Gradle wrapper updated"
else
    print_warning "Gradle wrapper update failed"
fi

echo ""
print_status "=== DEPENDENCY UPDATES ==="

# Check for dependency updates (if plugin is available)
if ./gradlew dependencyUpdates --continue 2>/dev/null; then
    print_success "Dependency update check completed"
    print_status "Check build/dependencyUpdates/report.txt for available updates"
else
    print_warning "Dependency update check not available"
fi

echo ""
print_status "=== CLEANING BUILD ARTIFACTS ==="

# Clean build artifacts
print_status "Cleaning build artifacts..."
if ./gradlew clean --continue; then
    print_success "Build artifacts cleaned"
else
    print_warning "Clean failed"
fi

echo ""
print_status "=== VERIFICATION ==="

# Run a quick verification
print_status "Running quick verification..."
if ./gradlew ktlintCheck --continue; then
    print_success "Verification passed - code is properly formatted"
else
    print_warning "Verification failed - manual fixes may be needed"
fi

echo ""
print_success "Auto-fix completed! 🎉"
echo ""
print_status "=== NEXT STEPS ==="
echo "1. Review the changes made by auto-fix"
echo "2. Run './scripts/quality-check.sh' to verify all issues are resolved"
echo "3. Commit your changes if everything looks good"
echo ""
print_status "Files that may have been modified:"
echo "  - *.kt files (formatting and style fixes)"
echo "  - gradle/wrapper/ (wrapper updates)"
echo "  - build.gradle.kts files (if dependency updates were applied)"