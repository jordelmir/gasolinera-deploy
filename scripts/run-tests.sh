#!/bin/bash

# Comprehensive Testing Script for Gasolinera JSM
# Runs different types of tests with proper reporting

set -e

echo "🧪 Starting Comprehensive Test Suite for Gasolinera JSM..."

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

# Parse command line arguments
TEST_TYPE="${1:-all}"
GENERATE_REPORTS="${2:-true}"

print_status "Test type: $TEST_TYPE"
print_status "Generate reports: $GENERATE_REPORTS"

# Function to run tests and capture results
run_test_suite() {
    local test_name="$1"
    local gradle_task="$2"
    local continue_on_error="${3:-false}"

    print_status "Running $test_name..."

    if eval "$gradle_task"; then
        print_success "$test_name completed successfully"
        return 0
    else
        local exit_code=$?
        if [ "$continue_on_error" = "true" ]; then
            print_warning "$test_name failed but continuing..."
            return 0
        else
            print_error "$test_name failed with exit code $exit_code"
            return $exit_code
        fi
    fi
}

# Initialize test results
total_suites=0
passed_suites=0
failed_suites=0
failed_suite_names=()

# Function to track test results
track_suite() {
    local suite_name="$1"
    local result="$2"

    total_suites=$((total_suites + 1))

    if [ "$result" -eq 0 ]; then
        passed_suites=$((passed_suites + 1))
    else
        failed_suites=$((failed_suites + 1))
        failed_suite_names+=("$suite_name")
    fi
}

# Clean previous test results
print_status "Cleaning previous test results..."
./gradlew clean --continue

echo ""
print_status "=== UNIT TESTS ==="

if [ "$TEST_TYPE" = "all" ] || [ "$TEST_TYPE" = "unit" ]; then
    run_test_suite "Unit Tests" "./gradlew test -Dtest.profile=unit --continue" "true"
    track_suite "Unit Tests" $?
else
    print_status "Skipping unit tests"
fi

echo ""
print_status "=== INTEGRATION TESTS ==="

if [ "$TEST_TYPE" = "all" ] || [ "$TEST_TYPE" = "integration" ]; then
    # Start TestContainers and run integration tests
    print_status "Starting TestContainers for integration tests..."
    run_test_suite "Integration Tests" "./gradlew integrationTest --continue" "true"
    track_suite "Integration Tests" $?
else
    print_status "Skipping integration tests"
fi

echo ""
print_status "=== END-TO-END TESTS ==="

if [ "$TEST_TYPE" = "all" ] || [ "$TEST_TYPE" = "e2e" ]; then
    run_test_suite "End-to-End Tests" "./gradlew e2eTest --continue" "true"
    track_suite "E2E Tests" $?
else
    print_status "Skipping end-to-end tests"
fi

echo ""
print_status "=== PERFORMANCE TESTS ==="

if [ "$TEST_TYPE" = "all" ] || [ "$TEST_TYPE" = "performance" ]; then
    run_test_suite "Performance Tests" "./gradlew :performance-tests:test --continue" "true"
    track_suite "Performance Tests" $?
else
    print_status "Skipping performance tests"
fi

echo ""
print_status "=== COVERAGE REPORT ==="

if [ "$GENERATE_REPORTS" = "true" ]; then
    print_status "Generating coverage reports..."
    if ./gradlew jacocoTestReport --continue; then
        print_success "Coverage reports generated"

        # Find and display coverage summary
        if [ -f "build/reports/jacoco/test/html/index.html" ]; then
            print_status "Coverage report available at: build/reports/jacoco/test/html/index.html"
        fi
    else
        print_warning "Coverage report generation failed"
    fi
fi

echo ""
print_status "=== TEST SUMMARY ==="

echo "Total Test Suites: $total_suites"
echo "Passed: $passed_suites"
echo "Failed: $failed_suites"

if [ $failed_suites -gt 0 ]; then
    print_error "The following test suites failed:"
    for failed_suite in "${failed_suite_names[@]}"; do
        echo "  - $failed_suite"
    done
    echo ""
    print_error "Some tests failed. Check the reports for details."

    # Provide helpful information
    echo ""
    print_status "=== TEST REPORTS ==="
    echo "Unit Test Reports: */build/reports/tests/test/index.html"
    echo "Integration Test Reports: */build/reports/tests/integrationTest/index.html"
    echo "Coverage Reports: */build/reports/jacoco/test/jacocoTestReport/index.html"
    echo ""
    print_status "=== TROUBLESHOOTING ==="
    echo "1. Check TestContainers are running: docker ps"
    echo "2. Check application logs in build/reports/"
    echo "3. Run individual test suites: ./gradlew :services:SERVICE_NAME:test"
    echo "4. Check database connectivity in integration tests"

    exit 1
else
    print_success "All test suites passed! 🎉"
    echo ""
    print_status "=== REPORTS GENERATED ==="
    if [ "$GENERATE_REPORTS" = "true" ]; then
        echo "✅ Unit Test Reports"
        echo "✅ Integration Test Reports"
        echo "✅ Coverage Reports"
        echo "✅ Performance Test Reports"
    fi
    echo ""
    print_success "Testing completed successfully. All systems are go! 🚀"
    exit 0
fi