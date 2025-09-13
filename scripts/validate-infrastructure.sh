#!/bin/bash
# Infrastructure Validation Script for Gasolinera JSM Platform

set -e

echo "🚀 Gasolinera JSM Platform - Infrastructure Validation"
echo "======================================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    local status=$1
    local message=$2
    case $status in
        "SUCCESS")
            echo -e "${GREEN}✅ $message${NC}"
            ;;
        "ERROR")
            echo -e "${RED}❌ $message${NC}"
            ;;
        "WARNING")
            echo -e "${YELLOW}⚠️  $message${NC}"
            ;;
        "INFO")
            echo -e "${BLUE}ℹ️  $message${NC}"
            ;;
    esac
}

# Function to check if a service is running
check_service() {
    local service_name=$1
    local container_name=$2

    if docker ps --format "table {{.Names}}" | grep -q "^${container_name}$"; then
        print_status "SUCCESS" "$service_name is running"
        return 0
    else
        print_status "ERROR" "$service_name is not running"
        return 1
    fi
}

# Function to check service health
check_health() {
    local service_name=$1
    local health_url=$2
    local expected_status=${3:-"UP"}

    print_status "INFO" "Checking $service_name health..."

    if response=$(curl -s "$health_url" 2>/dev/null); then
        if echo "$response" | grep -q "$expected_status"; then
            print_status "SUCCESS" "$service_name health check passed"
            return 0
        else
            print_status "WARNING" "$service_name health check returned unexpected status: $response"
            return 1
        fi
    else
        print_status "ERROR" "$service_name health check failed - service not responding"
        return 1
    fi
}

# Function to check database connectivity
check_database() {
    print_status "INFO" "Checking PostgreSQL connectivity..."

    if docker exec postgres pg_isready -U gasolinera_user -d gasolinera_jsm >/dev/null 2>&1; then
        print_status "SUCCESS" "PostgreSQL is ready and accepting connections"

        # Check if schemas exist
        schema_count=$(docker exec postgres psql -U gasolinera_user -d gasolinera_jsm -t -c "SELECT count(*) FROM information_schema.schemata WHERE schema_name IN ('auth_schema', 'station_schema', 'coupon_schema', 'redemption_schema', 'ad_schema', 'raffle_schema');" 2>/dev/null | tr -d ' ')

        if [ "$schema_count" = "6" ]; then
            print_status "SUCCESS" "All required database schemas are present"
        else
            print_status "WARNING" "Some database schemas are missing (found $schema_count/6)"
        fi
    else
        print_status "ERROR" "PostgreSQL is not ready"
        return 1
    fi
}

# Function to check Redis connectivity
check_redis() {
    print_status "INFO" "Checking Redis connectivity..."

    if docker exec redis redis-cli ping >/dev/null 2>&1; then
        print_status "SUCCESS" "Redis is responding to ping"
    else
        print_status "ERROR" "Redis is not responding"
        return 1
    fi
}

# Function to check RabbitMQ connectivity
check_rabbitmq() {
    print_status "INFO" "Checking RabbitMQ connectivity..."

    if docker exec rabbitmq rabbitmqctl status >/dev/null 2>&1; then
        print_status "SUCCESS" "RabbitMQ is running and healthy"

        # Check if exchanges exist
        if docker exec rabbitmq rabbitmqctl list_exchanges | grep -q "gasolinera.events"; then
            print_status "SUCCESS" "RabbitMQ exchanges are configured"
        else
            print_status "WARNING" "RabbitMQ exchanges may not be configured"
        fi
    else
        print_status "ERROR" "RabbitMQ is not responding"
        return 1
    fi
}

# Function to check Vault connectivity
check_vault() {
    print_status "INFO" "Checking HashiCorp Vault connectivity..."

    if docker exec vault vault status >/dev/null 2>&1; then
        print_status "SUCCESS" "Vault is running and accessible"

        # Check if secrets are configured
        if docker exec vault vault kv list secret/ 2>/dev/null | grep -q "database"; then
            print_status "SUCCESS" "Vault secrets are configured"
        else
            print_status "WARNING" "Vault secrets may not be configured"
        fi
    else
        print_status "ERROR" "Vault is not responding"
        return 1
    fi
}

# Function to check Jaeger connectivity
check_jaeger() {
    print_status "INFO" "Checking Jaeger connectivity..."

    if curl -s http://localhost:16686/api/services >/dev/null 2>&1; then
        print_status "SUCCESS" "Jaeger is accessible"
    else
        print_status "WARNING" "Jaeger UI is not accessible"
    fi
}

# Function to validate network connectivity between services
check_network_connectivity() {
    print_status "INFO" "Checking network connectivity between services..."

    # Test database connectivity from auth service
    if docker exec auth-service nc -z postgres 5432 >/dev/null 2>&1; then
        print_status "SUCCESS" "Auth service can reach PostgreSQL"
    else
        print_status "ERROR" "Auth service cannot reach PostgreSQL"
    fi

    # Test Redis connectivity from auth service
    if docker exec auth-service nc -z redis 6379 >/dev/null 2>&1; then
        print_status "SUCCESS" "Auth service can reach Redis"
    else
        print_status "ERROR" "Auth service cannot reach Redis"
    fi

    # Test RabbitMQ connectivity from coupon service
    if docker exec coupon-service nc -z rabbitmq 5672 >/dev/null 2>&1; then
        print_status "SUCCESS" "Coupon service can reach RabbitMQ"
    else
        print_status "ERROR" "Coupon service cannot reach RabbitMQ"
    fi
}

# Function to check API endpoints
check_api_endpoints() {
    print_status "INFO" "Checking API endpoints..."

    # Wait a bit for services to be fully ready
    sleep 5

    # Check API Gateway
    if curl -s http://localhost:8080/actuator/health >/dev/null 2>&1; then
        print_status "SUCCESS" "API Gateway is accessible"
    else
        print_status "WARNING" "API Gateway is not accessible yet"
    fi

    # Check individual services
    services=("auth-service:8081" "redemption-service:8082" "station-service:8083" "ad-engine:8084" "raffle-service:8085" "coupon-service:8086")

    for service in "${services[@]}"; do
        IFS=':' read -r name port <<< "$service"
        if curl -s "http://localhost:$port/actuator/health" >/dev/null 2>&1; then
            print_status "SUCCESS" "$name is accessible on port $port"
        else
            print_status "WARNING" "$name is not accessible on port $port yet"
        fi
    done
}

# Main validation function
main() {
    echo ""
    print_status "INFO" "Starting infrastructure validation..."
    echo ""

    # Check if Docker is running
    if ! docker info >/dev/null 2>&1; then
        print_status "ERROR" "Docker is not running or not accessible"
        exit 1
    fi

    print_status "SUCCESS" "Docker is running"
    echo ""

    # Check infrastructure services
    echo "🔍 Checking Infrastructure Services"
    echo "=================================="

    check_service "PostgreSQL" "postgres"
    check_service "Redis" "redis"
    check_service "RabbitMQ" "rabbitmq"
    check_service "Vault" "vault"
    check_service "Jaeger" "jaeger"

    echo ""

    # Check infrastructure connectivity
    echo "🔗 Checking Infrastructure Connectivity"
    echo "======================================"

    check_database
    check_redis
    check_rabbitmq
    check_vault
    check_jaeger

    echo ""

    # Check microservices
    echo "🏗️  Checking Microservices"
    echo "========================="

    check_service "API Gateway" "api-gateway"
    check_service "Auth Service" "auth-service"
    check_service "Redemption Service" "redemption-service"
    check_service "Station Service" "station-service"
    check_service "Ad Engine" "ad-engine"
    check_service "Raffle Service" "raffle-service"
    check_service "Coupon Service" "coupon-service"

    echo ""

    # Check network connectivity
    echo "🌐 Checking Network Connectivity"
    echo "==============================="

    check_network_connectivity

    echo ""

    # Check API endpoints
    echo "🔌 Checking API Endpoints"
    echo "========================"

    check_api_endpoints

    echo ""
    echo "🎉 Infrastructure validation completed!"
    echo "======================================"

    # Summary
    echo ""
    print_status "INFO" "Summary of accessible services:"
    echo "- API Gateway: http://localhost:8080"
    echo "- Auth Service: http://localhost:8081"
    echo "- Redemption Service: http://localhost:8082"
    echo "- Station Service: http://localhost:8083"
    echo "- Ad Engine: http://localhost:8084"
    echo "- Raffle Service: http://localhost:8085"
    echo "- Coupon Service: http://localhost:8086"
    echo "- Jaeger UI: http://localhost:16686"
    echo "- RabbitMQ Management: http://localhost:15672"
    echo "- Vault UI: http://localhost:8200"

    echo ""
    print_status "SUCCESS" "Infrastructure validation completed successfully!"
}

# Run main function
main "$@"