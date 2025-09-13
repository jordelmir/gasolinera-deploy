#!/bin/bash

# Secret Rotation Script for Gasolinera JSM Vault
# Rotates JWT secrets, database passwords, and API keys

set -e

# Configuration
VAULT_ADDR=${VAULT_ADDR:-"http://localhost:8200"}
VAULT_TOKEN=${VAULT_TOKEN:-""}
ROTATION_LOG="/var/log/vault-rotation.log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log() {
    local message="[$(date +'%Y-%m-%d %H:%M:%S')] $1"
    echo -e "${GREEN}$message${NC}"
    echo "$message" >> "$ROTATION_LOG"
}

warn() {
    local message="[$(date +'%Y-%m-%d %H:%M:%S')] WARNING: $1"
    echo -e "${YELLOW}$message${NC}"
    echo "$message" >> "$ROTATION_LOG"
}

error() {
    local message="[$(date +'%Y-%m-%d %H:%M:%S')] ERROR: $1"
    echo -e "${RED}$message${NC}"
    echo "$message" >> "$ROTATION_LOG"
    exit 1
}

# Check if Vault is accessible
check_vault_status() {
    log "Checking Vault status..."

    if ! vault status > /dev/null 2>&1; then
        error "Vault is not accessible or not unsealed"
    fi

    log "Vault is accessible and unsealed"
}

# Authenticate with Vault
authenticate() {
    if [ -z "$VAULT_TOKEN" ]; then
        error "VAULT_TOKEN environment variable is required"
    fi

    export VAULT_TOKEN="$VAULT_TOKEN"

    # Verify token is valid
    if ! vault token lookup > /dev/null 2>&1; then
        error "Invalid or expired Vault token"
    fi

    log "Successfully authenticated with Vault"
}

# Generate secure random string
generate_secret() {
    local length=${1:-32}
    openssl rand -base64 $length | tr -d "=+/" | cut -c1-$length
}

# Rotate JWT secrets
rotate_jwt_secrets() {
    log "Rotating JWT secrets..."

    local new_jwt_secret=$(generate_secret 64)

    # Get current configuration
    local current_config=$(vault kv get -format=json gasolinera-jsm/auth-service)
    local issuer=$(echo "$current_config" | jq -r '.data.data["jwt-issuer"]')
    local access_exp=$(echo "$current_config" | jq -r '.data.data["jwt-access-expiration"]')
    local refresh_exp=$(echo "$current_config" | jq -r '.data.data["jwt-refresh-expiration"]')

    # Update with new secret
    vault kv put gasolinera-jsm/auth-service \
        jwt-secret="$new_jwt_secret" \
        jwt-issuer="$issuer" \
        jwt-access-expiration="$access_exp" \
        jwt-refresh-expiration="$refresh_exp"

    log "JWT secret rotated successfully"
}

# Rotate database passwords
rotate_database_passwords() {
    log "Rotating database passwords..."

    # Get current database configuration
    local current_config=$(vault kv get -format=json gasolinera-jsm/shared/database)
    local host=$(echo "$current_config" | jq -r '.data.data.host')
    local port=$(echo "$current_config" | jq -r '.data.data.port')
    local database=$(echo "$current_config" | jq -r '.data.data.database')
    local username=$(echo "$current_config" | jq -r '.data.data.username')

    local new_password=$(generate_secret 32)

    # Update password in Vault
    vault kv put gasolinera-jsm/shared/database \
        host="$host" \
        port="$port" \
        database="$database" \
        username="$username" \
        password="$new_password"

    log "Database password rotated successfully"
    warn "Remember to update the actual database user password: ALTER USER $username PASSWORD '$new_password';"
}

# Rotate Redis password
rotate_redis_password() {
    log "Rotating Redis password..."

    local current_config=$(vault kv get -format=json gasolinera-jsm/shared/redis)
    local host=$(echo "$current_config" | jq -r '.data.data.host')
    local port=$(echo "$current_config" | jq -r '.data.data.port')

    local new_password=$(generate_secret 32)

    vault kv put gasolinera-jsm/shared/redis \
        host="$host" \
        port="$port" \
        password="$new_password"

    log "Redis password rotated successfully"
    warn "Remember to update Redis configuration: CONFIG SET requirepass '$new_password'"
}

# Rotate RabbitMQ password
rotate_rabbitmq_password() {
    log "Rotating RabbitMQ password..."

    local current_config=$(vault kv get -format=json gasolinera-jsm/shared/rabbitmq)
    local host=$(echo "$current_config" | jq -r '.data.data.host')
    local port=$(echo "$current_config" | jq -r '.data.data.port')
    local username=$(echo "$current_config" | jq -r '.data.data.username')
    local vhost=$(echo "$current_config" | jq -r '.data.data["virtual-host"]')

    local new_password=$(generate_secret 32)

    vault kv put gasolinera-jsm/shared/rabbitmq \
        host="$host" \
        port="$port" \
        username="$username" \
        password="$new_password" \
        virtual-host="$vhost"

    log "RabbitMQ password rotated successfully"
    warn "Remember to update RabbitMQ user password: rabbitmqctl change_password $username '$new_password'"
}

# Rotate service-specific secrets
rotate_service_secrets() {
    log "Rotating service-specific secrets..."

    # Coupon Service secrets
    local coupon_qr_key=$(generate_secret 32)
    local coupon_salt=$(generate_secret 16)

    vault kv put gasolinera-jsm/coupon-service \
        qr-encryption-key="$coupon_qr_key" \
        coupon-salt="$coupon_salt"

    # Raffle Service secrets
    local raffle_seed=$(generate_secret 32)
    local prize_key=$(generate_secret 32)

    vault kv put gasolinera-jsm/raffle-service \
        random-seed="$raffle_seed" \
        prize-encryption-key="$prize_key"

    # Ad Engine secrets
    local current_ad_config=$(vault kv get -format=json gasolinera-jsm/ad-engine)
    local analytics_key=$(echo "$current_ad_config" | jq -r '.data.data["analytics-api-key"]')
    local targeting_secret=$(generate_secret 32)

    vault kv put gasolinera-jsm/ad-engine \
        analytics-api-key="$analytics_key" \
        targeting-secret="$targeting_secret"

    log "Service-specific secrets rotated successfully"
}

# Rotate transit encryption keys
rotate_transit_keys() {
    log "Rotating transit encryption keys..."

    # Rotate main encryption key
    vault write -f transit/keys/gasolinera-key/rotate

    # Rotate PII encryption key
    vault write -f transit/keys/gasolinera-pii-key/rotate

    log "Transit encryption keys rotated successfully"
}

# Notify services about rotation
notify_services() {
    log "Notifying services about secret rotation..."

    # This would typically send notifications to services
    # to refresh their configurations or restart

    # Example: Send webhook notifications
    # curl -X POST http://service-discovery/api/v1/notify-rotation

    # Example: Update Kubernetes secrets
    # kubectl create secret generic app-secrets --from-literal=rotation-timestamp="$(date -u +%Y-%m-%dT%H:%M:%SZ)" --dry-run=client -o yaml | kubectl apply -f -

    log "Service notification completed"
}

# Backup current secrets before rotation
backup_secrets() {
    log "Creating backup of current secrets..."

    local backup_dir="/tmp/vault-backup-$(date +%Y%m%d-%H%M%S)"
    mkdir -p "$backup_dir"

    # Backup all secrets
    vault kv get -format=json gasolinera-jsm/auth-service > "$backup_dir/auth-service.json"
    vault kv get -format=json gasolinera-jsm/shared/database > "$backup_dir/database.json"
    vault kv get -format=json gasolinera-jsm/shared/redis > "$backup_dir/redis.json"
    vault kv get -format=json gasolinera-jsm/shared/rabbitmq > "$backup_dir/rabbitmq.json"
    vault kv get -format=json gasolinera-jsm/coupon-service > "$backup_dir/coupon-service.json"
    vault kv get -format=json gasolinera-jsm/raffle-service > "$backup_dir/raffle-service.json"
    vault kv get -format=json gasolinera-jsm/ad-engine > "$backup_dir/ad-engine.json"

    log "Secrets backed up to: $backup_dir"
}

# Main rotation function
rotate_all_secrets() {
    log "Starting complete secret rotation..."

    backup_secrets
    rotate_jwt_secrets
    rotate_database_passwords
    rotate_redis_password
    rotate_rabbitmq_password
    rotate_service_secrets
    rotate_transit_keys
    notify_services

    log "Complete secret rotation finished successfully"
}

# Show usage
usage() {
    echo "Usage: $0 [OPTION]"
    echo "Rotate secrets in HashiCorp Vault for Gasolinera JSM"
    echo ""
    echo "Options:"
    echo "  all         Rotate all secrets"
    echo "  jwt         Rotate JWT secrets only"
    echo "  database    Rotate database passwords only"
    echo "  redis       Rotate Redis password only"
    echo "  rabbitmq    Rotate RabbitMQ password only"
    echo "  services    Rotate service-specific secrets only"
    echo "  transit     Rotate transit encryption keys only"
    echo "  backup      Create backup of current secrets only"
    echo "  help        Show this help message"
    echo ""
    echo "Environment variables:"
    echo "  VAULT_ADDR   Vault server address (default: http://localhost:8200)"
    echo "  VAULT_TOKEN  Vault authentication token (required)"
}

# Main execution
main() {
    export VAULT_ADDR="$VAULT_ADDR"

    check_vault_status
    authenticate

    case "${1:-all}" in
        "all")
            rotate_all_secrets
            ;;
        "jwt")
            backup_secrets
            rotate_jwt_secrets
            ;;
        "database")
            backup_secrets
            rotate_database_passwords
            ;;
        "redis")
            backup_secrets
            rotate_redis_password
            ;;
        "rabbitmq")
            backup_secrets
            rotate_rabbitmq_password
            ;;
        "services")
            backup_secrets
            rotate_service_secrets
            ;;
        "transit")
            rotate_transit_keys
            ;;
        "backup")
            backup_secrets
            ;;
        "help"|"-h"|"--help")
            usage
            exit 0
            ;;
        *)
            error "Unknown option: $1. Use 'help' for usage information."
            ;;
    esac
}

# Run main function
main "$@"