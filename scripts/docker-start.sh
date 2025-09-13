#!/bin/bash

# Docker start script for Gasolinera JSM Platform
# This script starts the entire platform using Docker Compose

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

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker and try again."
    exit 1
fi

# Default configuration
COMPOSE_FILES="-f docker-compose.yml"
ENVIRONMENT="development"

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --prod|--production)
            COMPOSE_FILES="-f docker-compose.yml -f docker-compose.prod.yml"
            ENVIRONMENT="production"
            shift
            ;;
        --dev|--development)
            COMPOSE_FILES="-f docker-compose.yml -f docker-compose.override.yml"
            ENVIRONMENT="development"
            shift
            ;;
        --build)
            BUILD_FLAG="--build"
            shift
            ;;
        --detach|-d)
            DETACH_FLAG="-d"
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --prod, --production    Start in production mode"
            echo "  --dev, --development    Start in development mode (default)"
            echo "  --build                 Build images before starting"
            echo "  --detach, -d           Run in detached mode"
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

print_status "Starting Gasolinera JSM Platform in $ENVIRONMENT mode..."

# Create logs directory if it doesn't exist
mkdir -p logs/{api-gateway,auth-service,station-service,coupon-service,redemption-service,ad-engine,raffle-service,postgres,redis,rabbitmq}

# Load environment variables
if [ -f .env.docker ]; then
    print_status "Loading Docker environment variables..."
    export $(cat .env.docker | grep -v '^#' | xargs)
fi

# Create network if it doesn't exist
print_status "Creating Docker network..."
docker network create gasolinera-network --driver bridge --subnet 172.20.0.0/16 2>/dev/null || true

# Start infrastructure services first
print_status "Starting infrastructure services..."
docker-compose $COMPOSE_FILES up $DETACH_FLAG postgres redis rabbitmq vault jaeger

# Wait for infrastructure services to be ready
print_status "Waiting for infrastructure services to be ready..."
sleep 15

# Check if infrastructure services are healthy
print_status "Checking infrastructure health..."
for i in {1..60}; do
    healthy_count=0

    # Check each service individually
    if docker-compose $COMPOSE_FILES ps postgres | grep -q "healthy\|Up"; then
        healthy_count=$((healthy_count + 1))
    fi

    if docker-compose $COMPOSE_FILES ps redis | grep -q "healthy\|Up"; then
        healthy_count=$((healthy_count + 1))
    fi

    if docker-compose $COMPOSE_FILES ps rabbitmq | grep -q "healthy\|Up"; then
        healthy_count=$((healthy_count + 1))
    fi

    if docker-compose $COMPOSE_FILES ps vault | grep -q "healthy\|Up"; then
        healthy_count=$((healthy_count + 1))
    fi

    if docker-compose $COMPOSE_FILES ps jaeger | grep -q "healthy\|Up"; then
        healthy_count=$((healthy_count + 1))
    fi

    if [ $healthy_count -eq 5 ]; then
        print_success "All infrastructure services are ready"
        break
    fi

    if [ $i -eq 60 ]; then
        print_error "Infrastructure services failed to start properly"
        print_status "Service status:"
        docker-compose $COMPOSE_FILES ps
        print_status "Logs:"
        docker-compose $COMPOSE_FILES logs --tail=20 postgres redis rabbitmq vault jaeger
        exit 1
    fi

    print_status "Waiting for infrastructure services... ($healthy_count/5 ready, attempt $i/60)"
    sleep 5
done

# Start monitoring services if in monitoring mode
if [ -f docker-compose.monitoring.yml ] && [[ "$COMPOSE_FILES" == *"monitoring"* ]]; then
    print_status "Starting monitoring services..."
    docker-compose $COMPOSE_FILES up $DETACH_FLAG prometheus grafana alertmanager node-exporter cadvisor
    sleep 10
fi

# Start application services
print_status "Starting application services..."
docker-compose $COMPOSE_FILES up $BUILD_FLAG $DETACH_FLAG

if [ -z "$DETACH_FLAG" ]; then
    print_warning "Services are running in foreground mode. Press Ctrl+C to stop."
else
    print_success "All services started successfully!"
    echo ""
    print_status "Service URLs:"
    echo "  API Gateway:     http://localhost:8080"
    echo "  Auth Service:    http://localhost:8081"
    echo "  Station Service: http://localhost:8082"
    echo "  Coupon Service:  http://localhost:8083"
    echo "  Redemption:      http://localhost:8084"
    echo "  Ad Engine:       http://localhost:8085"
    echo "  Raffle Service:  http://localhost:8086"
    echo ""
    print_status "Monitoring URLs:"
    echo "  Grafana:         http://localhost:3000 (admin/gasolinera_grafana_password)"
    echo "  Prometheus:      http://localhost:9090"
    echo "  Jaeger:          http://localhost:16686"
    echo "  RabbitMQ:        http://localhost:15672 (gasolinera_user/gasolinera_password)"
    echo ""
    print_status "To view logs: docker-compose $COMPOSE_FILES logs -f [service-name]"
    print_status "To stop: ./scripts/docker-stop.sh"
fi