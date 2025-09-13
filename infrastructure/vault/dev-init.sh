#!/bin/bash

# Vault Development Initialization Script
# Sets up Vault with development secrets and policies

set -euo pipefail

# Configuration
VAULT_ADDR="${VAULT_ADDR:-http://localhost:8200}"
VAULT_TOKEN="${VAULT_TOKEN:-dev_vault_token_2024}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_status() {
    echo -e "${BLUE}[VAULT-INIT]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[VAULT-INIT]${NC} $1"
}

print_error() {
    echo -e "${RED}[VAULT-INIT]${NC} $1"
}

wait_for_vault() {
    print_status "Waiting for Vault to be ready..."

    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if vault status > /dev/null 2>&1; then
            print_success "Vault is ready!"
            return 0
        fi

        print_status "Attempt $attempt/$max_attempts - Vault not ready yet..."
        sleep 2
        ((attempt++))
    done

    print_error "Vault failed to become ready within timeout"
    return 1
}

setup_auth_methods() {
    print_status "Setting up authentication methods..."

    # Enable userpass auth method
    vault auth enable -path=userpass userpass || true

    # Create development users
    vault write auth/userpass/users/dev-admin \
        password=dev_admin_password_2024 \
        policies=dev-admin-policy

    vault write auth/userpass/users/dev-service \
        password=dev_service_password_2024 \
        policies=dev-service-policy

    print_success "Authentication methods configured"
}

create_policies() {
    print_status "Creating Vault policies..."

    # Admin policy for development
    vault policy write dev-admin-policy - <<EOF
# Development Admin Policy
path "*" {
  capabilities = ["create", "read", "update", "delete", "list", "sudo"]
}
EOF

    # Service policy for applications
    vault policy write dev-service-policy - <<EOF
# Development Service Policy
path "secret/data/gasolinera/*" {
  capabilities = ["read"]
}

path "secret/data/shared/*" {
  capabilities = ["read"]
}

path "database/creds/gasolinera-role" {
  capabilities = ["read"]
}

path "auth/token/lookup-self" {
  capabilities = ["read"]
}

path "auth/token/renew-self" {
  capabilities = ["update"]
}
EOF

    print_success "Policies created"
}

enable_secrets_engines() {
    print_status "Enabling secrets engines..."

    # Enable KV v2 secrets engine
    vault secrets enable -path=secret kv-v2 || true

    # Enable database secrets engine
    vault secrets enable database || true

    print_success "Secrets engines enabled"
}

setup_database_secrets() {
    print_status "Setting up database secrets..."

    # Configure PostgreSQL connection
    vault write database/config/gasolinera-postgres \
        plugin_name=postgresql-database-plugin \
        connection_url="postgresql://{{username}}:{{password}}@postgres:5432/gasolinera_jsm_dev?sslmode=disable" \
        allowed_roles="gasolinera-role" \
        username="gasolinera_dev" \
        password="dev_password_2024"

    # Create database role
    vault write database/roles/gasolinera-role \
        db_name=gasolinera-postgres \
        creation_statements="CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; \
                           GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO \"{{name}}\"; \
                           GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO \"{{name}}\";" \
        default_ttl="1h" \
        max_ttl="24h"

    print_success "Database secrets configured"
}

store_application_secrets() {
    print_status "Storing application secrets..."

    # JWT secrets
    vault kv put secret/gasolinera/jwt \
        secret="dev_jwt_secret_key_change_in_production_2024_super_secure" \
        refresh_secret="dev_jwt_refresh_secret_key_change_in_production_2024_super_secure" \
        expiration="3600" \
        refresh_expiration="86400"

    # Database configuration
    vault kv put secret/gasolinera/database \
        host="postgres" \
        port="5432" \
        database="gasolinera_jsm_dev" \
        username="gasolinera_dev" \
        password="dev_password_2024" \
        max_connections="20" \
        connection_timeout="30000"

    # Redis configuration
    vault kv put secret/gasolinera/redis \
        host="redis" \
        port="6379" \
        password="" \
        database="0" \
        timeout="5000" \
        max_connections="10"

    # RabbitMQ configuration
    vault kv put secret/gasolinera/rabbitmq \
        host="rabbitmq" \
        port="5672" \
        username="gasolinera_dev" \
        password="dev_password_2024" \
        virtual_host="gasolinera_dev_vhost" \
        connection_timeout="30000"

    # External service configurations (mock for development)
    vault kv put secret/gasolinera/external/payment \
        api_key="dev_payment_api_key_2024" \
        api_secret="dev_payment_api_secret_2024" \
        base_url="http://mock-payment-service:8090" \
        timeout="30000"

    vault kv put secret/gasolinera/external/sms \
        api_key="dev_sms_api_key_2024" \
        api_secret="dev_sms_api_secret_2024" \
        base_url="http://mock-sms-service:8091" \
        timeout="15000"

    vault kv put secret/gasolinera/external/email \
        api_key="dev_email_api_key_2024" \
        smtp_host="mock-email-service" \
        smtp_port="587" \
        smtp_username="dev@gasolinera.com" \
        smtp_password="dev_email_password_2024"

    # Business configuration
    vault kv put secret/gasolinera/business \
        qr_expiration_minutes="15" \
        coupon_default_expiration_days="30" \
        raffle_ticket_price_default="50" \
        max_raffle_participants="1000" \
        fuel_price_update_interval="300"

    # Security configuration
    vault kv put secret/gasolinera/security \
        encryption_key="dev_encryption_key_2024_change_in_production" \
        api_rate_limit="1000" \
        session_timeout="7200" \
        max_login_attempts="5" \
        lockout_duration="900"

    # Shared configuration
    vault kv put secret/shared/monitoring \
        jaeger_endpoint="http://jaeger:14268/api/traces" \
        prometheus_endpoint="http://prometheus:9090" \
        grafana_endpoint="http://grafana:3000" \
        log_level="DEBUG"

    vault kv put secret/shared/features \
        enable_debug_endpoints="true" \
        enable_swagger="true" \
        enable_actuator="true" \
        enable_cors="true" \
        cors_allowed_origins="http://localhost:3000,http://localhost:3001,http://localhost:3010"

    print_success "Application secrets stored"
}

create_service_tokens() {
    print_status "Creating service tokens..."

    # Create tokens for each service
    local services=("auth-service" "coupon-service" "station-service" "raffle-service" "redemption-service" "ad-engine" "api-gateway")

    for service in "${services[@]}"; do
        local token_info
        token_info=$(vault write -format=json auth/token/create \
            policies="dev-service-policy" \
            ttl="24h" \
            renewable=true \
            display_name="$service-dev-token")

        local token
        token=$(echo "$token_info" | jq -r '.auth.client_token')

        # Store token for the service to use
        vault kv put "secret/gasolinera/tokens/$service" \
            token="$token" \
            created_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
            ttl="24h"

        print_success "Created token for $service"
    done
}

setup_audit_logging() {
    print_status "Setting up audit logging..."

    # Enable file audit device
    vault audit enable file file_path=/vault/logs/audit.log || true

    print_success "Audit logging configured"
}

show_vault_info() {
    print_status "Vault Development Setup Complete!"
    echo ""
    echo "🔒 Vault Information:"
    echo "  URL: $VAULT_ADDR"
    echo "  Root Token: $VAULT_TOKEN"
    echo ""
    echo "👤 Development Users:"
    echo "  Admin: dev-admin / dev_admin_password_2024"
    echo "  Service: dev-service / dev_service_password_2024"
    echo ""
    echo "📁 Secret Paths:"
    echo "  Application: secret/gasolinera/*"
    echo "  Shared: secret/shared/*"
    echo "  Tokens: secret/gasolinera/tokens/*"
    echo ""
    echo "🔑 Useful Commands:"
    echo "  vault kv get secret/gasolinera/jwt"
    echo "  vault kv get secret/gasolinera/database"
    echo "  vault read database/creds/gasolinera-role"
    echo ""
}

main() {
    print_status "Initializing Vault for development..."

    # Set Vault address and token
    export VAULT_ADDR="$VAULT_ADDR"
    export VAULT_TOKEN="$VAULT_TOKEN"

    # Wait for Vault to be ready
    wait_for_vault

    # Setup Vault configuration
    setup_auth_methods
    create_policies
    enable_secrets_engines
    setup_database_secrets
    store_application_secrets
    create_service_tokens
    setup_audit_logging

    # Show information
    show_vault_info

    print_success "Vault development initialization completed!"
}

# Run main function
main "$@"