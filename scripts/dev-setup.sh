#!/bin/bash

# Development Environment Setup Script
# Sets up the complete development environment with all optimizations

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
DATA_DIR="$PROJECT_ROOT/data"

# Functions
print_banner() {
    echo -e "${PURPLE}"
    echo "╔══════════════════════════════════════════════════════════════════════════════╗"
    echo "║                    🚀 GASOLINERA JSM DEVELOPMENT SETUP 🚀                   ║"
    echo "║                          TOP MUNDIAL CONFIGURATION                           ║"
    echo "╚══════════════════════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

print_header() {
    echo -e "${CYAN}================================${NC}"
    echo -e "${CYAN}  $1${NC}"
    echo -e "${CYAN}================================${NC}"
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

check_prerequisites() {
    print_header "Checking Prerequisites"

    local missing_tools=()

    # Check Docker
    if ! command -v docker &> /dev/null; then
        missing_tools+=("docker")
    fi

    # Check Docker Compose
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        missing_tools+=("docker-compose")
    fi

    # Check Git
    if ! command -v git &> /dev/null; then
        missing_tools+=("git")
    fi

    # Check Make
    if ! command -v make &> /dev/null; then
        missing_tools+=("make")
    fi

    if [ ${#missing_tools[@]} -ne 0 ]; then
        print_error "Missing required tools: ${missing_tools[*]}"
        print_status "Please install the missing tools and run this script again."
        exit 1
    fi

    # Check Docker daemon
    if ! docker info &> /dev/null; then
        print_error "Docker daemon is not running. Please start Docker and try again."
        exit 1
    fi

    print_success "All prerequisites are available"
}

create_data_directories() {
    print_header "Creating Data Directories"

    local directories=(
        "$DATA_DIR/postgres-dev"
        "$DATA_DIR/redis-dev"
        "$DATA_DIR/rabbitmq-dev"
        "$DATA_DIR/vault-dev"
        "$DATA_DIR/pgadmin-dev"
        "$DATA_DIR/uploads"
        "$DATA_DIR/logs"
        "$DATA_DIR/backups"
    )

    for dir in "${directories[@]}"; do
        if [ ! -d "$dir" ]; then
            mkdir -p "$dir"
            print_success "Created directory: $dir"
        else
            print_status "Directory already exists: $dir"
        fi
    done

    # Set proper permissions
    chmod 755 "$DATA_DIR"
    chmod -R 755 "$DATA_DIR"/*

    print_success "Data directories created and configured"
}

setup_environment_files() {
    print_header "Setting Up Environment Files"

    # Copy .env.dev to .env if it doesn't exist
    if [ ! -f "$PROJECT_ROOT/.env" ]; then
        cp "$PROJECT_ROOT/.env.dev" "$PROJECT_ROOT/.env"
        print_success "Created .env file from .env.dev template"
    else
        print_status ".env file already exists"
    fi

    # Create gitignore entries for data directories
    local gitignore_entries=(
        "# Development data directories"
        "data/"
        "*.log"
        ".env.local"
        ".env.*.local"
    )

    local gitignore_file="$PROJECT_ROOT/.gitignore"
    for entry in "${gitignore_entries[@]}"; do
        if ! grep -q "$entry" "$gitignore_file" 2>/dev/null; then
            echo "$entry" >> "$gitignore_file"
        fi
    done

    print_success "Environment files configured"
}

create_development_configs() {
    print_header "Creating Development Configurations"

    # Create Spring Boot development profiles
    local dev_profiles_dir="$PROJECT_ROOT/config/spring-profiles"
    mkdir -p "$dev_profiles_dir"

    # Application-dev.yml for all services
    cat > "$dev_profiles_dir/application-dev.yml" << 'EOF'
# Development Profile Configuration
spring:
  devtools:
    restart:
      enabled: true
    livereload:
      enabled: true
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
  h2:
    console:
      enabled: false

logging:
  level:
    com.gasolinerajsm: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE

management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: always
    metrics:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true

debug: true
EOF

    print_success "Development configurations created"
}

setup_docker_networks() {
    print_header "Setting Up Docker Networks"

    # Remove existing network if it exists
    if docker network ls | grep -q "gasolinera-dev-network"; then
        print_status "Removing existing development network..."
        docker network rm gasolinera-dev-network || true
    fi

    # Create development network
    docker network create \
        --driver bridge \
        --subnet=172.21.0.0/16 \
        --gateway=172.21.0.1 \
        --opt com.docker.network.bridge.name=gasolinera-dev-br0 \
        --opt com.docker.network.driver.mtu=1500 \
        gasolinera-dev-network

    print_success "Docker network created: gasolinera-dev-network"
}

build_development_images() {
    print_header "Building Development Images"

    print_status "Building optimized Docker images for development..."

    # Use the optimized build script
    if [ -f "$PROJECT_ROOT/scripts/docker-build-optimized.sh" ]; then
        cd "$PROJECT_ROOT"
        ./scripts/docker-build-optimized.sh --parallel 4
        print_success "Development images built successfully"
    else
        print_warning "Optimized build script not found, using standard build..."
        cd "$PROJECT_ROOT"
        docker-compose -f docker-compose.dev.yml build --parallel
        print_success "Images built with standard method"
    fi
}

start_infrastructure_services() {
    print_header "Starting Infrastructure Services"

    cd "$PROJECT_ROOT"

    # Start infrastructure services first
    print_status "Starting PostgreSQL, Redis, RabbitMQ, Jaeger, and Vault..."

    docker-compose -f docker-compose.dev.yml up -d \
        postgres \
        redis \
        rabbitmq \
        jaeger \
        vault \
        pgadmin \
        redis-commander

    # Wait for services to be healthy
    print_status "Waiting for infrastructure services to be ready..."

    local max_wait=120
    local wait_time=0

    while [ $wait_time -lt $max_wait ]; do
        local healthy_services=0
        local total_services=5

        # Check each service
        if docker-compose -f docker-compose.dev.yml ps postgres | grep -q "healthy"; then
            ((healthy_services++))
        fi

        if docker-compose -f docker-compose.dev.yml ps redis | grep -q "healthy"; then
            ((healthy_services++))
        fi

        if docker-compose -f docker-compose.dev.yml ps rabbitmq | grep -q "healthy"; then
            ((healthy_services++))
        fi

        if docker-compose -f docker-compose.dev.yml ps jaeger | grep -q "healthy"; then
            ((healthy_services++))
        fi

        if docker-compose -f docker-compose.dev.yml ps vault | grep -q "healthy"; then
            ((healthy_services++))
        fi

        if [ $healthy_services -eq $total_services ]; then
            print_success "All infrastructure services are ready!"
            break
        fi

        sleep 5
        wait_time=$((wait_time + 5))

        if [ $((wait_time % 30)) -eq 0 ]; then
            print_status "Still waiting for services... ($healthy_services/$total_services ready)"
        fi
    done

    if [ $wait_time -ge $max_wait ]; then
        print_warning "Some services may not be fully ready, but continuing..."
    fi
}

start_application_services() {
    print_header "Starting Application Services"

    cd "$PROJECT_ROOT"

    # Start application services
    print_status "Starting microservices..."

    docker-compose -f docker-compose.dev.yml up -d \
        auth-service \
        coupon-service \
        station-service \
        raffle-service \
        redemption-service \
        ad-engine

    # Wait a bit for services to start
    sleep 10

    # Start API Gateway last
    print_status "Starting API Gateway..."
    docker-compose -f docker-compose.dev.yml up -d api-gateway

    print_success "Application services started"
}

run_data_seeder() {
    print_header "Running Development Data Seeder"

    cd "$PROJECT_ROOT"

    # Wait for all services to be ready
    print_status "Waiting for all services to be ready before seeding data..."
    sleep 30

    # Run the data seeder
    print_status "Seeding development data..."
    docker-compose -f docker-compose.dev.yml up dev-data-seeder

    print_success "Development data seeded successfully"
}

show_service_status() {
    print_header "Service Status"

    cd "$PROJECT_ROOT"

    echo -e "${BLUE}Docker Compose Services:${NC}"
    docker-compose -f docker-compose.dev.yml ps

    echo ""
    echo -e "${BLUE}Service Health Status:${NC}"

    local services=(
        "postgres:5432"
        "redis:6379"
        "rabbitmq:15672"
        "jaeger:16686"
        "vault:8200"
        "auth-service:8081"
        "coupon-service:8082"
        "station-service:8083"
        "raffle-service:8084"
        "redemption-service:8085"
        "ad-engine:8086"
        "api-gateway:8080"
    )

    for service in "${services[@]}"; do
        local name="${service%:*}"
        local port="${service#*:}"

        if curl -f -s "http://localhost:$port/actuator/health" > /dev/null 2>&1 || \
           curl -f -s "http://localhost:$port" > /dev/null 2>&1; then
            echo -e "  ${GREEN}✓${NC} $name (http://localhost:$port)"
        else
            echo -e "  ${RED}✗${NC} $name (http://localhost:$port)"
        fi
    done
}

show_access_information() {
    print_header "🎉 Development Environment Ready!"

    echo -e "${GREEN}Your Gasolinera JSM development environment is now running!${NC}"
    echo ""
    echo -e "${CYAN}🔗 Service URLs:${NC}"
    echo "  📊 API Gateway:        http://localhost:8080"
    echo "  🔐 Auth Service:       http://localhost:8081"
    echo "  🎫 Coupon Service:     http://localhost:8082"
    echo "  ⛽ Station Service:    http://localhost:8083"
    echo "  🎰 Raffle Service:     http://localhost:8084"
    echo "  💰 Redemption Service: http://localhost:8085"
    echo "  📢 Ad Engine:          http://localhost:8086"
    echo ""
    echo -e "${CYAN}🛠️  Development Tools:${NC}"
    echo "  🐘 PgAdmin:            http://localhost:5050"
    echo "  🔴 Redis Commander:    http://localhost:8081"
    echo "  🐰 RabbitMQ Management: http://localhost:15672"
    echo "  🔍 Jaeger Tracing:     http://localhost:16686"
    echo "  🔒 Vault UI:           http://localhost:8200"
    echo ""
    echo -e "${CYAN}👤 Test Accounts:${NC}"
    echo "  Admin:      admin@gasolinera.com / Admin123!"
    echo "  User:       user1@test.com / Test123!"
    echo "  Owner:      owner1@test.com / Test123!"
    echo "  Advertiser: advertiser1@test.com / Test123!"
    echo ""
    echo -e "${CYAN}🚀 Quick Commands:${NC}"
    echo "  make dev-logs          # Follow all service logs"
    echo "  make dev-restart       # Restart all services"
    echo "  make dev-rebuild       # Rebuild and restart"
    echo "  make dev-stop          # Stop all services"
    echo "  make dev-clean         # Clean everything"
    echo ""
    echo -e "${YELLOW}💡 Pro Tips:${NC}"
    echo "  • Debug ports are available on 5005-5011 for remote debugging"
    echo "  • Hot reload is enabled - just modify your code and it will restart"
    echo "  • All actuator endpoints are exposed for monitoring"
    echo "  • Check service health at /actuator/health endpoints"
    echo ""
    echo -e "${GREEN}Happy coding! 🎯${NC}"
}

cleanup_on_error() {
    print_error "Setup failed. Cleaning up..."
    cd "$PROJECT_ROOT"
    docker-compose -f docker-compose.dev.yml down --remove-orphans || true
    docker network rm gasolinera-dev-network || true
}

main() {
    # Set up error handling
    trap cleanup_on_error ERR

    print_banner

    # Change to project root
    cd "$PROJECT_ROOT"

    # Execute setup steps
    check_prerequisites
    create_data_directories
    setup_environment_files
    create_development_configs
    setup_docker_networks
    build_development_images
    start_infrastructure_services
    start_application_services
    run_data_seeder

    # Show results
    show_service_status
    show_access_information

    print_success "🚀 Development environment setup completed successfully!"
}

# Handle command line arguments
case "${1:-}" in
    --help|-h)
        echo "Usage: $0 [OPTIONS]"
        echo "Options:"
        echo "  --help, -h     Show this help message"
        echo "  --clean        Clean existing environment before setup"
        echo "  --no-build     Skip building images"
        echo "  --no-seed      Skip seeding development data"
        exit 0
        ;;
    --clean)
        print_status "Cleaning existing environment..."
        cd "$PROJECT_ROOT"
        docker-compose -f docker-compose.dev.yml down --volumes --remove-orphans || true
        docker network rm gasolinera-dev-network || true
        rm -rf "$DATA_DIR" || true
        ;;
esac

# Run main function
main "$@"