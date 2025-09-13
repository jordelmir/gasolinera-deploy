#!/bin/bash

# Docker logs script for Gasolinera JSM Platform
# This script provides easy access to service logs

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

# Available services
SERVICES=(
    "api-gateway"
    "auth-service"
    "station-service"
    "coupon-service"
    "redemption-service"
    "ad-engine"
    "raffle-service"
    "postgres"
    "redis"
    "rabbitmq"
    "prometheus"
    "grafana"
    "jaeger"
)

# Default configuration
COMPOSE_FILES="-f docker-compose.yml"
FOLLOW_LOGS=false
TAIL_LINES="100"
SERVICE=""

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
        --follow|-f)
            FOLLOW_LOGS=true
            shift
            ;;
        --tail|-t)
            TAIL_LINES="$2"
            shift 2
            ;;
        --all|-a)
            SERVICE="all"
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS] [SERVICE]"
            echo ""
            echo "Options:"
            echo "  --prod, --production    Use production configuration"
            echo "  --dev, --development    Use development configuration"
            echo "  --follow, -f           Follow log output"
            echo "  --tail, -t LINES       Number of lines to show (default: 100)"
            echo "  --all, -a              Show logs for all services"
            echo "  --help, -h             Show this help message"
            echo ""
            echo "Available services:"
            printf "  %s\n" "${SERVICES[@]}"
            exit 0
            ;;
        *)
            if [[ " ${SERVICES[@]} " =~ " $1 " ]]; then
                SERVICE="$1"
            else
                print_error "Unknown service: $1"
                echo "Available services: ${SERVICES[*]}"
                exit 1
            fi
            shift
            ;;
    esac
done

# If no service specified, show menu
if [ -z "$SERVICE" ]; then
    echo "Select a service to view logs:"
    echo "0) All services"
    for i in "${!SERVICES[@]}"; do
        echo "$((i+1))) ${SERVICES[$i]}"
    done

    read -p "Enter your choice (0-${#SERVICES[@]}): " choice

    if [ "$choice" = "0" ]; then
        SERVICE="all"
    elif [ "$choice" -ge 1 ] && [ "$choice" -le "${#SERVICES[@]}" ]; then
        SERVICE="${SERVICES[$((choice-1))]}"
    else
        print_error "Invalid choice"
        exit 1
    fi
fi

# Build docker-compose command
DOCKER_CMD="docker-compose $COMPOSE_FILES logs"

if [ "$FOLLOW_LOGS" = true ]; then
    DOCKER_CMD="$DOCKER_CMD -f"
fi

DOCKER_CMD="$DOCKER_CMD --tail=$TAIL_LINES"

# Execute command
if [ "$SERVICE" = "all" ]; then
    print_status "Showing logs for all services..."
    $DOCKER_CMD
else
    print_status "Showing logs for $SERVICE..."
    $DOCKER_CMD $SERVICE
fi