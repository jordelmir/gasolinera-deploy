#!/bin/bash

# Environment Setup Script for Gasolinera JSM Platform
# This script sets up environment-specific configurations

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

# Function to generate secure passwords
generate_password() {
    openssl rand -base64 32 | tr -d "=+/" | cut -c1-25
}

# Function to setup development environment
setup_development() {
    print_status "Setting up development environment..."

    # Create development environment file
    cat > .env.development << EOF
# Development Environment Configuration for Gasolinera JSM Platform

# Database Configuration
POSTGRES_DB=gasolinera_db
POSTGRES_USER=gasolinera_user
POSTGRES_PASSWORD=gasolinera_dev_password
POSTGRES_HOST=localhost
POSTGRES_PORT=5432

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=gasolinera_redis_dev_password

# RabbitMQ Configuration
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=gasolinera_user
RABBITMQ_PASSWORD=gasolinera_rabbitmq_dev_password
RABBITMQ_VIRTUAL_HOST=gasolinera_vhost

# Vault Configuration
VAULT_TOKEN=gasolinera_vault_dev_token_2024
VAULT_ADDR=http://localhost:8200

# JWT Configuration
JWT_SECRET=gasolinera_jwt_secret_key_for_development_only_do_not_use_in_production
JWT_EXPIRATION=86400000

# Application Configuration
SPRING_PROFILES_ACTIVE=dev
LOGGING_LEVEL_ROOT=DEBUG
LOGGING_LEVEL_COM_GASOLINERAJSM=TRACE

# Monitoring Configuration
MANAGEMENT_TRACING_SAMPLING_PROBABILITY=1.0
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus,env,configprops

# Development Features
DEBUG_ENABLED=true
SWAGGER_ENABLED=true
H2_CONSOLE_ENABLED=false
EOF

    print_success "Development environment configuration created"
}

# Function to setup production environment
setup_production() {
    print_status "Setting up production environment..."

    # Generate secure passwords
    local db_password=$(generate_password)
    local redis_password=$(generate_password)
    local rabbitmq_password=$(generate_password)
    local jwt_secret=$(generate_password)$(generate_password)
    local vault_token=$(generate_password)

    # Create production environment file
    cat > .env.production << EOF
# Production Environment Configuration for Gasolinera JSM Platform

# Database Configuration
POSTGRES_DB=gasolinera_db
POSTGRES_USER=gasolinera_user
POSTGRES_PASSWORD=$db_password
POSTGRES_HOST=postgres
POSTGRES_PORT=5432

# Redis Configuration
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=$redis_password

# RabbitMQ Configuration
RABBITMQ_HOST=rabbitmq
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=gasolinera_user
RABBITMQ_PASSWORD=$rabbitmq_password
RABBITMQ_VIRTUAL_HOST=gasolinera_vhost

# Vault Configuration
VAULT_TOKEN=$vault_token
VAULT_ADDR=http://vault:8200

# JWT Configuration
JWT_SECRET=$jwt_secret
JWT_EXPIRATION=3600000

# Application Configuration
SPRING_PROFILES_ACTIVE=prod
LOGGING_LEVEL_ROOT=WARN
LOGGING_LEVEL_COM_GASOLINERAJSM=INFO

# Monitoring Configuration
MANAGEMENT_TRACING_SAMPLING_PROBABILITY=0.1
MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus

# Production Features
DEBUG_ENABLED=false
SWAGGER_ENABLED=false
H2_CONSOLE_ENABLED=false

# Security Configuration
SECURITY_REQUIRE_SSL=true
SECURITY_HEADERS_FRAME_OPTIONS=DENY
SECURITY_HEADERS_CONTENT_TYPE_OPTIONS=nosniff
SECURITY_HEADERS_XSS_PROTECTION=1; mode=block
EOF

    # Update Redis configuration with production password
    sed -i.bak "s/requirepass gasolinera_redis_password/requirepass $redis_password/" config/redis/redis.conf

    # Update RabbitMQ configuration with production password
    sed -i.bak "s/default_pass = gasolinera_password/default_pass = $rabbitmq_password/" config/rabbitmq/rabbitmq.conf

    print_success "Production environment configuration created"
    print_warning "IMPORTANT: Save the generated passwords securely!"
    print_warning "Database password: $db_password"
    print_warning "Redis password: $redis_password"
    print_warning "RabbitMQ password: $rabbitmq_password"
    print_warning "Vault token: $vault_token"
}

# Function to setup directories
setup_directories() {
    print_status "Creating required directories..."

    # Create log directories
    mkdir -p logs/{api-gateway,auth-service,station-service,coupon-service,redemption-service,ad-engine,raffle-service}
    mkdir -p logs/{postgres,redis,rabbitmq,vault,jaeger,prometheus,grafana}

    # Create config directories
    mkdir -p config/{grafana/dashboards,prometheus/rules,alertmanager}

    # Create data directories for development
    mkdir -p data/{postgres,redis,rabbitmq,vault,prometheus,grafana}

    # Set proper permissions
    chmod -R 755 logs/
    chmod -R 755 config/
    chmod -R 755 data/

    print_success "Directories created successfully"
}

# Function to setup SSL certificates (for production)
setup_ssl_certificates() {
    print_status "Setting up SSL certificates..."

    mkdir -p ssl/

    # Generate self-signed certificates for development/testing
    openssl req -x509 -newkey rsa:4096 -keyout ssl/private.key -out ssl/certificate.crt -days 365 -nodes \
        -subj "/C=MX/ST=CDMX/L=Mexico City/O=Gasolinera JSM/OU=IT Department/CN=localhost"

    # Set proper permissions
    chmod 600 ssl/private.key
    chmod 644 ssl/certificate.crt

    print_success "SSL certificates generated"
    print_warning "For production, replace with proper SSL certificates from a trusted CA"
}

# Function to validate environment
validate_environment() {
    print_status "Validating environment setup..."

    local validation_failed=0

    # Check required files
    local required_files=(
        "docker-compose.yml"
        "config/redis/redis.conf"
        "config/rabbitmq/rabbitmq.conf"
        "config/prometheus/prometheus.yml"
    )

    for file in "${required_files[@]}"; do
        if [ ! -f "$file" ]; then
            print_error "Required file missing: $file"
            validation_failed=1
        fi
    done

    # Check required directories
    local required_dirs=(
        "logs"
        "config"
        "scripts"
    )

    for dir in "${required_dirs[@]}"; do
        if [ ! -d "$dir" ]; then
            print_error "Required directory missing: $dir"
            validation_failed=1
        fi
    done

    # Check Docker
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed"
        validation_failed=1
    fi

    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose is not installed"
        validation_failed=1
    fi

    if [ $validation_failed -eq 0 ]; then
        print_success "Environment validation passed"
        return 0
    else
        print_error "Environment validation failed"
        return 1
    fi
}

# Function to display usage
show_usage() {
    echo "Usage: $0 [ENVIRONMENT] [OPTIONS]"
    echo ""
    echo "Environments:"
    echo "  dev, development    Setup development environment"
    echo "  prod, production    Setup production environment"
    echo ""
    echo "Options:"
    echo "  --ssl              Generate SSL certificates"
    echo "  --validate         Validate environment setup"
    echo "  --help, -h         Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 dev             Setup development environment"
    echo "  $0 prod --ssl      Setup production environment with SSL"
    echo "  $0 --validate      Validate current environment"
}

# Main function
main() {
    local environment=""
    local setup_ssl=false
    local validate_only=false

    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            dev|development)
                environment="development"
                shift
                ;;
            prod|production)
                environment="production"
                shift
                ;;
            --ssl)
                setup_ssl=true
                shift
                ;;
            --validate)
                validate_only=true
                shift
                ;;
            --help|-h)
                show_usage
                exit 0
                ;;
            *)
                print_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
        esac
    done

    print_status "Setting up Gasolinera JSM Platform environment..."

    # Setup directories first
    setup_directories

    # Validate only if requested
    if [ "$validate_only" = true ]; then
        validate_environment
        exit $?
    fi

    # Setup environment-specific configuration
    case $environment in
        development)
            setup_development
            ;;
        production)
            setup_production
            ;;
        *)
            print_error "Please specify an environment: dev or prod"
            show_usage
            exit 1
            ;;
    esac

    # Setup SSL if requested
    if [ "$setup_ssl" = true ]; then
        setup_ssl_certificates
    fi

    # Validate setup
    if validate_environment; then
        print_success "Environment setup completed successfully!"
        echo ""
        print_status "Next steps:"
        echo "1. Review the generated .env.$environment file"
        echo "2. Update any configuration as needed"
        echo "3. Run: ./scripts/docker-build.sh"
        echo "4. Run: ./scripts/docker-start.sh --$environment"
    else
        print_error "Environment setup completed with errors"
        exit 1
    fi
}

# Execute main function
main "$@"