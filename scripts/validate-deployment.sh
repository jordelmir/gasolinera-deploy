#!/bin/bash

# Deployment Validation Script for Gasolinera JSM Platform
# This script validates that all services are running correctly

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
MAX_RETRIES=30
RETRY_INTERVAL=10
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
REPORT_FILE="deployment_validation_report_$TIMESTAMP.md"

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

# Function to check service health
check_service_health() {
    local service_name=$1
    local health_url=$2
    local retries=0

    print_status "Checking health of $service_name..."

    while [ $retries -lt $MAX_RETRIES ]; do
        if curl -s -f "$health_url" > /dev/null 2>&1; then
            print_success "$service_name is healthy"
            return 0
        fi

        retries=$((retries + 1))
        if [ $retries -lt $MAX_RETRIES ]; then
            print_warning "$service_name not ready, retrying in $RETRY_INTERVAL seconds... ($retries/$MAX_RETRIES)"
            sleep $RETRY_INTERVAL
        fi
    done

    print_error "$service_name failed health check after $MAX_RETRIES attempts"
    return 1
}

# Function to check service metrics
check_service_metrics() {
    local service_name=$1
    local metrics_url=$2

    print_status "Checking metrics for $service_name..."

    if curl -s -f "$metrics_url" | grep -q "jvm_memory_used_bytes"; then
        print_success "$service_name metrics are available"
        return 0
    else
        print_error "$service_name metrics are not available"
        return 1
    fi
}

# Function to check database connectivity
check_database_connectivity() {
    print_status "Checking database connectivity..."

    if docker-compose exec -T postgres pg_isready -U gasolinera_user -d gasolinera_db > /dev/null 2>&1; then
        print_success "Database is accessible"
        return 0
    else
        print_error "Database is not accessible"
        return 1
    fi
}

# Function to check Redis connectivity
check_redis_connectivity() {
    print_status "Checking Redis connectivity..."

    if docker-compose exec -T redis redis-cli ping | grep -q "PONG"; then
        print_success "Redis is accessible"
        return 0
    else
        print_error "Redis is not accessible"
        return 1
    fi
}

# Function to check RabbitMQ connectivity
check_rabbitmq_connectivity() {
    print_status "Checking RabbitMQ connectivity..."

    if docker-compose exec -T rabbitmq rabbitmq-diagnostics ping > /dev/null 2>&1; then
        print_success "RabbitMQ is accessible"
        return 0
    else
        print_error "RabbitMQ is not accessible"
        return 1
    fi
}

# Function to test API endpoints
test_api_endpoints() {
    print_status "Testing API endpoints..."

    local endpoints=(
        "http://localhost:8080/actuator/health|API Gateway Health"
        "http://localhost:8081/actuator/health|Auth Service Health"
        "http://localhost:8082/actuator/health|Redemption Service Health"
        "http://localhost:8083/actuator/health|Station Service Health"
        "http://localhost:8084/actuator/health|Ad Engine Health"
        "http://localhost:8085/actuator/health|Raffle Service Health"
        "http://localhost:8086/actuator/health|Coupon Service Health"
    )

    local failed_endpoints=0

    for endpoint_info in "${endpoints[@]}"; do
        IFS='|' read -r url description <<< "$endpoint_info"

        if curl -s -f "$url" > /dev/null 2>&1; then
            print_success "$description: OK"
        else
            print_error "$description: FAILED"
            failed_endpoints=$((failed_endpoints + 1))
        fi
    done

    if [ $failed_endpoints -eq 0 ]; then
        print_success "All API endpoints are healthy"
        return 0
    else
        print_error "$failed_endpoints API endpoints failed"
        return 1
    fi
}

# Function to test business functionality
test_business_functionality() {
    print_status "Testing business functionality..."

    # Test user registration
    local registration_response=$(curl -s -X POST \
        -H "Content-Type: application/json" \
        -d '{"phoneNumber":"+525555000001","firstName":"Test","lastName":"User"}' \
        http://localhost:8080/api/auth/register)

    if echo "$registration_response" | grep -q "success\|exists\|created"; then
        print_success "User registration endpoint is working"
    else
        print_warning "User registration endpoint may have issues"
    fi

    # Test station listing
    local stations_response=$(curl -s http://localhost:8080/api/stations)

    if echo "$stations_response" | grep -q "\[\]" || echo "$stations_response" | grep -q "id"; then
        print_success "Station listing endpoint is working"
    else
        print_warning "Station listing endpoint may have issues"
    fi

    # Test coupon campaigns
    local campaigns_response=$(curl -s http://localhost:8080/api/campaigns)

    if echo "$campaigns_response" | grep -q "\[\]" || echo "$campaigns_response" | grep -q "id"; then
        print_success "Campaign listing endpoint is working"
    else
        print_warning "Campaign listing endpoint may have issues"
    fi
}

# Function to check monitoring stack
check_monitoring_stack() {
    print_status "Checking monitoring stack..."

    local monitoring_services=(
        "http://localhost:9090/-/healthy|Prometheus"
        "http://localhost:3000/api/health|Grafana"
        "http://localhost:16686/|Jaeger"
        "http://localhost:15672/|RabbitMQ Management"
    )

    local failed_monitoring=0

    for service_info in "${monitoring_services[@]}"; do
        IFS='|' read -r url service_name <<< "$service_info"

        if curl -s -f "$url" > /dev/null 2>&1; then
            print_success "$service_name is accessible"
        else
            print_error "$service_name is not accessible"
            failed_monitoring=$((failed_monitoring + 1))
        fi
    done

    if [ $failed_monitoring -eq 0 ]; then
        print_success "All monitoring services are accessible"
        return 0
    else
        print_error "$failed_monitoring monitoring services failed"
        return 1
    fi
}

# Function to check resource usage
check_resource_usage() {
    print_status "Checking resource usage..."

    # Check Docker stats
    local docker_stats=$(docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}")

    print_status "Current resource usage:"
    echo "$docker_stats"

    # Check for high memory usage
    local high_memory_containers=$(echo "$docker_stats" | awk 'NR>1 {gsub(/[^0-9.]/, "", $3); if ($3 > 80) print $1}')

    if [ -n "$high_memory_containers" ]; then
        print_warning "High memory usage detected in containers: $high_memory_containers"
    else
        print_success "Memory usage is within acceptable limits"
    fi
}

# Function to generate validation report
generate_validation_report() {
    local overall_status=$1

    cat > "$REPORT_FILE" << EOF
# Deployment Validation Report

**Generated:** $(date)
**Platform:** Gasolinera JSM Platform
**Overall Status:** $overall_status

## Validation Summary

### Infrastructure Services
- PostgreSQL: $(check_database_connectivity && echo "✅ PASS" || echo "❌ FAIL")
- Redis: $(check_redis_connectivity && echo "✅ PASS" || echo "❌ FAIL")
- RabbitMQ: $(check_rabbitmq_connectivity && echo "✅ PASS" || echo "❌ FAIL")

### Application Services
- API Gateway: $(curl -s -f http://localhost:8080/actuator/health > /dev/null && echo "✅ PASS" || echo "❌ FAIL")
- Auth Service: $(curl -s -f http://localhost:8081/actuator/health > /dev/null && echo "✅ PASS" || echo "❌ FAIL")
- Station Service: $(curl -s -f http://localhost:8083/actuator/health > /dev/null && echo "✅ PASS" || echo "❌ FAIL")
- Coupon Service: $(curl -s -f http://localhost:8086/actuator/health > /dev/null && echo "✅ PASS" || echo "❌ FAIL")
- Redemption Service: $(curl -s -f http://localhost:8082/actuator/health > /dev/null && echo "✅ PASS" || echo "❌ FAIL")
- Ad Engine: $(curl -s -f http://localhost:8084/actuator/health > /dev/null && echo "✅ PASS" || echo "❌ FAIL")
- Raffle Service: $(curl -s -f http://localhost:8085/actuator/health > /dev/null && echo "✅ PASS" || echo "❌ FAIL")

### Monitoring Services
- Prometheus: $(curl -s -f http://localhost:9090/-/healthy > /dev/null && echo "✅ PASS" || echo "❌ FAIL")
- Grafana: $(curl -s -f http://localhost:3000/api/health > /dev/null && echo "✅ PASS" || echo "❌ FAIL")
- Jaeger: $(curl -s -f http://localhost:16686/ > /dev/null && echo "✅ PASS" || echo "❌ FAIL")

## Service URLs

### Application Services
- API Gateway: http://localhost:8080
- Auth Service: http://localhost:8081
- Station Service: http://localhost:8083
- Coupon Service: http://localhost:8086
- Redemption Service: http://localhost:8082
- Ad Engine: http://localhost:8084
- Raffle Service: http://localhost:8085

### Monitoring & Management
- Grafana: http://localhost:3000 (admin/gasolinera_grafana_password)
- Prometheus: http://localhost:9090
- Jaeger: http://localhost:16686
- RabbitMQ Management: http://localhost:15672 (gasolinera_user/gasolinera_password)

## Resource Usage

$(docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}")

## Recommendations

### If validation failed:
1. Check service logs: \`docker-compose logs [service-name]\`
2. Verify resource availability (RAM, CPU, disk space)
3. Ensure all required ports are available
4. Check network connectivity between services

### For production deployment:
1. Use production configuration: \`docker-compose -f docker-compose.yml -f docker-compose.prod.yml up\`
2. Configure external load balancer
3. Set up external monitoring and alerting
4. Configure backup strategies for data volumes
5. Implement proper secret management

---

*This report was generated automatically by the deployment validation script.*
EOF

    print_success "Validation report generated: $REPORT_FILE"
}

# Main validation function
main() {
    print_status "Starting deployment validation for Gasolinera JSM Platform..."
    echo ""

    local validation_failed=0

    # Check infrastructure services
    print_status "=== Infrastructure Services Validation ==="
    check_database_connectivity || validation_failed=1
    check_redis_connectivity || validation_failed=1
    check_rabbitmq_connectivity || validation_failed=1
    echo ""

    # Check application services
    print_status "=== Application Services Validation ==="
    check_service_health "API Gateway" "http://localhost:8080/actuator/health" || validation_failed=1
    check_service_health "Auth Service" "http://localhost:8081/actuator/health" || validation_failed=1
    check_service_health "Station Service" "http://localhost:8083/actuator/health" || validation_failed=1
    check_service_health "Coupon Service" "http://localhost:8086/actuator/health" || validation_failed=1
    check_service_health "Redemption Service" "http://localhost:8082/actuator/health" || validation_failed=1
    check_service_health "Ad Engine" "http://localhost:8084/actuator/health" || validation_failed=1
    check_service_health "Raffle Service" "http://localhost:8085/actuator/health" || validation_failed=1
    echo ""

    # Check monitoring stack
    print_status "=== Monitoring Stack Validation ==="
    check_monitoring_stack || validation_failed=1
    echo ""

    # Test API endpoints
    print_status "=== API Endpoints Testing ==="
    test_api_endpoints || validation_failed=1
    echo ""

    # Test business functionality
    print_status "=== Business Functionality Testing ==="
    test_business_functionality
    echo ""

    # Check resource usage
    print_status "=== Resource Usage Check ==="
    check_resource_usage
    echo ""

    # Generate report
    if [ $validation_failed -eq 0 ]; then
        print_success "🎉 All validation checks passed!"
        generate_validation_report "✅ PASS"
    else
        print_error "❌ Some validation checks failed. Please review the issues above."
        generate_validation_report "❌ FAIL"
        exit 1
    fi
}

# Execute main function
main "$@"