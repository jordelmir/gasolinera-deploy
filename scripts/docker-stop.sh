#!/bin/bash

# Docker stop script for Gasolinera JSM Platform
# This script stops all platform services

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

# Default configuration
COMPOSE_FILES="-f docker-compose.yml"
REMOVE_VOLUMES=false
REMOVE_IMAGES=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --prod|--production)
            COMPOSE_FILES="-f docker-compose.yml -f docker-compose.prod.yml"
            shift
            ;;
        --dev|--development)
            COMPOSE_FILES="-f docker-compose.yml -f docker-compose.override.yml"
            shift
            ;;
        --volumes|-v)
            REMOVE_VOLUMES=true
            shift
            ;;
        --images|-i)
            REMOVE_IMAGES=true
            shift
            ;;
        --clean)
            REMOVE_VOLUMES=true
            REMOVE_IMAGES=true
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --prod, --production    Stop production configuration"
            echo "  --dev, --development    Stop development configuration"
            echo "  --volumes, -v          Remove volumes (data will be lost!)"
            echo "  --images, -i           Remove built images"
            echo "  --clean                Remove volumes and images (full cleanup)"
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

print_status "Stopping Gasolinera JSM Platform..."

# Stop all services
print_status "Stopping all services..."
docker-compose $COMPOSE_FILES down

if [ "$REMOVE_VOLUMES" = true ]; then
    print_warning "Removing volumes (this will delete all data)..."
    docker-compose $COMPOSE_FILES down -v
    print_warning "All data has been removed!"
fi

if [ "$REMOVE_IMAGES" = true ]; then
    print_status "Removing built images..."
    docker images | grep gasolinera-jsm | awk '{print $3}' | xargs -r docker rmi -f
    print_success "Built images removed"
fi

# Clean up dangling resources
print_status "Cleaning up dangling resources..."
docker system prune -f > /dev/null 2>&1 || true

print_success "Platform stopped successfully!"

# Show remaining containers if any
RUNNING_CONTAINERS=$(docker ps -q --filter "name=gasolinera")
if [ ! -z "$RUNNING_CONTAINERS" ]; then
    print_warning "Some containers are still running:"
    docker ps --filter "name=gasolinera"
fi