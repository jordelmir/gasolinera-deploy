#!/bin/bash

# RabbitMQ Setup Script for Gasolinera JSM Platform
# This script configures exchanges, queues, and bindings

set -e

# Configuration
RABBITMQ_HOST=${RABBITMQ_HOST:-localhost}
RABBITMQ_PORT=${RABBITMQ_PORT:-15672}
RABBITMQ_USER=${RABBITMQ_USER:-gasolinera_user}
RABBITMQ_PASS=${RABBITMQ_PASS:-gasolinera_pass}
RABBITMQ_VHOST=${RABBITMQ_VHOST:-/gasolinera}

echo "🐰 Setting up RabbitMQ for Gasolinera JSM Platform..."
echo "Host: $RABBITMQ_HOST:$RABBITMQ_PORT"
echo "VHost: $RABBITMQ_VHOST"

# Function to make RabbitMQ API calls
rabbitmq_api() {
    local method=$1
    local path=$2
    local data=$3

    curl -s -u "$RABBITMQ_USER:$RABBITMQ_PASS" \
         -H "Content-Type: application/json" \
         -X "$method" \
         "http://$RABBITMQ_HOST:$RABBITMQ_PORT/api$path" \
         ${data:+-d "$data"}
}

# Wait for RabbitMQ to be ready
echo "⏳ Waiting for RabbitMQ to be ready..."
until rabbitmq_api GET "/overview" > /dev/null 2>&1; do
    echo "Waiting for RabbitMQ..."
    sleep 5
done
echo "✅ RabbitMQ is ready!"

# Create VHost if it doesn't exist
echo "🏠 Creating virtual host: $RABBITMQ_VHOST"
rabbitmq_api PUT "/vhosts$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')" || true

# Set permissions for the user on the vhost
echo "🔐 Setting permissions for user: $RABBITMQ_USER"
rabbitmq_api PUT "/permissions$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/$RABBITMQ_USER" \
    '{"configure":".*","write":".*","read":".*"}' || true

# Create Exchanges
echo "📡 Creating exchanges..."

# Main events exchange
rabbitmq_api PUT "/exchanges$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/gasolinera.events" \
    '{"type":"topic","durable":true,"auto_delete":false,"arguments":{"alternate-exchange":"gasolinera.dlx"}}'

# Dead Letter Exchange
rabbitmq_api PUT "/exchanges$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/gasolinera.dlx" \
    '{"type":"direct","durable":true,"auto_delete":false}'

# Redemption Exchange
rabbitmq_api PUT "/exchanges$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/redemption.exchange" \
    '{"type":"topic","durable":true,"auto_delete":false,"arguments":{"alternate-exchange":"gasolinera.dlx"}}'

# Ad Engine Exchange
rabbitmq_api PUT "/exchanges$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/ad.exchange" \
    '{"type":"topic","durable":true,"auto_delete":false,"arguments":{"alternate-exchange":"gasolinera.dlx"}}'

# Raffle Exchange
rabbitmq_api PUT "/exchanges$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/raffle.exchange" \
    '{"type":"topic","durable":true,"auto_delete":false,"arguments":{"alternate-exchange":"gasolinera.dlx"}}'

# Coupon Exchange
rabbitmq_api PUT "/exchanges$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/coupon.exchange" \
    '{"type":"topic","durable":true,"auto_delete":false,"arguments":{"alternate-exchange":"gasolinera.dlx"}}'

# Audit Exchange
rabbitmq_api PUT "/exchanges$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/audit.exchange" \
    '{"type":"topic","durable":true,"auto_delete":false}'

echo "✅ Exchanges created successfully!"

# Create Queues
echo "📥 Creating queues..."

# Dead Letter Queue
rabbitmq_api PUT "/queues$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/gasolinera.dlq" \
    '{"durable":true,"auto_delete":false,"arguments":{"x-message-ttl":86400000,"x-max-length":10000}}'

# Redemption Queues
rabbitmq_api PUT "/queues$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/redemption.created.queue" \
    '{"durable":true,"auto_delete":false,"arguments":{"x-dead-letter-exchange":"gasolinera.dlx","x-dead-letter-routing-key":"redemption.created.failed","x-message-ttl":3600000}}'

rabbitmq_api PUT "/queues$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/redemption.completed.queue" \
    '{"durable":true,"auto_delete":false,"arguments":{"x-dead-letter-exchange":"gasolinera.dlx","x-dead-letter-routing-key":"redemption.completed.failed","x-message-ttl":3600000}}'

rabbitmq_api PUT "/queues$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/redemption.voided.queue" \
    '{"durable":true,"auto_delete":false,"arguments":{"x-dead-letter-exchange":"gasolinera.dlx","x-dead-letter-routing-key":"redemption.voided.failed","x-message-ttl":3600000}}'

# Raffle Queues
rabbitmq_api PUT "/queues$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/raffle.tickets.generated.queue" \
    '{"durable":true,"auto_delete":false,"arguments":{"x-dead-letter-exchange":"gasolinera.dlx","x-dead-letter-routing-key":"raffle.tickets.generated.failed","x-message-ttl":3600000}}'

rabbitmq_api PUT "/queues$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/raffle.entry.created.queue" \
    '{"durable":true,"auto_delete":false,"arguments":{"x-dead-letter-exchange":"gasolinera.dlx","x-dead-letter-routing-key":"raffle.entry.created.failed","x-message-ttl":3600000}}'

rabbitmq_api PUT "/queues$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/raffle.winner.selected.queue" \
    '{"durable":true,"auto_delete":false,"arguments":{"x-dead-letter-exchange":"gasolinera.dlx","x-dead-letter-routing-key":"raffle.winner.selected.failed","x-message-ttl":3600000}}'

# Ad Engine Queues
rabbitmq_api PUT "/queues$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/ad.engagement.completed.queue" \
    '{"durable":true,"auto_delete":false,"arguments":{"x-dead-letter-exchange":"gasolinera.dlx","x-dead-letter-routing-key":"ad.engagement.completed.failed","x-message-ttl":3600000}}'

rabbitmq_api PUT "/queues$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/ad.tickets.multiplied.queue" \
    '{"durable":true,"auto_delete":false,"arguments":{"x-dead-letter-exchange":"gasolinera.dlx","x-dead-letter-routing-key":"ad.tickets.multiplied.failed","x-message-ttl":3600000}}'

# Coupon Queues
rabbitmq_api PUT "/queues$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/coupon.validated.queue" \
    '{"durable":true,"auto_delete":false,"arguments":{"x-dead-letter-exchange":"gasolinera.dlx","x-dead-letter-routing-key":"coupon.validated.failed","x-message-ttl":3600000}}'

rabbitmq_api PUT "/queues$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/coupon.redeemed.queue" \
    '{"durable":true,"auto_delete":false,"arguments":{"x-dead-letter-exchange":"gasolinera.dlx","x-dead-letter-routing-key":"coupon.redeemed.failed","x-message-ttl":3600000}}'

# Audit Queues
rabbitmq_api PUT "/queues$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/audit.events.queue" \
    '{"durable":true,"auto_delete":false,"arguments":{"x-message-ttl":604800000,"x-max-length":100000}}'

rabbitmq_api PUT "/queues$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/audit.security.queue" \
    '{"durable":true,"auto_delete":false,"arguments":{"x-message-ttl":2592000000,"x-max-length":50000}}'

echo "✅ Queues created successfully!"

# Create Bindings
echo "🔗 Creating bindings..."

# Redemption bindings
rabbitmq_api POST "/bindings$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/e/redemption.exchange/q/redemption.created.queue" \
    '{"routing_key":"redemption.created"}'

rabbitmq_api POST "/bindings$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/e/redemption.exchange/q/redemption.completed.queue" \
    '{"routing_key":"redemption.completed"}'

rabbitmq_api POST "/bindings$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/e/redemption.exchange/q/redemption.voided.queue" \
    '{"routing_key":"redemption.voided"}'

# Cross-service binding for raffle tickets from redemptions
rabbitmq_api POST "/bindings$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/e/redemption.exchange/q/raffle.tickets.generated.queue" \
    '{"routing_key":"raffle.tickets.generated"}'

# Ad Engine bindings
rabbitmq_api POST "/bindings$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/e/ad.exchange/q/ad.engagement.completed.queue" \
    '{"routing_key":"ad.engagement.completed"}'

rabbitmq_api POST "/bindings$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/e/ad.exchange/q/ad.tickets.multiplied.queue" \
    '{"routing_key":"ad.tickets.multiplied"}'

# Cross-service binding for raffle tickets from ad engine
rabbitmq_api POST "/bindings$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/e/ad.exchange/q/raffle.tickets.generated.queue" \
    '{"routing_key":"raffle.tickets.generated"}'

# Raffle bindings
rabbitmq_api POST "/bindings$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/e/raffle.exchange/q/raffle.entry.created.queue" \
    '{"routing_key":"raffle.entry.created"}'

rabbitmq_api POST "/bindings$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/e/raffle.exchange/q/raffle.winner.selected.queue" \
    '{"routing_key":"raffle.winner.selected"}'

# Coupon bindings
rabbitmq_api POST "/bindings$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/e/coupon.exchange/q/coupon.validated.queue" \
    '{"routing_key":"coupon.validated"}'

rabbitmq_api POST "/bindings$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/e/coupon.exchange/q/coupon.redeemed.queue" \
    '{"routing_key":"coupon.redeemed"}'

# Audit bindings
rabbitmq_api POST "/bindings$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/e/audit.exchange/q/audit.events.queue" \
    '{"routing_key":"audit.#"}'

rabbitmq_api POST "/bindings$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/e/audit.exchange/q/audit.security.queue" \
    '{"routing_key":"audit.security.#"}'

# Dead letter bindings
rabbitmq_api POST "/bindings$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/e/gasolinera.dlx/q/gasolinera.dlq" \
    '{"routing_key":"#"}'

echo "✅ Bindings created successfully!"

# Enable management plugin policies
echo "📋 Setting up policies..."

# Set HA policy for high availability
rabbitmq_api PUT "/policies$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/ha-all" \
    '{"pattern":".*","definition":{"ha-mode":"all","ha-sync-mode":"automatic"},"priority":0,"apply-to":"queues"}'

# Set TTL policy for temporary queues
rabbitmq_api PUT "/policies$(echo $RABBITMQ_VHOST | sed 's/\//%2F/g')/ttl-policy" \
    '{"pattern":".*\\.temp\\..*","definition":{"message-ttl":300000,"expires":600000},"priority":1,"apply-to":"queues"}'

echo "✅ Policies configured successfully!"

echo ""
echo "🎉 RabbitMQ setup completed successfully!"
echo ""
echo "📊 Summary:"
echo "  - Virtual Host: $RABBITMQ_VHOST"
echo "  - Exchanges: 7 (including DLX)"
echo "  - Queues: 13 (including DLQ)"
echo "  - Bindings: 15+"
echo "  - Policies: 2"
echo ""
echo "🔍 You can view the configuration at:"
echo "  http://$RABBITMQ_HOST:$RABBITMQ_PORT"
echo ""
echo "✨ Ready for event-driven communication!"