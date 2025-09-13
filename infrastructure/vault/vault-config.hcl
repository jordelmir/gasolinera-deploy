# HashiCorp Vault Configuration for Gasolinera JSM
# This configuration sets up Vault for development and production environments

# Storage backend configuration
storage "postgresql" {
  connection_url = "postgres://vault_user:vault_password@localhost:5432/vault_db?sslmode=disable"
  table          = "vault_kv_store"
  max_parallel   = 128
}

# Development storage (file-based for local development)
# storage "file" {
#   path = "/vault/data"
# }

# API listener configuration
listener "tcp" {
  address       = "0.0.0.0:8200"
  tls_disable   = true  # Set to false in production with proper certificates
  # tls_cert_file = "/vault/tls/vault.crt"
  # tls_key_file  = "/vault/tls/vault.key"
}

# UI configuration
ui = true

# Cluster configuration for HA setup
cluster_addr  = "https://127.0.0.1:8201"
api_addr      = "http://127.0.0.1:8200"

# Logging configuration
log_level = "INFO"
log_format = "json"

# Disable mlock for development (enable in production)
disable_mlock = true

# Plugin directory
plugin_directory = "/vault/plugins"

# Default lease TTL and max lease TTL
default_lease_ttl = "768h"  # 32 days
max_lease_ttl = "8760h"     # 1 year

# Performance and telemetry
telemetry {
  prometheus_retention_time = "30s"
  disable_hostname = true
}

# Seal configuration (auto-unseal with cloud KMS in production)
# seal "awskms" {
#   region     = "us-west-2"
#   kms_key_id = "alias/vault-unseal-key"
# }