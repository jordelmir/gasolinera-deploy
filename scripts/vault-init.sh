#!/bin/bash
# HashiCorp Vault Initialization Script for Gasolinera JSM Platform

set -e

echo "Initializing HashiCorp Vault for Gasolinera JSM Platform..."

# Wait for Vault to be ready
until vault status; do
  echo "Waiting for Vault to start..."
  sleep 2
done

echo "Vault is ready. Setting up secrets and policies..."

# Login with root token
vault auth -method=token token=gasolinera_vault_token_2024

# Enable KV secrets engine if not already enabled
vault secrets enable -path=secret kv-v2 || echo "KV secrets engine already enabled"

# Create application secrets
echo "Creating application secrets..."

# Database secrets
vault kv put secret/database \
  postgres_host=postgres \
  postgres_port=5432 \
  postgres_db=gasolinera_jsm \
  postgres_user=gasolinera_user \
  postgres_password=gasolinera_pass_2024 \
  auth_service_user=auth_service_user \
  auth_service_password=auth_service_pass_2024 \
  station_service_user=station_service_user \
  station_service_password=station_service_pass_2024 \
  coupon_service_user=coupon_service_user \
  coupon_service_password=coupon_service_pass_2024 \
  redemption_service_user=redemption_service_user \
  redemption_service_password=redemption_service_pass_2024 \
  ad_service_user=ad_service_user \
  ad_service_password=ad_service_pass_2024 \
  raffle_service_user=raffle_service_user \
  raffle_service_password=raffle_service_pass_2024

# Redis secrets
vault kv put secret/redis \
  host=redis \
  port=6379 \
  password=redis_pass_2024 \
  database=0

# RabbitMQ secrets
vault kv put secret/rabbitmq \
  host=rabbitmq \
  port=5672 \
  username=gasolinera_user \
  password=rabbitmq_pass_2024 \
  vhost=gasolinera_vhost \
  management_port=15672

# JWT secrets
vault kv put secret/jwt \
  secret_key=gasolinera_jwt_secret_key_2024_super_secure_256_bits_long \
  refresh_secret_key=gasolinera_jwt_refresh_secret_key_2024_super_secure_256_bits_long \
  access_token_expiration=3600 \
  refresh_token_expiration=86400 \
  issuer=gasolinera-jsm-platform \
  audience=gasolinera-jsm-users

# QR Code signing secrets
vault kv put secret/qr \
  signature_secret=gasolinera_qr_signature_secret_2024_super_secure_key \
  algorithm=HmacSHA256 \
  expiration_hours=24

# External service secrets
vault kv put secret/external \
  fallback_ad_url=https://example.com/fallback-ad.mp4 \
  sms_provider_api_key=dummy_sms_api_key \
  sms_provider_url=https://api.sms-provider.com \
  email_smtp_host=smtp.gmail.com \
  email_smtp_port=587 \
  email_username=noreply@gasolinera-jsm.com \
  email_password=dummy_email_password

# Monitoring and observability secrets
vault kv put secret/monitoring \
  jaeger_endpoint=http://jaeger:4317 \
  prometheus_endpoint=http://prometheus:9090 \
  grafana_admin_password=grafana_admin_2024

# Service-specific secrets
vault kv put secret/auth-service \
  otp_expiration_minutes=5 \
  max_login_attempts=5 \
  account_lockout_minutes=30 \
  password_reset_expiration_hours=2

vault kv put secret/coupon-service \
  max_coupons_per_campaign=10000 \
  default_coupon_expiration_days=30 \
  qr_code_size=200 \
  batch_generation_size=1000

vault kv put secret/redemption-service \
  max_redemptions_per_user_per_day=10 \
  redemption_timeout_seconds=30 \
  retry_attempts=3 \
  default_raffle_tickets_per_redemption=1

vault kv put secret/ad-engine \
  default_ad_duration_seconds=30 \
  max_ads_per_user_per_day=20 \
  ticket_multiplier_base=2 \
  ad_engagement_timeout_seconds=60

vault kv put secret/raffle-service \
  max_tickets_per_entry=100 \
  draw_execution_timeout_minutes=10 \
  winner_notification_timeout_hours=24 \
  prize_claim_deadline_days=30

vault kv put secret/api-gateway \
  rate_limit_requests_per_minute=100 \
  circuit_breaker_failure_threshold=5 \
  circuit_breaker_timeout_seconds=30 \
  cors_allowed_origins=http://localhost:3000,http://localhost:8080

# Create policies for each service
echo "Creating Vault policies..."

# Auth Service Policy
vault policy write auth-service-policy - <<EOF
path "secret/data/database" {
  capabilities = ["read"]
}
path "secret/data/redis" {
  capabilities = ["read"]
}
path "secret/data/jwt" {
  capabilities = ["read"]
}
path "secret/data/auth-service" {
  capabilities = ["read"]
}
path "secret/data/external" {
  capabilities = ["read"]
}
EOF

# Station Service Policy
vault policy write station-service-policy - <<EOF
path "secret/data/database" {
  capabilities = ["read"]
}
path "secret/data/jwt" {
  capabilities = ["read"]
}
path "secret/data/station-service" {
  capabilities = ["read"]
}
EOF

# Coupon Service Policy
vault policy write coupon-service-policy - <<EOF
path "secret/data/database" {
  capabilities = ["read"]
}
path "secret/data/redis" {
  capabilities = ["read"]
}
path "secret/data/rabbitmq" {
  capabilities = ["read"]
}
path "secret/data/qr" {
  capabilities = ["read"]
}
path "secret/data/coupon-service" {
  capabilities = ["read"]
}
EOF

# Redemption Service Policy
vault policy write redemption-service-policy - <<EOF
path "secret/data/database" {
  capabilities = ["read"]
}
path "secret/data/redis" {
  capabilities = ["read"]
}
path "secret/data/rabbitmq" {
  capabilities = ["read"]
}
path "secret/data/redemption-service" {
  capabilities = ["read"]
}
EOF

# Ad Engine Policy
vault policy write ad-engine-policy - <<EOF
path "secret/data/database" {
  capabilities = ["read"]
}
path "secret/data/external" {
  capabilities = ["read"]
}
path "secret/data/ad-engine" {
  capabilities = ["read"]
}
EOF

# Raffle Service Policy
vault policy write raffle-service-policy - <<EOF
path "secret/data/database" {
  capabilities = ["read"]
}
path "secret/data/rabbitmq" {
  capabilities = ["read"]
}
path "secret/data/raffle-service" {
  capabilities = ["read"]
}
EOF

# API Gateway Policy
vault policy write api-gateway-policy - <<EOF
path "secret/data/jwt" {
  capabilities = ["read"]
}
path "secret/data/api-gateway" {
  capabilities = ["read"]
}
path "secret/data/monitoring" {
  capabilities = ["read"]
}
EOF

echo "Vault initialization completed successfully!"
echo "Secrets created for: database, redis, rabbitmq, jwt, qr, external services"
echo "Service-specific configurations stored"
echo "Policies created for each microservice"
echo "Vault is ready for use by the Gasolinera JSM Platform"