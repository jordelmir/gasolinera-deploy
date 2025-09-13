#!/bin/bash

# Docker build script for Gasolinera JSM Platform
# This script builds all Docker images for the platform

set -e

echo "🚀 Building Gasolinera JSM Platform Docker Images..."

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

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker and try again."
    exit 1
fi

# Build arguments
BUILD_ARGS=""
if [ "$1" = "--no-cache" ]; then
    BUILD_ARGS="--no-cache"
    print_warning "Building with --no-cache flag"
fi

# Services to build
SERVICES=(
    "api-gateway"
    "auth-service"
    "station-service"
    "coupon-service"
    "redemption-service"
    "ad-engine"
    "raffle-service"
)

# Build each service
for service in "${SERVICES[@]}"; do
    print_status "Building $service..."

    if docker build $BUILD_ARGS -f services/$service/Dockerfile -t gasolinera-jsm/$service:latest .; then
        print_success "$service built successfully"
    else
        print_error "Failed to build $service"
        exit 1
    fi
done

# Build summary
print_success "All services built successfully!"
print_status "Built images:"
docker images | grep gasolinera-jsm

echo ""
print_status "To start the platform, run:"
echo "  ./scripts/docker-start.sh"
echo ""
print_status "To start with production configuration, run:"
echo "  ./scripts/docker-start.sh --prod"