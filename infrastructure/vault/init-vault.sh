#!/bin/bash

# HashiCorp Vault Initialization Script for Gasolinera JSM
# This script initializes Vault, creates policies, and sets up secrets

set -e

# Configuration
VAULT_ADDR=${VAULT_ADDR:-"http://localhost:8200"}
VAULT_TOKEN_FILE="/tmp/vault-root-token"
VAULT_KEYS_FILE="/tmp/vault-unseal-keys"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

warn() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARNING: $1${NC}"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR: $1${NC}"
    exit 1
}

# Wait for Vault to be ready
wait_for_vault() {
    log "Waiting for Vault to be ready..."
    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if curl -s "$VAULT_ADDR/v1/sys/health" > /dev/null 2>&1; then
            log "Vault is ready!"
            return 0
        fi

        log "Attempt $attempt/$max_attempts: Vault not ready yet, waiting..."
        sleep 2
        ((attempt++))
    done

    error "Vault failed to become ready after $max_attempts attempts"
}

# Initialize Vault
initialize_vault() {
    log "Checking if Vault is already initialized..."

    if vault status 2>/dev/null | grep -q "Initialized.*true"; then
        warn "Vault is already initialized"
        return 0
    fi

    log "Initializing Vault..."
    vault operator init \
        -key-shares=5 \
        -key-threshold=3 \
        -format=json > "$VAULT_KEYS_FILE"

    if [ $? -eq 0 ]; then
        log "Vault initialized successfully"

        # Extract root token
        jq -r '.root_token' "$VAULT_KEYS_FILE" > "$VAULT_TOKEN_FILE"

        # Extract unseal keys
        log "Unseal keys saved to $VAULT_KEYS_FILE"
        log "Root token saved to $VAULT_TOKEN_FILE"

        # Set permissions
        chmod 600 "$VAULT_KEYS_FILE" "$VAULT_TOKEN_FILE"
    else
        error "Failed to initialize Vault"
    fi
}

# Unseal Vault
unseal_vault() {
    log "Checking if Vault needs to be unsealed..."

    if vault status 2>/dev/null | grep -q "Sealed.*false"; then
        log "Vault is already unsealed"
        return 0
    fi

    if [ ! -f "$VAULT_KEYS_FILE" ]; then
        error "Unseal keys file not found: $VAULT_KEYS_FILE"
    fi

    log "Unsealing Vault..."

    # Get the first 3 unseal keys
    for i in {0..2}; do
        key=$(jq -r ".unseal_keys_b64[$i]" "$VAULT_KEYS_FILE")
        vault operator unseal "$key"
    done

    log "Vault unsealed successfully"
}

# Authenticate with root token
authenticate() {
    if [ ! -f "$VAULT_TOKEN_FILE" ]; then
        error "Root token file not found: $VAULT_TOKEN_FILE"
    fi

    export VAULT_TOKEN=$(cat "$VAULT_TOKEN_FILE")
    log "Authenticated with Vault using root token"
}

# Enable secret engines
enable_secret_engines() {
    log "Enabling secret engines..."

    # Enable KV v2 secret engine for application secrets
    vault secrets enable -path=gasolinera-jsm -version=2 kv || warn "KV engine may already be enabled"

    # Enable database secret engine for dynamic credentials
    vault secrets enable database || warn "Database engine may already be enabled"

    # Enable PKI for certificate management
    vault secrets enable pki || warn "PKI engine may already be enabled"
    vault secrets tune -max-lease-ttl=87600h pki || warn "PKI tuning may have failed"

    # Enable transit for encryption as a service
    vault secrets enable transit || warn "Transit engine may already be enabled"

    log "Secret engines enabled successfully"
}

# Create policies
create_policies() {
    log "Creating Vault policies..."

    # Application policy for microservices
    cat > /tmp/gasolinera-app-policy.hcl << 'EOF'
# Policy for Gasolinera JSM microservices
path "gasolinera-jsm/data/auth-service/*" {
  capabilities = ["read"]
}

path "gasolinera-jsm/data/station-service/*" {
  capabilities = ["read"]
}

path "gasolinera-jsm/data/coupon-service/*" {
  capabilities = ["read"]
}

path "gasolinera-jsm/data/redemption-service/*" {
  capabilities = ["read"]
}

path "gasolinera-jsm/data/raffle-service/*" {
  capabilities = ["read"]
}

path "gasolinera-jsm/data/ad-engine/*" {
  capabilities = ["read"]
}

path "gasolinera-jsm/data/shared/*" {
  capabilities = ["read"]
}

# Database credentials
path "database/creds/gasolinera-readonly" {
  capabilities = ["read"]
}

path "database/creds/gasolinera-readwrite" {
  capabilities = ["read"]
}

# Transit encryption
path "transit/encrypt/gasolinera-key" {
  capabilities = ["update"]
}

path "transit/decrypt/gasolinera-key" {
  capabilities = ["update"]
}
EOF

    vault policy write gasolinera-app /tmp/gasolinera-app-policy.hcl

    # Admin policy for management operations
    cat > /tmp/gasolinera-admin-policy.hcl << 'EOF'
# Policy for Gasolinera JSM administrators
path "gasolinera-jsm/*" {
  capabilities = ["create", "read", "update", "delete", "list"]
}

path "database/*" {
  capabilities = ["create", "read", "update", "delete", "list"]
}

path "pki/*" {
  capabilities = ["create", "read", "update", "delete", "list"]
}

path "transit/*" {
  capabilities = ["create", "read", "update", "delete", "list"]
}

path "sys/policies/acl/*" {
  capabilities = ["create", "read", "update", "delete", "list"]
}

path "auth/*" {
  capabilities = ["create", "read", "update", "delete", "list"]
}
EOF

    vault policy write gasolinera-admin /tmp/gasolinera-admin-policy.hcl

    log "Policies created successfully"
}

# Configure database secret engine
configure_database_engine() {
    log "Configuring database secret engine..."

    # Configure PostgreSQL connection
    vault write database/config/postgresql \
        plugin_name=postgresql-database-plugin \
        connection_url="postgresql://{{username}}:{{password}}@localhost:5432/gasolinera_db?sslmode=disable" \
        allowed_roles="gasolinera-readonly,gasolinera-readwrite" \
        username="vault_db_user" \
        password="vault_db_password"

    # Create readonly role
    vault write database/roles/gasolinera-readonly \
        db_name=postgresql \
        creation_statements="CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; \
                           GRANT SELECT ON ALL TABLES IN SCHEMA public TO \"{{name}}\";" \
        default_ttl="1h" \
        max_ttl="24h"

    # Create readwrite role
    vault write database/roles/gasolinera-readwrite \
        db_name=postgresql \
        creation_statements="CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; \
                           GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO \"{{name}}\";" \
        default_ttl="1h" \
        max_ttl="24h"

    log "Database secret engine configured successfully"
}

# Setup application secrets
setup_application_secrets() {
    log "Setting up application secrets..."

    # JWT secrets
    vault kv put gasolinera-jsm/auth-service \
        jwt-secret="$(openssl rand -base64 64)" \
        jwt-issuer="gasolinera-jsm" \
        jwt-access-expiration="3600" \
        jwt-refresh-expiration="86400"

    # Database credentials (fallback)
    vault kv put gasolinera-jsm/shared/database \
        host="localhost" \
        port="5432" \
        database="gasolinera_db" \
        username="gasolinera_user" \
        password="$(openssl rand -base64 32)"

    # Redis configuration
    vault kv put gasolinera-jsm/shared/redis \
        host="localhost" \
        port="6379" \
        password="$(openssl rand -base64 32)"

    # RabbitMQ configuration
    vault kv put gasolinera-jsm/shared/rabbitmq \
        host="localhost" \
        port="5672" \
        username="gasolinera_user" \
        password="$(openssl rand -base64 32)" \
        virtual-host="/gasolinera"

    # External API keys (placeholders)
    vault kv put gasolinera-jsm/shared/external-apis \
        google-maps-api-key="your-google-maps-api-key" \
        sendgrid-api-key="your-sendgrid-api-key" \
        twilio-api-key="your-twilio-api-key" \
        twilio-auth-token="your-twilio-auth-token"

    # Service-specific secrets
    vault kv put gasolinera-jsm/coupon-service \
        qr-encryption-key="$(openssl rand -base64 32)" \
        coupon-salt="$(openssl rand -base64 16)"

    vault kv put gasolinera-jsm/raffle-service \
        random-seed="$(openssl rand -base64 32)" \
        prize-encryption-key="$(openssl rand -base64 32)"

    vault kv put gasolinera-jsm/ad-engine \
        analytics-api-key="your-analytics-api-key" \
        targeting-secret="$(openssl rand -base64 32)"

    log "Application secrets configured successfully"
}

# Configure transit encryption
configure_transit() {
    log "Configuring transit encryption..."

    # Create encryption key for sensitive data
    vault write -f transit/keys/gasolinera-key

    # Create key for PII encryption
    vault write -f transit/keys/gasolinera-pii-key

    log "Transit encryption configured successfully"
}

# Enable authentication methods
enable_auth_methods() {
    log "Enabling authentication methods..."

    # Enable AppRole for service authentication
    vault auth enable approle || warn "AppRole may already be enabled"

    # Create AppRole for microservices
    vault write auth/approle/role/gasolinera-services \
        token_policies="gasolinera-app" \
        token_ttl=1h \
        token_max_ttl=4h \
        bind_secret_id=true

    # Get role ID and secret ID
    ROLE_ID=$(vault read -field=role_id auth/approle/role/gasolinera-services/role-id)
    SECRET_ID=$(vault write -field=secret_id -f auth/approle/role/gasolinera-services/secret-id)

    log "AppRole credentials:"
    log "Role ID: $ROLE_ID"
    log "Secret ID: $SECRET_ID"

    # Save credentials for services
    echo "$ROLE_ID" > /tmp/vault-role-id
    echo "$SECRET_ID" > /tmp/vault-secret-id
    chmod 600 /tmp/vault-role-id /tmp/vault-secret-id

    log "Authentication methods enabled successfully"
}

# Main execution
main() {
    log "Starting Vault initialization for Gasolinera JSM..."

    export VAULT_ADDR="$VAULT_ADDR"

    wait_for_vault
    initialize_vault
    unseal_vault
    authenticate
    enable_secret_engines
    create_policies
    configure_database_engine
    setup_application_secrets
    configure_transit
    enable_auth_methods

    log "Vault initialization completed successfully!"
    log ""
    log "Important files created:"
    log "  - Root token: $VAULT_TOKEN_FILE"
    log "  - Unseal keys: $VAULT_KEYS_FILE"
    log "  - Role ID: /tmp/vault-role-id"
    log "  - Secret ID: /tmp/vault-secret-id"
    log ""
    warn "Please secure these files and remove them from the filesystem after copying to a secure location!"
}

# Run main function
main "$@"