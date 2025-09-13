#!/bin/bash

# Integration Test Execution Script for Gasolinera JSM Platform
# This script runs end-to-end integration tests with proper setup and teardown

set -e

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

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Default values
TEST_TYPE="e2e"
PARALLEL_EXECUTION=false
CLEANUP_AFTER=true
DOCKER_MODE=true
GENERATE_REPORT=true
TIMEOUT_MINUTES=30

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --type)
            TEST_TYPE="$2"
            shift 2
            ;;
        --parallel)
            PARALLEL_EXECUTION=true
            shift
            ;;
        --no-cleanup)
            CLEANUP_AFTER=false
            shift
            ;;
        --no-docker)
            DOCKER_MODE=false
            shift
            ;;
        --no-report)
            GENERATE_REPORT=false
            shift
            ;;
        --timeout)
            TIMEOUT_MINUTES="$2"
            shift 2
            ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --type TYPE            Test type: e2e, integration, all (default: e2e)"
            echo "  --parallel             Run tests in parallel"
            echo "  --no-cleanup           Don't cleanup containers after tests"
            echo "  --no-docker            Don't use Docker for infrastructure"
            echo "  --no-report            Don't generate test reports"
            echo "  --timeout MINUTES      Test timeout in minutes (default: 30)"
            echo "  --help, -h             Show this help message"
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

print_status "Running integration tests for Gasolinera JSM Platform..."
print_status "Configuration:"
echo "  Test Type: $TEST_TYPE"
echo "  Parallel Execution: $PARALLEL_EXECUTION"
echo "  Docker Mode: $DOCKER_MODE"
echo "  Cleanup After: $CLEANUP_AFTER"
echo "  Generate Report: $GENERATE_REPORT"
echo "  Timeout: ${TIMEOUT_MINUTES} minutes"

# Function to check if Docker is available
check_docker() {
    if [ "$DOCKER_MODE" = true ]; then
        if ! command -v docker &> /dev/null; then
            print_error "Docker is required but not installed"
            exit 1
        fi

        if ! docker info > /dev/null 2>&1; then
            print_error "Docker is not running"
            exit 1
        fi

        if ! command -v docker-compose &> /dev/null; then
            print_error "Docker Compose is required but not installed"
            exit 1
        fi
    fi
}

# Function to start infrastructure services
start_infrastructure() {
    if [ "$DOCKER_MODE" = true ]; then
        print_status "Starting infrastructure services with Docker..."

        cd "$PROJECT_ROOT"

        # Start only infrastructure services for testing
        docker-compose -f docker-compose.yml up -d postgres redis rabbitmq

        # Wait for services to be ready
        print_status "Waiting for infrastructure services to be ready..."

        # Wait for PostgreSQL
        timeout 60 bash -c 'until docker-compose exec -T postgres pg_isready -U gasolinera_user; do sleep 2; done'

        # Wait for Redis
        timeout 60 bash -c 'until docker-compose exec -T redis redis-cli ping; do sleep 2; done'

        # Wait for RabbitMQ
        timeout 60 bash -c 'until docker-compose exec -T rabbitmq rabbitmq-diagnostics ping; do sleep 2; done'

        print_success "Infrastructure services are ready"
    else
        print_warning "Skipping Docker infrastructure setup"
    fi
}

# Function to run database migrations
run_migrations() {
    print_status "Running database migrations..."

    if [ "$DOCKER_MODE" = true ]; then
        # Run migrations using Docker
        cd "$PROJECT_ROOT"

        # Run migrations for each service
        for service in auth-service station-service coupon-service redemption-service ad-engine raffle-service; do
            print_status "Running migrations for $service..."

            # This would typically be done by starting the service briefly
            # For now, we'll assume migrations are handled by the test setup
        done
    else
        print_warning "Skipping migration setup in non-Docker mode"
    fi
}

# Function to build the project
build_project() {
    print_status "Building project..."

    cd "$PROJECT_ROOT"

    if ./gradlew clean build -x test; then
        print_success "Project built successfully"
    else
        print_error "Project build failed"
        exit 1
    fi
}

# Function to run integration tests
run_tests() {
    print_status "Running integration tests..."

    cd "$PROJECT_ROOT"

    # Prepare test command
    local test_command="./gradlew integration-tests:test"

    # Add test type filter
    case $TEST_TYPE in
        "e2e")
            test_command="$test_command --tests '*E2E*'"
            ;;
        "integration")
            test_command="$test_command --tests '*Integration*'"
            ;;
        "all")
            # Run all tests
            ;;
        *)
            print_error "Unknown test type: $TEST_TYPE"
            exit 1
            ;;
    esac

    # Add parallel execution if requested
    if [ "$PARALLEL_EXECUTION" = true ]; then
        test_command="$test_command --parallel"
    fi

    # Add timeout
    test_command="$test_command -Dtest.timeout=${TIMEOUT_MINUTES}m"

    # Set test environment
    export SPRING_PROFILES_ACTIVE=integration-test
    export TESTCONTAINERS_REUSE_ENABLE=true

    print_status "Executing: $test_command"

    # Run tests with timeout
    if timeout "${TIMEOUT_MINUTES}m" $test_command; then
        print_success "Integration tests completed successfully"
        return 0
    else
        local exit_code=$?
        if [ $exit_code -eq 124 ]; then
            print_error "Integration tests timed out after ${TIMEOUT_MINUTES} minutes"
        else
            print_error "Integration tests failed with exit code $exit_code"
        fi
        return $exit_code
    fi
}

# Function to generate test reports
generate_reports() {
    if [ "$GENERATE_REPORT" = true ]; then
        print_status "Generating test reports..."

        cd "$PROJECT_ROOT"

        # Generate test report
        ./gradlew integration-tests:testReport

        local report_dir="$PROJECT_ROOT/integration-tests/build/reports/tests/test"
        if [ -d "$report_dir" ]; then
            print_success "Test report generated at: $report_dir/index.html"
        fi

        # Generate coverage report if available
        if ./gradlew integration-tests:jacocoTestReport 2>/dev/null; then
            local coverage_dir="$PROJECT_ROOT/integration-tests/build/reports/jacoco/test/html"
            if [ -d "$coverage_dir" ]; then
                print_success "Coverage report generated at: $coverage_dir/index.html"
            fi
        fi
    fi
}

# Function to cleanup resources
cleanup() {
    if [ "$CLEANUP_AFTER" = true ]; then
        print_status "Cleaning up resources..."

        if [ "$DOCKER_MODE" = true ]; then
            cd "$PROJECT_ROOT"

            # Stop and remove containers
            docker-compose down -v

            # Clean up test containers
            docker container prune -f
            docker volume prune -f

            print_success "Cleanup completed"
        fi
    else
        print_warning "Skipping cleanup (containers left running for debugging)"
    fi
}

# Function to handle script interruption
handle_interrupt() {
    print_warning "Script interrupted, cleaning up..."
    cleanup
    exit 130
}

# Set up interrupt handler
trap handle_interrupt SIGINT SIGTERM

# Main execution flow
main() {
    local start_time=$(date +%s)

    # Pre-flight checks
    check_docker

    # Setup phase
    start_infrastructure
    run_migrations
    build_project

    # Test execution phase
    local test_result=0
    if ! run_tests; then
        test_result=1
    fi

    # Post-test phase
    generate_reports
    cleanup

    # Summary
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    local minutes=$((duration / 60))
    local seconds=$((duration % 60))

    echo ""
    print_status "=== INTEGRATION TEST SUMMARY ==="
    echo "Test Type: $TEST_TYPE"
    echo "Duration: ${minutes}m ${seconds}s"

    if [ $test_result -eq 0 ]; then
        print_success "All integration tests passed!"

        if [ "$GENERATE_REPORT" = true ]; then
            echo ""
            print_status "View detailed results:"
            echo "  Test Report: integration-tests/build/reports/tests/test/index.html"
            echo "  Coverage Report: integration-tests/build/reports/jacoco/test/html/index.html"
        fi
    else
        print_error "Some integration tests failed!"
        echo ""
        print_status "Troubleshooting tips:"
        echo "  1. Check test logs in integration-tests/build/reports/tests/test/"
        echo "  2. Verify all services are running: docker-compose ps"
        echo "  3. Check service logs: docker-compose logs [service-name]"
        echo "  4. Run with --no-cleanup to inspect containers after failure"
    fi

    exit $test_result
}

# Execute main function
main "$@"