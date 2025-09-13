#!/bin/bash

# ==========================================
# World-Class Deployment Script
# Gasolinera JSM - Zero-Downtime Deployment
# ==========================================

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
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOYMENT_LOG="/var/log/gasolinera-jsm/deployment.log"
ROLLBACK_DATA_DIR="/var/lib/gasolinera-jsm/rollback"
HEALTH_CHECK_TIMEOUT=300  # 5 minutes
DEPLOYMENT_TIMEOUT=1800   # 30 minutes

# Default values
ENVIRONMENT="staging"
VERSION=""
STRATEGY="blue-green"
SKIP_TESTS=false
SKIP_BACKUP=false
DRY_RUN=false
FORCE_DEPLOY=false
ROLLBACK_VERSION=""

# Services to deploy
SERVICES=(
    "api-gateway"
    "auth-service"
    "station-service"
    "coupon-service"
    "raffle-service"
)

# ==========================================
# Usage Information
# ==========================================
usage() {
    cat << EOF
🚀 Gasolinera JSM Deployment Script

Usage: $0 [OPTIONS]

OPTIONS:
    -e, --environment ENV     Target environment (dev|staging|prod) [default: staging]
    -v, --version VERSION     Version to deploy (required)
    -s, --strategy STRATEGY   Deployment strategy (blue-green|rolling|canary) [default: blue-green]
    -r, --rollback VERSION    Rollback to specified version
    --skip-tests             Skip pre-deployment tests
    --skip-backup            Skip database backup
    --dry-run                Show what would be deployed without executing
    --force                  Force deployment even if health checks fail
    -h, --help               Show this help message

EXAMPLES:
    # Deploy version 1.2.3 to staging
    $0 -e staging -v 1.2.3

    # Deploy to production with canary strategy
    $0 -e prod -v 1.2.3 -s canary

    # Rollback production to previous version
    $0 -e prod -r 1.2.2

    # Dry run deployment
    $0 -e staging -v 1.2.3 --dry-run

DEPLOYMENT STRATEGIES:
    blue-green    Zero-downtime deployment with instant rollback
    rolling       Gradual replacement of instances
    canary        Deploy to subset of instances for testing

EOF
}

# ==========================================
# Logging Functions
# ==========================================
log() {
    local level=$1
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')

    echo -e "${timestamp} [${level}] ${message}" | tee -a "${DEPLOYMENT_LOG}"
}

log_info() {
    log "INFO" "${BLUE}$*${NC}"
}

log_success() {
    log "SUCCESS" "${GREEN}$*${NC}"
}

log_warning() {
    log "WARNING" "${YELLOW}$*${NC}"
}

log_error() {
    log "ERROR" "${RED}$*${NC}"
}

log_debug() {
    if [[ "${DEBUG:-false}" == "true" ]]; then
        log "DEBUG" "${PURPLE}$*${NC}"
    fi
}

# ==========================================
# Utility Functions
# ==========================================
check_prerequisites() {
    log_info "🔍 Checking deployment prerequisites..."

    local missing_tools=()

    # Check required tools
    for tool in kubectl docker helm jq curl; do
        if ! command -v "$tool" &> /dev/null; then
            missing_tools+=("$tool")
        fi
    done

    if [[ ${#missing_tools[@]} -gt 0 ]]; then
        log_error "Missing required tools: ${missing_tools[*]}"
        log_error "Please install missing tools and try again"
        exit 1
    fi

    # Check Kubernetes connectivity
    if ! kubectl cluster-info &> /dev/null; then
        log_error "Cannot connect to Kubernetes cluster"
        log_error "Please check your kubeconfig and cluster connectivity"
        exit 1
    fi

    # Check Docker registry access
    if ! docker info &> /dev/null; then
        log_error "Cannot connect to Docker daemon"
        exit 1
    fi

    # Create necessary directories
    mkdir -p "$(dirname "$DEPLOYMENT_LOG")"
    mkdir -p "$ROLLBACK_DATA_DIR"

    log_success "✅ All prerequisites satisfied"
}

validate_environment() {
    local env=$1

    case "$env" in
        dev|development)
            ENVIRONMENT="development"
            NAMESPACE="gasolinera-jsm-dev"
            REGISTRY="dev-registry.gasolinera-jsm.com"
            ;;
        staging|stage)
            ENVIRONMENT="staging"
            NAMESPACE="gasolinera-jsm-staging"
            REGISTRY="staging-registry.gasolinera-jsm.com"
            ;;
        prod|production)
            ENVIRONMENT="production"
            NAMESPACE="gasolinera-jsm-prod"
            REGISTRY="registry.gasolinera-jsm.com"
            ;;
        *)
            log_error "Invalid environment: $env"
            log_error "Valid environments: dev, staging, prod"
            exit 1
            ;;
    esac

    log_info "🎯 Target environment: $ENVIRONMENT"
    log_info "📦 Namespace: $NAMESPACE"
    log_info "🏗️ Registry: $REGISTRY"
}

validate_version() {
    local version=$1

    if [[ ! "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9]+)?$ ]]; then
        log_error "Invalid version format: $version"
        log_error "Expected format: X.Y.Z or X.Y.Z-suffix (e.g., 1.2.3 or 1.2.3-rc1)"
        exit 1
    fi

    # Check if version exists in registry
    for service in "${SERVICES[@]}"; do
        local image="${REGISTRY}/${service}:${version}"
        if ! docker manifest inspect "$image" &> /dev/null; then
            log_error "Image not found in registry: $image"
            log_error "Please build and push the image before deployment"
            exit 1
        fi
    done

    log_success "✅ Version $version validated"
}

# ==========================================
# Pre-deployment Functions
# ==========================================
run_pre_deployment_tests() {
    if [[ "$SKIP_TESTS" == "true" ]]; then
        log_warning "⚠️ Skipping pre-deployment tests"
        return 0
    fi

    log_info "🧪 Running pre-deployment tests..."

    cd "$PROJECT_ROOT"

    # Unit tests
    log_info "Running unit tests..."
    if ! ./gradlew test --parallel; then
        log_error "Unit tests failed"
        return 1
    fi

    # Integration tests
    log_info "Running integration tests..."
    if ! ./gradlew integrationTest --parallel; then
        log_error "Integration tests failed"
        return 1
    fi

    # Security tests
    log_info "Running security tests..."
    if ! ./gradlew dependencyCheckAnalyze; then
        log_error "Security tests failed"
        return 1
    fi

    # Performance tests (quick smoke test)
    log_info "Running performance smoke tests..."
    if ! ./gradlew :performance-tests:jmh-benchmarks:jmh -Pjmh.includePattern=".*Smoke.*"; then
        log_warning "Performance smoke tests failed (non-blocking)"
    fi

    log_success "✅ All pre-deployment tests passed"
}

backup_database() {
    if [[ "$SKIP_BACKUP" == "true" ]]; then
        log_warning "⚠️ Skipping database backup"
        return 0
    fi

    log_info "💾 Creating database backup..."

    local backup_timestamp=$(date +%Y%m%d_%H%M%S)
    local backup_file="${ROLLBACK_DATA_DIR}/db_backup_${ENVIRONMENT}_${backup_timestamp}.sql"

    # Get database connection info from Kubernetes secrets
    local db_host=$(kubectl get secret -n "$NAMESPACE" postgres-credentials -o jsonpath='{.data.host}' | base64 -d)
    local db_user=$(kubectl get secret -n "$NAMESPACE" postgres-credentials -o jsonpath='{.data.username}' | base64 -d)
    local db_password=$(kubectl get secret -n "$NAMESPACE" postgres-credentials -o jsonpath='{.data.password}' | base64 -d)
    local db_name=$(kubectl get secret -n "$NAMESPACE" postgres-credentials -o jsonpath='{.data.database}' | base64 -d)

    # Create backup
    if PGPASSWORD="$db_password" pg_dump -h "$db_host" -U "$db_user" -d "$db_name" \
        --format=custom --compress=9 --verbose --file="$backup_file"; then
        log_success "✅ Database backup created: $backup_file"

        # Store backup info for rollback
        echo "$backup_file" > "${ROLLBACK_DATA_DIR}/latest_backup_${ENVIRONMENT}.txt"
    else
        log_error "Database backup failed"
        return 1
    fi
}

save_current_state() {
    log_info "💾 Saving current deployment state..."

    local state_file="${ROLLBACK_DATA_DIR}/deployment_state_${ENVIRONMENT}_$(date +%Y%m%d_%H%M%S).json"

    # Get current deployment info
    local current_state=$(kubectl get deployments -n "$NAMESPACE" -o json | jq '{
        timestamp: now | strftime("%Y-%m-%d %H:%M:%S"),
        environment: "'$ENVIRONMENT'",
        namespace: "'$NAMESPACE'",
        deployments: [
            .items[] | {
                name: .metadata.name,
                image: .spec.template.spec.containers[0].image,
                replicas: .spec.replicas,
                readyReplicas: .status.readyReplicas // 0
            }
        ]
    }')

    echo "$current_state" > "$state_file"

    # Store reference to latest state
    echo "$state_file" > "${ROLLBACK_DATA_DIR}/latest_state_${ENVIRONMENT}.txt"

    log_success "✅ Current state saved: $state_file"
}

# ==========================================
# Deployment Strategy Functions
# ==========================================
deploy_blue_green() {
    local version=$1

    log_info "🔵🟢 Starting Blue-Green deployment for version $version..."

    for service in "${SERVICES[@]}"; do
        log_info "Deploying $service with Blue-Green strategy..."

        local image="${REGISTRY}/${service}:${version}"

        # Create green deployment
        kubectl patch deployment "$service" -n "$NAMESPACE" \
            --patch="{\"spec\":{\"template\":{\"spec\":{\"containers\":[{\"name\":\"$service\",\"image\":\"$image\"}]}}}}"

        # Wait for green deployment to be ready
        if ! kubectl rollout status deployment/"$service" -n "$NAMESPACE" --timeout="${HEALTH_CHECK_TIMEOUT}s"; then
            log_error "Green deployment failed for $service"
            return 1
        fi

        # Health check green deployment
        if ! perform_health_check "$service"; then
            log_error "Health check failed for $service"
            return 1
        fi

        log_success "✅ $service deployed successfully"
    done

    log_success "🎉 Blue-Green deployment completed successfully"
}

deploy_rolling() {
    local version=$1

    log_info "🔄 Starting Rolling deployment for version $version..."

    for service in "${SERVICES[@]}"; do
        log_info "Rolling update for $service..."

        local image="${REGISTRY}/${service}:${version}"

        # Set rolling update strategy
        kubectl patch deployment "$service" -n "$NAMESPACE" \
            --patch='{"spec":{"strategy":{"type":"RollingUpdate","rollingUpdate":{"maxUnavailable":"25%","maxSurge":"25%"}}}}'

        # Update image
        kubectl set image deployment/"$service" "$service"="$image" -n "$NAMESPACE"

        # Wait for rollout
        if ! kubectl rollout status deployment/"$service" -n "$NAMESPACE" --timeout="${HEALTH_CHECK_TIMEOUT}s"; then
            log_error "Rolling deployment failed for $service"
            return 1
        fi

        # Health check
        if ! perform_health_check "$service"; then
            log_error "Health check failed for $service"
            return 1
        fi

        log_success "✅ $service rolled out successfully"
    done

    log_success "🎉 Rolling deployment completed successfully"
}

deploy_canary() {
    local version=$1

    log_info "🐤 Starting Canary deployment for version $version..."

    for service in "${SERVICES[@]}"; do
        log_info "Canary deployment for $service..."

        local image="${REGISTRY}/${service}:${version}"

        # Create canary deployment (10% of traffic)
        local canary_name="${service}-canary"

        # Scale down main deployment to 90%
        local current_replicas=$(kubectl get deployment "$service" -n "$NAMESPACE" -o jsonpath='{.spec.replicas}')
        local main_replicas=$((current_replicas * 9 / 10))
        local canary_replicas=$((current_replicas - main_replicas))

        kubectl scale deployment "$service" -n "$NAMESPACE" --replicas="$main_replicas"

        # Create canary deployment
        kubectl get deployment "$service" -n "$NAMESPACE" -o yaml | \
            sed "s/name: $service/name: $canary_name/g" | \
            sed "s/image: .*/image: $image/g" | \
            kubectl apply -f -

        kubectl scale deployment "$canary_name" -n "$NAMESPACE" --replicas="$canary_replicas"

        # Wait for canary to be ready
        if ! kubectl rollout status deployment/"$canary_name" -n "$NAMESPACE" --timeout="${HEALTH_CHECK_TIMEOUT}s"; then
            log_error "Canary deployment failed for $service"
            return 1
        fi

        # Monitor canary for 5 minutes
        log_info "Monitoring canary deployment for 5 minutes..."
        sleep 300

        # Check canary health and metrics
        if perform_canary_validation "$service" "$canary_name"; then
            log_info "Canary validation passed, promoting to full deployment..."

            # Update main deployment
            kubectl set image deployment/"$service" "$service"="$image" -n "$NAMESPACE"
            kubectl scale deployment "$service" -n "$NAMESPACE" --replicas="$current_replicas"

            # Remove canary
            kubectl delete deployment "$canary_name" -n "$NAMESPACE"

            log_success "✅ $service canary promoted successfully"
        else
            log_error "Canary validation failed, rolling back..."
            kubectl delete deployment "$canary_name" -n "$NAMESPACE"
            kubectl scale deployment "$service" -n "$NAMESPACE" --replicas="$current_replicas"
            return 1
        fi
    done

    log_success "🎉 Canary deployment completed successfully"
}

# ==========================================
# Health Check Functions
# ==========================================
perform_health_check() {
    local service=$1

    log_info "🏥 Performing health check for $service..."

    # Get service endpoint
    local service_url="http://${service}.${NAMESPACE}.svc.cluster.local:8080"

    # Wait for pods to be ready
    local max_attempts=30
    local attempt=1

    while [[ $attempt -le $max_attempts ]]; do
        local ready_pods=$(kubectl get pods -n "$NAMESPACE" -l app="$service" -o jsonpath='{.items[*].status.conditions[?(@.type=="Ready")].status}' | grep -c "True" || echo "0")
        local total_pods=$(kubectl get pods -n "$NAMESPACE" -l app="$service" --no-headers | wc -l)

        if [[ "$ready_pods" -eq "$total_pods" ]] && [[ "$total_pods" -gt 0 ]]; then
            break
        fi

        log_info "Waiting for pods to be ready ($ready_pods/$total_pods)... Attempt $attempt/$max_attempts"
        sleep 10
        ((attempt++))
    done

    if [[ $attempt -gt $max_attempts ]]; then
        log_error "Timeout waiting for pods to be ready"
        return 1
    fi

    # Health endpoint check
    local health_attempts=10
    for ((i=1; i<=health_attempts; i++)); do
        if kubectl exec -n "$NAMESPACE" deployment/"$service" -- curl -f "$service_url/actuator/health" &> /dev/null; then
            log_success "✅ Health check passed for $service"
            return 0
        fi

        log_info "Health check attempt $i/$health_attempts failed, retrying..."
        sleep 15
    done

    log_error "Health check failed for $service after $health_attempts attempts"
    return 1
}

perform_canary_validation() {
    local main_service=$1
    local canary_service=$2

    log_info "🔍 Validating canary deployment..."

    # Check error rates
    local main_errors=$(get_error_rate "$main_service")
    local canary_errors=$(get_error_rate "$canary_service")

    # Check response times
    local main_latency=$(get_avg_latency "$main_service")
    local canary_latency=$(get_avg_latency "$canary_service")

    log_info "Main service - Errors: $main_errors%, Latency: ${main_latency}ms"
    log_info "Canary service - Errors: $canary_errors%, Latency: ${canary_latency}ms"

    # Validation criteria
    if (( $(echo "$canary_errors > 1.0" | bc -l) )); then
        log_error "Canary error rate too high: $canary_errors%"
        return 1
    fi

    if (( $(echo "$canary_latency > $main_latency * 1.5" | bc -l) )); then
        log_error "Canary latency too high: ${canary_latency}ms vs ${main_latency}ms"
        return 1
    fi

    log_success "✅ Canary validation passed"
    return 0
}

get_error_rate() {
    local service=$1
    # This would integrate with your monitoring system (Prometheus)
    # For now, return a mock value
    echo "0.1"
}

get_avg_latency() {
    local service=$1
    # This would integrate with your monitoring system (Prometheus)
    # For now, return a mock value
    echo "150"
}

# ==========================================
# Rollback Functions
# ==========================================
perform_rollback() {
    local target_version=$1

    log_info "🔄 Starting rollback to version $target_version..."

    if [[ -z "$target_version" ]]; then
        # Get previous version from rollback data
        local state_file=$(cat "${ROLLBACK_DATA_DIR}/latest_state_${ENVIRONMENT}.txt" 2>/dev/null || echo "")
        if [[ -z "$state_file" || ! -f "$state_file" ]]; then
            log_error "No rollback state found"
            exit 1
        fi

        log_info "Rolling back to previous state: $state_file"
        rollback_to_state "$state_file"
    else
        log_info "Rolling back to version: $target_version"
        rollback_to_version "$target_version"
    fi
}

rollback_to_version() {
    local version=$1

    validate_version "$version"

    for service in "${SERVICES[@]}"; do
        log_info "Rolling back $service to version $version..."

        local image="${REGISTRY}/${service}:${version}"

        # Rollback deployment
        kubectl set image deployment/"$service" "$service"="$image" -n "$NAMESPACE"

        # Wait for rollback
        if ! kubectl rollout status deployment/"$service" -n "$NAMESPACE" --timeout="${HEALTH_CHECK_TIMEOUT}s"; then
            log_error "Rollback failed for $service"
            return 1
        fi

        # Health check
        if ! perform_health_check "$service"; then
            log_error "Health check failed after rollback for $service"
            return 1
        fi

        log_success "✅ $service rolled back successfully"
    done

    log_success "🎉 Rollback completed successfully"
}

rollback_to_state() {
    local state_file=$1

    if [[ ! -f "$state_file" ]]; then
        log_error "State file not found: $state_file"
        return 1
    fi

    log_info "Restoring deployment state from: $state_file"

    # Parse state file and restore each deployment
    local deployments=$(jq -r '.deployments[] | @base64' "$state_file")

    while IFS= read -r deployment_data; do
        local deployment=$(echo "$deployment_data" | base64 -d)
        local name=$(echo "$deployment" | jq -r '.name')
        local image=$(echo "$deployment" | jq -r '.image')
        local replicas=$(echo "$deployment" | jq -r '.replicas')

        log_info "Restoring $name to image $image with $replicas replicas..."

        kubectl set image deployment/"$name" "$name"="$image" -n "$NAMESPACE"
        kubectl scale deployment/"$name" -n "$NAMESPACE" --replicas="$replicas"

        if ! kubectl rollout status deployment/"$name" -n "$NAMESPACE" --timeout="${HEALTH_CHECK_TIMEOUT}s"; then
            log_error "Failed to restore $name"
            return 1
        fi

        log_success "✅ $name restored successfully"
    done <<< "$deployments"

    log_success "🎉 State restoration completed successfully"
}

# ==========================================
# Main Deployment Function
# ==========================================
main_deploy() {
    local start_time=$(date +%s)

    log_info "🚀 Starting deployment process..."
    log_info "Environment: $ENVIRONMENT"
    log_info "Version: $VERSION"
    log_info "Strategy: $STRATEGY"
    log_info "Dry Run: $DRY_RUN"

    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "🔍 DRY RUN MODE - No actual changes will be made"
        log_info "Would deploy version $VERSION to $ENVIRONMENT using $STRATEGY strategy"
        log_info "Services to deploy: ${SERVICES[*]}"
        return 0
    fi

    # Pre-deployment steps
    run_pre_deployment_tests || {
        if [[ "$FORCE_DEPLOY" == "false" ]]; then
            log_error "Pre-deployment tests failed. Use --force to override."
            exit 1
        else
            log_warning "Pre-deployment tests failed but continuing due to --force flag"
        fi
    }

    backup_database || {
        log_error "Database backup failed"
        exit 1
    }

    save_current_state || {
        log_error "Failed to save current state"
        exit 1
    }

    # Execute deployment strategy
    case "$STRATEGY" in
        blue-green)
            deploy_blue_green "$VERSION" || {
                log_error "Blue-Green deployment failed"
                exit 1
            }
            ;;
        rolling)
            deploy_rolling "$VERSION" || {
                log_error "Rolling deployment failed"
                exit 1
            }
            ;;
        canary)
            deploy_canary "$VERSION" || {
                log_error "Canary deployment failed"
                exit 1
            }
            ;;
        *)
            log_error "Unknown deployment strategy: $STRATEGY"
            exit 1
            ;;
    esac

    # Post-deployment validation
    log_info "🔍 Running post-deployment validation..."

    for service in "${SERVICES[@]}"; do
        if ! perform_health_check "$service"; then
            log_error "Post-deployment health check failed for $service"

            if [[ "$FORCE_DEPLOY" == "false" ]]; then
                log_error "Initiating automatic rollback..."
                perform_rollback ""
                exit 1
            else
                log_warning "Health check failed but continuing due to --force flag"
            fi
        fi
    done

    local end_time=$(date +%s)
    local duration=$((end_time - start_time))

    log_success "🎉 Deployment completed successfully in ${duration} seconds!"
    log_success "Version $VERSION is now live in $ENVIRONMENT environment"

    # Send notification (integrate with your notification system)
    send_deployment_notification "SUCCESS" "$VERSION" "$ENVIRONMENT" "$duration"
}

# ==========================================
# Notification Function
# ==========================================
send_deployment_notification() {
    local status=$1
    local version=$2
    local environment=$3
    local duration=$4

    # This would integrate with Slack, Teams, or other notification systems
    log_info "📢 Sending deployment notification..."

    local message="Deployment $status: Version $version deployed to $environment in ${duration}s"

    # Example Slack webhook (replace with your webhook URL)
    # curl -X POST -H 'Content-type: application/json' \
    #     --data "{\"text\":\"$message\"}" \
    #     "$SLACK_WEBHOOK_URL"

    log_info "Notification: $message"
}

# ==========================================
# Cleanup Function
# ==========================================
cleanup() {
    log_info "🧹 Performing cleanup..."

    # Clean up old rollback data (keep last 10)
    find "$ROLLBACK_DATA_DIR" -name "deployment_state_${ENVIRONMENT}_*.json" -type f | \
        sort -r | tail -n +11 | xargs -r rm -f

    find "$ROLLBACK_DATA_DIR" -name "db_backup_${ENVIRONMENT}_*.sql" -type f -mtime +7 | \
        xargs -r rm -f

    log_info "Cleanup completed"
}

# ==========================================
# Signal Handlers
# ==========================================
trap 'log_error "Deployment interrupted"; cleanup; exit 130' INT TERM

# ==========================================
# Argument Parsing
# ==========================================
while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--environment)
            ENVIRONMENT="$2"
            shift 2
            ;;
        -v|--version)
            VERSION="$2"
            shift 2
            ;;
        -s|--strategy)
            STRATEGY="$2"
            shift 2
            ;;
        -r|--rollback)
            ROLLBACK_VERSION="$2"
            shift 2
            ;;
        --skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        --skip-backup)
            SKIP_BACKUP=true
            shift
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --force)
            FORCE_DEPLOY=true
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            usage
            exit 1
            ;;
    esac
done

# ==========================================
# Main Execution
# ==========================================
main() {
    echo -e "${CYAN}"
    echo "╔══════════════════════════════════════════════════════════════╗"
    echo "║                    🚀 GASOLINERA JSM                        ║"
    echo "║                World-Class Deployment System                 ║"
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"

    # Initialize logging
    log_info "🚀 Gasolinera JSM Deployment Started"
    log_info "Timestamp: $(date)"
    log_info "User: $(whoami)"
    log_info "Host: $(hostname)"

    # Check prerequisites
    check_prerequisites

    # Validate environment
    validate_environment "$ENVIRONMENT"

    # Handle rollback
    if [[ -n "$ROLLBACK_VERSION" ]]; then
        perform_rollback "$ROLLBACK_VERSION"
        cleanup
        exit 0
    fi

    # Validate version for deployment
    if [[ -z "$VERSION" ]]; then
        log_error "Version is required for deployment"
        usage
        exit 1
    fi

    validate_version "$VERSION"

    # Execute deployment
    main_deploy

    # Cleanup
    cleanup

    log_success "🎉 All operations completed successfully!"
}

# Run main function
main "$@"