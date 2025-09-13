#!/bin/bash

# ==========================================
# World-Class Rollback Script
# Gasolinera JSM - Emergency Rollback System
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
ROLLBACK_LOG="/var/log/gasolinera-jsm/rollback.log"
ROLLBACK_DATA_DIR="/var/lib/gasolinera-jsm/rollback"
EMERGENCY_CONTACT="devops@gasolinera-jsm.com"
SLACK_WEBHOOK_URL="${SLACK_WEBHOOK_URL:-}"

# Default values
ENVIRONMENT=""
TARGET_VERSION=""
ROLLBACK_TYPE="deployment"  # deployment, database, full
EMERGENCY_MODE=false
SKIP_VALIDATION=false
DRY_RUN=false
AUTO_CONFIRM=false

# Services that can be rolled back
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
🔄 Gasolinera JSM Emergency Rollback System

Usage: $0 [OPTIONS]

OPTIONS:
    -e, --environment ENV     Target environment (dev|staging|prod) [REQUIRED]
    -v, --version VERSION     Specific version to rollback to
    -t, --type TYPE          Rollback type (deployment|database|full) [default: deployment]
    --emergency              Emergency mode - fastest rollback possible
    --skip-validation        Skip pre-rollback validation (dangerous)
    --dry-run                Show what would be rolled back without executing
    --auto-confirm           Skip confirmation prompts (for automation)
    -h, --help               Show this help message

ROLLBACK TYPES:
    deployment    Rollback application deployments only
    database      Rollback database to previous backup
    full          Complete rollback (deployments + database)

EXAMPLES:
    # Emergency rollback to last known good state
    $0 -e prod --emergency

    # Rollback to specific version
    $0 -e prod -v 1.2.2

    # Database-only rollback
    $0 -e staging -t database

    # Full system rollback with confirmation
    $0 -e prod -t full -v 1.2.2

    # Dry run to see what would be rolled back
    $0 -e prod --dry-run

EMERGENCY PROCEDURES:
    For critical production issues:
    1. Run: $0 -e prod --emergency --auto-confirm
    2. Contact: $EMERGENCY_CONTACT
    3. Create incident report

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

    echo -e "${timestamp} [${level}] ${message}" | tee -a "${ROLLBACK_LOG}"
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

log_emergency() {
    log "EMERGENCY" "${RED}🚨 $*${NC}"
}

# ==========================================
# Emergency Notification Functions
# ==========================================
send_emergency_alert() {
    local message=$1
    local severity=${2:-"HIGH"}

    log_emergency "$message"

    # Send Slack notification if webhook is configured
    if [[ -n "$SLACK_WEBHOOK_URL" ]]; then
        local payload=$(cat << EOF
{
    "text": "🚨 EMERGENCY ROLLBACK ALERT",
    "attachments": [
        {
            "color": "danger",
            "fields": [
                {
                    "title": "Environment",
                    "value": "$ENVIRONMENT",
                    "short": true
                },
                {
                    "title": "Severity",
                    "value": "$severity",
                    "short": true
                },
                {
                    "title": "Message",
                    "value": "$message",
                    "short": false
                },
                {
                    "title": "Timestamp",
                    "value": "$(date)",
                    "short": true
                },
                {
                    "title": "Operator",
                    "value": "$(whoami)@$(hostname)",
                    "short": true
                }
            ]
        }
    ]
}
EOF
        )

        curl -X POST -H 'Content-type: application/json' \
            --data "$payload" \
            "$SLACK_WEBHOOK_URL" &> /dev/null || true
    fi

    # Send email alert (if mail is configured)
    if command -v mail &> /dev/null; then
        echo "$message" | mail -s "🚨 EMERGENCY ROLLBACK: $ENVIRONMENT" "$EMERGENCY_CONTACT" || true
    fi
}

# ==========================================
# Validation Functions
# ==========================================
validate_environment() {
    local env=$1

    case "$env" in
        dev|development)
            ENVIRONMENT="development"
            NAMESPACE="gasolinera-jsm-dev"
            ;;
        staging|stage)
            ENVIRONMENT="staging"
            NAMESPACE="gasolinera-jsm-staging"
            ;;
        prod|production)
            ENVIRONMENT="production"
            NAMESPACE="gasolinera-jsm-prod"
            ;;
        *)
            log_error "Invalid environment: $env"
            exit 1
            ;;
    esac

    log_info "🎯 Target environment: $ENVIRONMENT"
    log_info "📦 Namespace: $NAMESPACE"
}

check_rollback_prerequisites() {
    log_info "🔍 Checking rollback prerequisites..."

    # Check kubectl connectivity
    if ! kubectl cluster-info &> /dev/null; then
        log_error "Cannot connect to Kubernetes cluster"
        exit 1
    fi

    # Check namespace exists
    if ! kubectl get namespace "$NAMESPACE" &> /dev/null; then
        log_error "Namespace $NAMESPACE does not exist"
        exit 1
    fi

    # Check rollback data directory
    if [[ ! -d "$ROLLBACK_DATA_DIR" ]]; then
        log_error "Rollback data directory not found: $ROLLBACK_DATA_DIR"
        log_error "No rollback data available"
        exit 1
    fi

    # Create log directory if needed
    mkdir -p "$(dirname "$ROLLBACK_LOG")"

    log_success "✅ Prerequisites check passed"
}

get_available_rollback_targets() {
    log_info "📋 Scanning available rollback targets..."

    local state_files=()
    local backup_files=()

    # Find deployment state files
    while IFS= read -r -d '' file; do
        state_files+=("$file")
    done < <(find "$ROLLBACK_DATA_DIR" -name "deployment_state_${ENVIRONMENT}_*.json" -print0 2>/dev/null | sort -z -r)

    # Find database backup files
    while IFS= read -r -d '' file; do
        backup_files+=("$file")
    done < <(find "$ROLLBACK_DATA_DIR" -name "db_backup_${ENVIRONMENT}_*.sql" -print0 2>/dev/null | sort -z -r)

    log_info "Available deployment states: ${#state_files[@]}"
    log_info "Available database backups: ${#backup_files[@]}"

    if [[ ${#state_files[@]} -eq 0 && ${#backup_files[@]} -eq 0 ]]; then
        log_error "No rollback targets available for environment: $ENVIRONMENT"
        exit 1
    fi

    # Display available targets
    if [[ ${#state_files[@]} -gt 0 ]]; then
        log_info "Recent deployment states:"
        for i in "${!state_files[@]}"; do
            if [[ $i -lt 5 ]]; then  # Show only last 5
                local file="${state_files[$i]}"
                local timestamp=$(basename "$file" | sed 's/deployment_state_.*_\(.*\)\.json/\1/')
                local readable_time=$(date -d "${timestamp:0:8} ${timestamp:9:2}:${timestamp:11:2}:${timestamp:13:2}" 2>/dev/null || echo "$timestamp")
                log_info "  $((i+1)). $readable_time - $(basename "$file")"
            fi
        done
    fi

    if [[ ${#backup_files[@]} -gt 0 ]]; then
        log_info "Recent database backups:"
        for i in "${!backup_files[@]}"; do
            if [[ $i -lt 5 ]]; then  # Show only last 5
                local file="${backup_files[$i]}"
                local timestamp=$(basename "$file" | sed 's/db_backup_.*_\(.*\)\.sql/\1/')
                local readable_time=$(date -d "${timestamp:0:8} ${timestamp:9:2}:${timestamp:11:2}:${timestamp:13:2}" 2>/dev/null || echo "$timestamp")
                local size=$(du -h "$file" | cut -f1)
                log_info "  $((i+1)). $readable_time - $(basename "$file") ($size)"
            fi
        done
    fi
}

# ==========================================
# Rollback Strategy Functions
# ==========================================
emergency_rollback() {
    log_emergency "🚨 INITIATING EMERGENCY ROLLBACK"
    send_emergency_alert "Emergency rollback initiated for $ENVIRONMENT environment" "CRITICAL"

    # Get the most recent known good state
    local latest_state_file="${ROLLBACK_DATA_DIR}/latest_state_${ENVIRONMENT}.txt"

    if [[ ! -f "$latest_state_file" ]]; then
        log_error "No emergency rollback state found"
        exit 1
    fi

    local state_file=$(cat "$latest_state_file")

    if [[ ! -f "$state_file" ]]; then
        log_error "Emergency rollback state file not found: $state_file"
        exit 1
    fi

    log_emergency "Rolling back to emergency state: $(basename "$state_file")"

    # Perform fastest possible rollback
    rollback_deployments_from_state "$state_file" true

    # Quick health check
    perform_emergency_health_check

    send_emergency_alert "Emergency rollback completed for $ENVIRONMENT environment" "HIGH"
    log_success "🎉 Emergency rollback completed"
}

rollback_deployments() {
    local target_version=$1
    local emergency_mode=${2:-false}

    if [[ "$emergency_mode" == "true" ]]; then
        log_emergency "🚨 EMERGENCY DEPLOYMENT ROLLBACK"
    else
        log_info "🔄 Starting deployment rollback..."
    fi

    if [[ -n "$target_version" ]]; then
        rollback_to_specific_version "$target_version" "$emergency_mode"
    else
        rollback_to_previous_state "$emergency_mode"
    fi
}

rollback_to_specific_version() {
    local version=$1
    local emergency_mode=${2:-false}

    log_info "Rolling back to version: $version"

    # Validate version exists in registry
    local registry="registry.gasolinera-jsm.com"
    if [[ "$ENVIRONMENT" != "production" ]]; then
        registry="${ENVIRONMENT}-registry.gasolinera-jsm.com"
    fi

    for service in "${SERVICES[@]}"; do
        local image="${registry}/${service}:${version}"

        if [[ "$emergency_mode" == "false" ]] && [[ "$SKIP_VALIDATION" == "false" ]]; then
            if ! docker manifest inspect "$image" &> /dev/null; then
                log_error "Image not found: $image"
                exit 1
            fi
        fi

        log_info "Rolling back $service to $version..."

        # Set image
        kubectl set image deployment/"$service" "$service"="$image" -n "$NAMESPACE"

        if [[ "$emergency_mode" == "true" ]]; then
            # In emergency mode, don't wait for graceful rollout
            kubectl patch deployment "$service" -n "$NAMESPACE" \
                --patch='{"spec":{"strategy":{"type":"Recreate"}}}'
        fi

        # Wait for rollout
        local timeout=300
        if [[ "$emergency_mode" == "true" ]]; then
            timeout=60  # Shorter timeout in emergency
        fi

        if ! kubectl rollout status deployment/"$service" -n "$NAMESPACE" --timeout="${timeout}s"; then
            if [[ "$emergency_mode" == "true" ]]; then
                log_warning "⚠️ Rollout timeout for $service in emergency mode - continuing"
            else
                log_error "Rollback failed for $service"
                exit 1
            fi
        fi

        log_success "✅ $service rolled back to $version"
    done
}

rollback_to_previous_state() {
    local emergency_mode=${1:-false}

    local latest_state_file="${ROLLBACK_DATA_DIR}/latest_state_${ENVIRONMENT}.txt"

    if [[ ! -f "$latest_state_file" ]]; then
        log_error "No previous state found for rollback"
        exit 1
    fi

    local state_file=$(cat "$latest_state_file")
    rollback_deployments_from_state "$state_file" "$emergency_mode"
}

rollback_deployments_from_state() {
    local state_file=$1
    local emergency_mode=${2:-false}

    if [[ ! -f "$state_file" ]]; then
        log_error "State file not found: $state_file"
        exit 1
    fi

    log_info "Restoring deployment state from: $(basename "$state_file")"

    # Parse and restore each deployment
    local deployments=$(jq -r '.deployments[] | @base64' "$state_file")

    while IFS= read -r deployment_data; do
        local deployment=$(echo "$deployment_data" | base64 -d)
        local name=$(echo "$deployment" | jq -r '.name')
        local image=$(echo "$deployment" | jq -r '.image')
        local replicas=$(echo "$deployment" | jq -r '.replicas')

        log_info "Restoring $name to $image ($replicas replicas)..."

        # Set image and scale
        kubectl set image deployment/"$name" "$name"="$image" -n "$NAMESPACE"
        kubectl scale deployment/"$name" -n "$NAMESPACE" --replicas="$replicas"

        if [[ "$emergency_mode" == "true" ]]; then
            # Force recreate in emergency mode
            kubectl patch deployment "$name" -n "$NAMESPACE" \
                --patch='{"spec":{"strategy":{"type":"Recreate"}}}'
        fi

        # Wait for rollout
        local timeout=300
        if [[ "$emergency_mode" == "true" ]]; then
            timeout=60
        fi

        if ! kubectl rollout status deployment/"$name" -n "$NAMESPACE" --timeout="${timeout}s"; then
            if [[ "$emergency_mode" == "true" ]]; then
                log_warning "⚠️ Rollout timeout for $name in emergency mode - continuing"
            else
                log_error "Failed to restore $name"
                exit 1
            fi
        fi

        log_success "✅ $name restored successfully"
    done <<< "$deployments"
}

rollback_database() {
    log_info "💾 Starting database rollback..."

    # Get latest backup
    local latest_backup_file="${ROLLBACK_DATA_DIR}/latest_backup_${ENVIRONMENT}.txt"

    if [[ ! -f "$latest_backup_file" ]]; then
        log_error "No database backup found for rollback"
        exit 1
    fi

    local backup_file=$(cat "$latest_backup_file")

    if [[ ! -f "$backup_file" ]]; then
        log_error "Database backup file not found: $backup_file"
        exit 1
    fi

    log_warning "⚠️ DATABASE ROLLBACK IS DESTRUCTIVE"
    log_warning "This will restore database to: $(basename "$backup_file")"

    if [[ "$AUTO_CONFIRM" == "false" ]]; then
        read -p "Are you sure you want to proceed? (type 'YES' to confirm): " confirmation
        if [[ "$confirmation" != "YES" ]]; then
            log_info "Database rollback cancelled"
            return 0
        fi
    fi

    # Get database credentials
    local db_host=$(kubectl get secret -n "$NAMESPACE" postgres-credentials -o jsonpath='{.data.host}' | base64 -d)
    local db_user=$(kubectl get secret -n "$NAMESPACE" postgres-credentials -o jsonpath='{.data.username}' | base64 -d)
    local db_password=$(kubectl get secret -n "$NAMESPACE" postgres-credentials -o jsonpath='{.data.password}' | base64 -d)
    local db_name=$(kubectl get secret -n "$NAMESPACE" postgres-credentials -o jsonpath='{.data.database}' | base64 -d)

    # Create current backup before rollback
    local pre_rollback_backup="${ROLLBACK_DATA_DIR}/pre_rollback_backup_${ENVIRONMENT}_$(date +%Y%m%d_%H%M%S).sql"
    log_info "Creating pre-rollback backup..."

    if PGPASSWORD="$db_password" pg_dump -h "$db_host" -U "$db_user" -d "$db_name" \
        --format=custom --compress=9 --file="$pre_rollback_backup"; then
        log_success "✅ Pre-rollback backup created: $(basename "$pre_rollback_backup")"
    else
        log_error "Failed to create pre-rollback backup"
        exit 1
    fi

    # Restore from backup
    log_info "Restoring database from backup..."

    if PGPASSWORD="$db_password" pg_restore -h "$db_host" -U "$db_user" -d "$db_name" \
        --clean --if-exists --verbose "$backup_file"; then
        log_success "✅ Database restored successfully"
    else
        log_error "Database restore failed"

        # Attempt to restore pre-rollback backup
        log_warning "Attempting to restore pre-rollback state..."
        if PGPASSWORD="$db_password" pg_restore -h "$db_host" -U "$db_user" -d "$db_name" \
            --clean --if-exists "$pre_rollback_backup"; then
            log_success "✅ Pre-rollback state restored"
        else
            log_error "❌ CRITICAL: Failed to restore pre-rollback state"
            send_emergency_alert "CRITICAL: Database rollback failed and pre-rollback state could not be restored" "CRITICAL"
        fi

        exit 1
    fi
}

# ==========================================
# Health Check Functions
# ==========================================
perform_emergency_health_check() {
    log_info "🏥 Performing emergency health check..."

    local failed_services=()

    for service in "${SERVICES[@]}"; do
        # Quick pod readiness check
        local ready_pods=$(kubectl get pods -n "$NAMESPACE" -l app="$service" -o jsonpath='{.items[*].status.conditions[?(@.type=="Ready")].status}' | grep -c "True" || echo "0")
        local total_pods=$(kubectl get pods -n "$NAMESPACE" -l app="$service" --no-headers | wc -l)

        if [[ "$ready_pods" -eq 0 ]] || [[ "$total_pods" -eq 0 ]]; then
            failed_services+=("$service")
            log_warning "⚠️ $service: $ready_pods/$total_pods pods ready"
        else
            log_success "✅ $service: $ready_pods/$total_pods pods ready"
        fi
    done

    if [[ ${#failed_services[@]} -gt 0 ]]; then
        log_warning "⚠️ Some services may not be fully ready: ${failed_services[*]}"
        log_warning "This is expected immediately after emergency rollback"
        return 1
    else
        log_success "✅ All services appear healthy"
        return 0
    fi
}

perform_full_health_check() {
    log_info "🏥 Performing comprehensive health check..."

    local failed_checks=0

    for service in "${SERVICES[@]}"; do
        log_info "Checking $service..."

        # Wait for pods to be ready
        local max_attempts=20
        local attempt=1

        while [[ $attempt -le $max_attempts ]]; do
            local ready_pods=$(kubectl get pods -n "$NAMESPACE" -l app="$service" -o jsonpath='{.items[*].status.conditions[?(@.type=="Ready")].status}' | grep -c "True" || echo "0")
            local total_pods=$(kubectl get pods -n "$NAMESPACE" -l app="$service" --no-headers | wc -l)

            if [[ "$ready_pods" -eq "$total_pods" ]] && [[ "$total_pods" -gt 0 ]]; then
                break
            fi

            log_info "Waiting for $service pods ($ready_pods/$total_pods ready)... Attempt $attempt/$max_attempts"
            sleep 10
            ((attempt++))
        done

        if [[ $attempt -gt $max_attempts ]]; then
            log_error "❌ $service: Timeout waiting for pods to be ready"
            ((failed_checks++))
            continue
        fi

        # Health endpoint check
        local health_url="http://${service}.${NAMESPACE}.svc.cluster.local:8080/actuator/health"

        if kubectl exec -n "$NAMESPACE" deployment/"$service" -- curl -f "$health_url" &> /dev/null; then
            log_success "✅ $service: Health check passed"
        else
            log_error "❌ $service: Health check failed"
            ((failed_checks++))
        fi
    done

    if [[ $failed_checks -gt 0 ]]; then
        log_error "❌ $failed_checks service(s) failed health checks"
        return 1
    else
        log_success "✅ All services passed health checks"
        return 0
    fi
}

# ==========================================
# Interactive Functions
# ==========================================
interactive_rollback_selection() {
    log_info "🔍 Interactive rollback target selection..."

    get_available_rollback_targets

    echo ""
    echo "Select rollback target:"
    echo "1. Emergency rollback (latest known good state)"
    echo "2. Specific version"
    echo "3. Previous deployment state"
    echo "4. Database backup"
    echo "5. Cancel"

    read -p "Enter your choice (1-5): " choice

    case $choice in
        1)
            ROLLBACK_TYPE="deployment"
            EMERGENCY_MODE=true
            ;;
        2)
            read -p "Enter version to rollback to: " TARGET_VERSION
            ROLLBACK_TYPE="deployment"
            ;;
        3)
            ROLLBACK_TYPE="deployment"
            ;;
        4)
            ROLLBACK_TYPE="database"
            ;;
        5)
            log_info "Rollback cancelled"
            exit 0
            ;;
        *)
            log_error "Invalid choice"
            exit 1
            ;;
    esac
}

# ==========================================
# Main Rollback Function
# ==========================================
main_rollback() {
    local start_time=$(date +%s)

    log_info "🔄 Starting rollback process..."
    log_info "Environment: $ENVIRONMENT"
    log_info "Type: $ROLLBACK_TYPE"
    log_info "Emergency Mode: $EMERGENCY_MODE"
    log_info "Target Version: ${TARGET_VERSION:-"auto"}"

    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "🔍 DRY RUN MODE - No actual changes will be made"
        get_available_rollback_targets
        return 0
    fi

    # Confirmation for production
    if [[ "$ENVIRONMENT" == "production" ]] && [[ "$AUTO_CONFIRM" == "false" ]] && [[ "$EMERGENCY_MODE" == "false" ]]; then
        echo ""
        log_warning "⚠️  PRODUCTION ROLLBACK WARNING"
        log_warning "You are about to rollback the PRODUCTION environment"
        log_warning "This operation may cause service disruption"
        echo ""
        read -p "Type 'ROLLBACK PRODUCTION' to confirm: " confirmation

        if [[ "$confirmation" != "ROLLBACK PRODUCTION" ]]; then
            log_info "Rollback cancelled"
            exit 0
        fi
    fi

    # Execute rollback based on type
    case "$ROLLBACK_TYPE" in
        deployment)
            if [[ "$EMERGENCY_MODE" == "true" ]]; then
                emergency_rollback
            else
                rollback_deployments "$TARGET_VERSION"
            fi
            ;;
        database)
            rollback_database
            ;;
        full)
            rollback_deployments "$TARGET_VERSION"
            rollback_database
            ;;
        *)
            log_error "Unknown rollback type: $ROLLBACK_TYPE"
            exit 1
            ;;
    esac

    # Post-rollback validation
    if [[ "$SKIP_VALIDATION" == "false" ]]; then
        log_info "🔍 Running post-rollback validation..."

        if [[ "$EMERGENCY_MODE" == "true" ]]; then
            perform_emergency_health_check || log_warning "⚠️ Emergency health check failed - manual verification required"
        else
            perform_full_health_check || {
                log_error "Post-rollback health check failed"
                send_emergency_alert "Rollback completed but health checks failed in $ENVIRONMENT" "HIGH"
                exit 1
            }
        fi
    fi

    local end_time=$(date +%s)
    local duration=$((end_time - start_time))

    log_success "🎉 Rollback completed successfully in ${duration} seconds!"

    # Send success notification
    local message="Rollback completed successfully in $ENVIRONMENT environment (${duration}s)"
    if [[ "$EMERGENCY_MODE" == "true" ]]; then
        send_emergency_alert "$message" "MEDIUM"
    fi
}

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
            TARGET_VERSION="$2"
            shift 2
            ;;
        -t|--type)
            ROLLBACK_TYPE="$2"
            shift 2
            ;;
        --emergency)
            EMERGENCY_MODE=true
            shift
            ;;
        --skip-validation)
            SKIP_VALIDATION=true
            shift
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --auto-confirm)
            AUTO_CONFIRM=true
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
    echo -e "${RED}"
    echo "╔══════════════════════════════════════════════════════════════╗"
    echo "║                    🔄 GASOLINERA JSM                        ║"
    echo "║                Emergency Rollback System                     ║"
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"

    # Initialize logging
    log_info "🔄 Gasolinera JSM Rollback Started"
    log_info "Timestamp: $(date)"
    log_info "User: $(whoami)"
    log_info "Host: $(hostname)"

    # Validate environment
    if [[ -z "$ENVIRONMENT" ]]; then
        log_error "Environment is required"
        usage
        exit 1
    fi

    validate_environment "$ENVIRONMENT"

    # Check prerequisites
    check_rollback_prerequisites

    # Interactive mode if no specific parameters
    if [[ -z "$TARGET_VERSION" ]] && [[ "$EMERGENCY_MODE" == "false" ]] && [[ "$DRY_RUN" == "false" ]]; then
        interactive_rollback_selection
    fi

    # Execute rollback
    main_rollback

    log_success "🎉 All rollback operations completed successfully!"
}

# Signal handlers
trap 'log_error "Rollback interrupted"; exit 130' INT TERM

# Run main function
main "$@"