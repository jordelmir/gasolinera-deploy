#!/bin/bash

# Optimized Docker Build Script for Gasolinera JSM
# Implements layer caching, parallel builds, and build optimization

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
REGISTRY_PREFIX="${REGISTRY_PREFIX:-gasolinera-jsm}"
SERVICES=("auth-service" "api-gateway" "coupon-service" "station-service" "raffle-service" "redemption-service" "ad-engine")
BUILD_ARGS=""
PARALLEL_BUILDS="${PARALLEL_BUILDS:-4}"
CACHE_FROM="${CACHE_FROM:-true}"
PUSH_IMAGES="${PUSH_IMAGES:-false}"
BUILD_CONTEXT="."
DOCKER_BUILDKIT="${DOCKER_BUILDKIT:-1}"

# Functions
print_header() {
    echo -e "${BLUE}================================${NC}"
    echo -e "${BLUE}  Optimized Docker Builder${NC}"
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

check_docker() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed or not in PATH"
        exit 1
    fi

    if ! docker info &> /dev/null; then
        print_error "Docker daemon is not running"
        exit 1
    fi

    print_success "Docker is available and running"
}

enable_buildkit() {
    export DOCKER_BUILDKIT=1
    export COMPOSE_DOCKER_CLI_BUILD=1
    print_status "Docker BuildKit enabled for optimized builds"
}

create_builder() {
    local builder_name="gasolinera-builder"

    if ! docker buildx inspect "$builder_name" &> /dev/null; then
        print_status "Creating multi-platform builder: $builder_name"
        docker buildx create --name "$builder_name" --driver docker-container --use
        docker buildx inspect --bootstrap
    else
        print_status "Using existing builder: $builder_name"
        docker buildx use "$builder_name"
    fi
}

get_cache_args() {
    local service="$1"
    local cache_args=""

    if [ "$CACHE_FROM" = "true" ]; then
        # Use registry cache if available
        cache_args="--cache-from type=registry,ref=$REGISTRY_PREFIX/$service:cache"
        cache_args="$cache_args --cache-to type=registry,ref=$REGISTRY_PREFIX/$service:cache,mode=max"

        # Also use local cache
        cache_args="$cache_args --cache-from type=local,src=/tmp/.buildx-cache-$service"
        cache_args="$cache_args --cache-to type=local,dest=/tmp/.buildx-cache-$service,mode=max"
    fi

    echo "$cache_args"
}

build_service() {
    local service="$1"
    local dockerfile="services/$service/Dockerfile"
    local image_tag="$REGISTRY_PREFIX/$service:latest"
    local build_tag="$REGISTRY_PREFIX/$service:$(git rev-parse --short HEAD 2>/dev/null || echo 'local')"

    print_status "Building $service..."

    # Check if Dockerfile exists
    if [ ! -f "$dockerfile" ]; then
        print_error "Dockerfile not found: $dockerfile"
        return 1
    fi

    # Get cache arguments
    local cache_args
    cache_args=$(get_cache_args "$service")

    # Build arguments
    local build_cmd="docker buildx build"
    build_cmd="$build_cmd --platform linux/amd64"
    build_cmd="$build_cmd --file $dockerfile"
    build_cmd="$build_cmd --tag $image_tag"
    build_cmd="$build_cmd --tag $build_tag"
    build_cmd="$build_cmd $cache_args"
    build_cmd="$build_cmd --build-arg BUILDKIT_INLINE_CACHE=1"
    build_cmd="$build_cmd --build-arg SERVICE_NAME=$service"

    # Add custom build args if provided
    if [ -n "$BUILD_ARGS" ]; then
        build_cmd="$build_cmd $BUILD_ARGS"
    fi

    # Add push flag if enabled
    if [ "$PUSH_IMAGES" = "true" ]; then
        build_cmd="$build_cmd --push"
    else
        build_cmd="$build_cmd --load"
    fi

    # Add build context
    build_cmd="$build_cmd $BUILD_CONTEXT"

    # Execute build
    if eval "$build_cmd"; then
        print_success "$service built successfully"

        # Get image size
        if [ "$PUSH_IMAGES" = "false" ]; then
            local image_size
            image_size=$(docker images --format "table {{.Size}}" "$image_tag" | tail -n 1)
            print_status "$service image size: $image_size"
        fi

        return 0
    else
        print_error "Failed to build $service"
        return 1
    fi
}

build_services_parallel() {
    local services_to_build=("$@")
    local pids=()
    local failed_builds=()
    local successful_builds=()

    print_status "Building ${#services_to_build[@]} services with max $PARALLEL_BUILDS parallel jobs"

    # Create temporary directory for build logs
    local log_dir="/tmp/gasolinera-build-logs"
    mkdir -p "$log_dir"

    local active_jobs=0
    local service_index=0

    while [ $service_index -lt ${#services_to_build[@]} ] || [ $active_jobs -gt 0 ]; do
        # Start new jobs if we have capacity and services to build
        while [ $active_jobs -lt $PARALLEL_BUILDS ] && [ $service_index -lt ${#services_to_build[@]} ]; do
            local service="${services_to_build[$service_index]}"
            local log_file="$log_dir/$service.log"

            print_status "Starting build for $service (job $((active_jobs + 1))/$PARALLEL_BUILDS)"

            # Start build in background
            (
                if build_service "$service" > "$log_file" 2>&1; then
                    echo "SUCCESS:$service" > "$log_dir/$service.result"
                else
                    echo "FAILED:$service" > "$log_dir/$service.result"
                fi
            ) &

            pids+=($!)
            ((active_jobs++))
            ((service_index++))
        done

        # Wait for any job to complete
        if [ $active_jobs -gt 0 ]; then
            wait -n
            ((active_jobs--))
        fi

        # Check completed jobs
        for i in "${!pids[@]}"; do
            local pid="${pids[$i]}"
            if ! kill -0 "$pid" 2>/dev/null; then
                # Job completed, check result
                local service_name
                for service in "${services_to_build[@]}"; do
                    if [ -f "$log_dir/$service.result" ]; then
                        local result
                        result=$(cat "$log_dir/$service.result")
                        if [[ "$result" == "SUCCESS:$service" ]]; then
                            successful_builds+=("$service")
                            print_success "$service build completed"
                        elif [[ "$result" == "FAILED:$service" ]]; then
                            failed_builds+=("$service")
                            print_error "$service build failed"
                            # Show last few lines of log
                            print_error "Last 10 lines of $service build log:"
                            tail -n 10 "$log_dir/$service.log" | sed 's/^/  /'
                        fi
                        rm -f "$log_dir/$service.result"
                    fi
                done
                unset pids[$i]
            fi
        done

        # Clean up pids array
        pids=("${pids[@]}")

        sleep 1
    done

    # Clean up
    rm -rf "$log_dir"

    # Report results
    print_status "Build Summary:"
    print_success "Successful builds (${#successful_builds[@]}): ${successful_builds[*]}"

    if [ ${#failed_builds[@]} -gt 0 ]; then
        print_error "Failed builds (${#failed_builds[@]}): ${failed_builds[*]}"
        return 1
    fi

    return 0
}

build_services_sequential() {
    local services_to_build=("$@")
    local failed_builds=()

    print_status "Building ${#services_to_build[@]} services sequentially"

    for service in "${services_to_build[@]}"; do
        if ! build_service "$service"; then
            failed_builds+=("$service")
        fi
    done

    if [ ${#failed_builds[@]} -gt 0 ]; then
        print_error "Failed builds: ${failed_builds[*]}"
        return 1
    fi

    return 0
}

show_build_summary() {
    print_status "Build Summary:"

    if [ "$PUSH_IMAGES" = "false" ]; then
        print_status "Local images created:"
        for service in "${SERVICES[@]}"; do
            if docker images --format "table {{.Repository}}:{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}" "$REGISTRY_PREFIX/$service:latest" 2>/dev/null; then
                echo "  $REGISTRY_PREFIX/$service:latest"
            fi
        done
    else
        print_status "Images pushed to registry with prefix: $REGISTRY_PREFIX"
    fi
}

cleanup_build_cache() {
    print_status "Cleaning up build cache..."

    # Clean up old build cache
    docker buildx prune --filter until=24h --force

    # Clean up dangling images
    docker image prune --force

    print_success "Build cache cleaned up"
}

main() {
    print_header

    # Parse command line arguments
    local services_to_build=()
    local build_all=true
    local parallel_build=true
    local clean_cache=false

    while [[ $# -gt 0 ]]; do
        case $1 in
            --service)
                services_to_build+=("$2")
                build_all=false
                shift 2
                ;;
            --parallel)
                PARALLEL_BUILDS="$2"
                shift 2
                ;;
            --sequential)
                parallel_build=false
                shift
                ;;
            --push)
                PUSH_IMAGES=true
                shift
                ;;
            --no-cache)
                CACHE_FROM=false
                shift
                ;;
            --clean-cache)
                clean_cache=true
                shift
                ;;
            --build-arg)
                BUILD_ARGS="$BUILD_ARGS --build-arg $2"
                shift 2
                ;;
            --help)
                echo "Usage: $0 [OPTIONS]"
                echo "Options:"
                echo "  --service NAME        Build specific service (can be used multiple times)"
                echo "  --parallel N          Number of parallel builds (default: 4)"
                echo "  --sequential          Build services sequentially"
                echo "  --push                Push images to registry"
                echo "  --no-cache            Disable build cache"
                echo "  --clean-cache         Clean up build cache after build"
                echo "  --build-arg ARG=VAL   Pass build argument"
                echo "  --help                Show this help"
                exit 0
                ;;
            *)
                print_error "Unknown option: $1"
                exit 1
                ;;
        esac
    done

    # Use all services if none specified
    if [ "$build_all" = true ]; then
        services_to_build=("${SERVICES[@]}")
    fi

    # Validate services
    for service in "${services_to_build[@]}"; do
        if [[ ! " ${SERVICES[*]} " =~ " $service " ]]; then
            print_error "Unknown service: $service"
            print_status "Available services: ${SERVICES[*]}"
            exit 1
        fi
    done

    check_docker
    enable_buildkit
    create_builder

    print_status "Building services: ${services_to_build[*]}"
    print_status "Registry prefix: $REGISTRY_PREFIX"
    print_status "Cache enabled: $CACHE_FROM"
    print_status "Push images: $PUSH_IMAGES"

    # Build services
    if [ "$parallel_build" = true ] && [ ${#services_to_build[@]} -gt 1 ]; then
        if ! build_services_parallel "${services_to_build[@]}"; then
            exit 1
        fi
    else
        if ! build_services_sequential "${services_to_build[@]}"; then
            exit 1
        fi
    fi

    show_build_summary

    if [ "$clean_cache" = true ]; then
        cleanup_build_cache
    fi

    print_success "All builds completed successfully!"
}

# Run main function with all arguments
main "$@"