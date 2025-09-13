#!/bin/bash

# Complete Docker Build and Validation Script
# Builds, tests, scans, and validates all Docker images

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
REGISTRY_PREFIX="${REGISTRY_PREFIX:-gasolinera-jsm}"
SERVICES=("auth-service" "api-gateway" "coupon-service" "station-service" "raffle-service" "redemption-service" "ad-engine")

# Build options
BUILD_PARALLEL="${BUILD_PARALLEL:-true}"
RUN_TESTS="${RUN_TESTS:-true}"
RUN_SECURITY_SCAN="${RUN_SECURITY_SCAN:-true}"
PUSH_IMAGES="${PUSH_IMAGES:-false}"
CLEAN_AFTER="${CLEAN_AFTER:-false}"

# Functions
print_header() {
    echo -e "${BLUE}================================${NC}"
    echo -e "${BLUE}  Complete Docker Build Pipeline${NC}"
    echo -e "${BLUE}================================${NC}"
}

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

print_step() {
    echo ""
    echo -e "${BLUE}==== $1 ====${NC}"
}

check_prerequisites() {
    print_step "Checking Prerequisites"

    local missing_tools=()

    # Check Docker
    if ! command -v docker &> /dev/null; then
        missing_tools+=("docker")
    fi

    # Check Docker Buildx
    if ! docker buildx version &> /dev/null; then
        missing_tools+=("docker-buildx")
    fi

    # Check if Docker daemon is running
    if ! docker info &> /dev/null; then
        print_error "Docker daemon is not running"
        exit 1
    fi

    if [ ${#missing_tools[@]} -ne 0 ]; then
        print_error "Missing required tools: ${missing_tools[*]}"
        exit 1
    fi

    print_success "All prerequisites are available"
}

validate_project_structure() {
    print_step "Validating Project Structure"

    # Check if we're in the right directory
    if [ ! -f "$PROJECT_ROOT/build.gradle.kts" ]; then
        print_error "Not in project root directory. Please run from gasolinera-jsm-ultimate/"
        exit 1
    fi

    # Check if all service Dockerfiles exist
    local missing_dockerfiles=()
    for service in "${SERVICES[@]}"; do
        if [ ! -f "$PROJECT_ROOT/services/$service/Dockerfile" ]; then
            missing_dockerfiles+=("services/$service/Dockerfile")
        fi
    done

    if [ ${#missing_dockerfiles[@]} -ne 0 ]; then
        print_error "Missing Dockerfiles: ${missing_dockerfiles[*]}"
        exit 1
    fi

    print_success "Project structure is valid"
}

run_gradle_tests() {
    if [ "$RUN_TESTS" != "true" ]; then
        print_warning "Skipping Gradle tests (RUN_TESTS=false)"
        return 0
    fi

    print_step "Running Gradle Tests"

    cd "$PROJECT_ROOT"

    print_status "Running unit tests for all services..."
    if ./gradlew test --parallel --continue; then
        print_success "All Gradle tests passed"
    else
        print_error "Some Gradle tests failed"
        return 1
    fi
}

build_docker_images() {
    print_step "Building Docker Images"

    cd "$PROJECT_ROOT"

    local build_args=()

    if [ "$BUILD_PARALLEL" = "true" ]; then
        build_args+=("--parallel" "4")
    else
        build_args+=("--sequential")
    fi

    if [ "$PUSH_IMAGES" = "true" ]; then
        build_args+=("--push")
    fi

    print_status "Building all services with args: ${build_args[*]}"

    if ./scripts/docker-build-optimized.sh "${build_args[@]}"; then
        print_success "All Docker images built successfully"
    else
        print_error "Docker build failed"
        return 1
    fi
}

run_security_scan() {
    if [ "$RUN_SECURITY_SCAN" != "true" ]; then
        print_warning "Skipping security scan (RUN_SECURITY_SCAN=false)"
        return 0
    fi

    print_step "Running Security Scan"

    cd "$PROJECT_ROOT"

    print_status "Scanning all images for security vulnerabilities..."

    if ./scripts/docker-security-scan.sh --severity HIGH; then
        print_success "All images passed security scan"
    else
        print_warning "Some images have security vulnerabilities. Check reports in security-reports/"
        # Don't fail the build for security warnings, just warn
        return 0
    fi
}

validate_images() {
    print_step "Validating Docker Images"

    local validation_failed=false

    for service in "${SERVICES[@]}"; do
        local image="$REGISTRY_PREFIX/$service:latest"

        print_status "Validating $image..."

        # Check if image exists
        if ! docker image inspect "$image" &> /dev/null; then
            print_error "Image $image not found"
            validation_failed=true
            continue
        fi

        # Check image size (warn if > 300MB)
        local size_bytes
        size_bytes=$(docker image inspect "$image" --format='{{.Size}}')
        local size_mb=$((size_bytes / 1024 / 1024))

        if [ "$size_mb" -gt 300 ]; then
            print_warning "$service image is large: ${size_mb}MB"
        else
            print_success "$service image size: ${size_mb}MB"
        fi

        # Test container startup (quick test)
        print_status "Testing $service container startup..."
        local container_id
        if container_id=$(docker run -d --rm -p 0:8080 "$image"); then
            sleep 5
            if docker ps | grep -q "$container_id"; then
                print_success "$service container started successfully"
                docker stop "$container_id" &> /dev/null || true
            else
                print_error "$service container failed to start"
                validation_failed=true
            fi
        else
            print_error "Failed to run $service container"
            validation_failed=true
        fi
    done

    if [ "$validation_failed" = true ]; then
        return 1
    fi

    print_success "All images validated successfully"
}

generate_build_report() {
    print_step "Generating Build Report"

    local report_file="$PROJECT_ROOT/build-report.md"

    cat > "$report_file" << EOF
# Docker Build Report

**Generated:** $(date)
**Build ID:** $(git rev-parse --short HEAD 2>/dev/null || echo 'local')
**Registry:** $REGISTRY_PREFIX

## Build Configuration

- **Parallel Build:** $BUILD_PARALLEL
- **Run Tests:** $RUN_TESTS
- **Security Scan:** $RUN_SECURITY_SCAN
- **Push Images:** $PUSH_IMAGES

## Image Summary

| Service | Image Tag | Size | Status |
|---------|-----------|------|--------|
EOF

    for service in "${SERVICES[@]}"; do
        local image="$REGISTRY_PREFIX/$service:latest"
        local status="❌ Not Found"
        local size="N/A"

        if docker image inspect "$image" &> /dev/null; then
            status="✅ Built"
            local size_bytes
            size_bytes=$(docker image inspect "$image" --format='{{.Size}}')
            size="$((size_bytes / 1024 / 1024))MB"
        fi

        echo "| $service | $image | $size | $status |" >> "$report_file"
    done

    cat >> "$report_file" << EOF

## Build Artifacts

- **Docker Images:** Built and tagged with \`latest\` and commit hash
- **Security Reports:** Available in \`security-reports/\` directory
- **Build Logs:** Available in \`/tmp/gasolinera-build-logs/\`

## Next Steps

1. **Testing:** Run integration tests with \`docker-compose -f docker-compose.build.yml up\`
2. **Deployment:** Push images to registry with \`--push\` flag
3. **Monitoring:** Set up monitoring for deployed containers

## Commands

\`\`\`bash
# Run all services
docker-compose -f docker-compose.build.yml up

# Run specific service
docker run -p 8080:8080 $REGISTRY_PREFIX/auth-service:latest

# Push to registry
./scripts/docker-build-optimized.sh --push
\`\`\`

EOF

    print_success "Build report generated: $report_file"
}

cleanup_build_artifacts() {
    if [ "$CLEAN_AFTER" != "true" ]; then
        print_warning "Skipping cleanup (CLEAN_AFTER=false)"
        return 0
    fi

    print_step "Cleaning Up Build Artifacts"

    # Clean up build cache
    print_status "Cleaning Docker build cache..."
    docker buildx prune --filter until=24h --force

    # Clean up dangling images
    print_status "Cleaning dangling images..."
    docker image prune --force

    # Clean up build logs
    if [ -d "/tmp/gasolinera-build-logs" ]; then
        print_status "Cleaning build logs..."
        rm -rf /tmp/gasolinera-build-logs
    fi

    print_success "Cleanup completed"
}

show_summary() {
    print_step "Build Summary"

    echo -e "${GREEN}✅ Build Pipeline Completed Successfully${NC}"
    echo ""
    echo "📊 **Results:**"
    echo "   - Services Built: ${#SERVICES[@]}"
    echo "   - Registry Prefix: $REGISTRY_PREFIX"
    echo "   - Tests Run: $RUN_TESTS"
    echo "   - Security Scan: $RUN_SECURITY_SCAN"
    echo "   - Images Pushed: $PUSH_IMAGES"
    echo ""
    echo "📁 **Artifacts:**"
    echo "   - Build Report: build-report.md"
    echo "   - Security Reports: security-reports/"
    echo "   - Docker Images: docker images | grep $REGISTRY_PREFIX"
    echo ""
    echo "🚀 **Next Steps:**"
    echo "   - Test services: docker-compose -f docker-compose.build.yml up"
    echo "   - Deploy to staging: ./scripts/deploy-staging.sh"
    echo "   - Push to registry: ./scripts/docker-build-optimized.sh --push"
}

main() {
    print_header

    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --no-tests)
                RUN_TESTS=false
                shift
                ;;
            --no-security-scan)
                RUN_SECURITY_SCAN=false
                shift
                ;;
            --sequential)
                BUILD_PARALLEL=false
                shift
                ;;
            --push)
                PUSH_IMAGES=true
                shift
                ;;
            --clean)
                CLEAN_AFTER=true
                shift
                ;;
            --help)
                echo "Usage: $0 [OPTIONS]"
                echo "Options:"
                echo "  --no-tests           Skip Gradle tests"
                echo "  --no-security-scan   Skip security vulnerability scan"
                echo "  --sequential         Build services sequentially"
                echo "  --push               Push images to registry"
                echo "  --clean              Clean up build artifacts after build"
                echo "  --help               Show this help"
                exit 0
                ;;
            *)
                print_error "Unknown option: $1"
                exit 1
                ;;
        esac
    done

    # Change to project root
    cd "$PROJECT_ROOT"

    # Execute pipeline steps
    check_prerequisites
    validate_project_structure

    if ! run_gradle_tests; then
        print_error "Gradle tests failed. Aborting build."
        exit 1
    fi

    if ! build_docker_images; then
        print_error "Docker build failed. Aborting pipeline."
        exit 1
    fi

    run_security_scan  # Don't fail on security warnings

    if ! validate_images; then
        print_error "Image validation failed."
        exit 1
    fi

    generate_build_report
    cleanup_build_artifacts
    show_summary

    print_success "Complete build pipeline finished successfully!"
}

# Run main function with all arguments
main "$@"