#!/bin/bash
# RabbitMQ Initialization Script for Gasolinera JSM Platform

set -e

echo "Initializing RabbitMQ for Gasolinera JSM Platform..."

# Wait for RabbitMQ to be ready
until rabbitmqctl status; do
  echo "Waiting for RabbitMQ to start..."
  sleep 2
done

echo "RabbitMQ is ready. Setting up exchanges, queues, and bindings..."

# Create exchanges
rabbitmqadmin declare exchange name=gasolinera.events type=topic durable=true
rabbitmqadmin declare exchange name=gasolinera.dlx type=direct durable=true

# Create queues for each service
# Coupon events
rabbitmqadmin declare queue name=coupon.redeemed durable=true arguments='{"x-dead-letter-exchange":"gasolinera.dlx","x-dead-letter-routing-key":"coupon.redeemed.dlq","x-message-ttl":3600000}'
rabbitmqadmin declare queue name=coupon.generated durable=true arguments='{"x-dead-letter-exchange":"gasolinera.dlx","x-dead-letter-routing-key":"coupon.generated.dlq","x-message-ttl":3600000}'

# Redemption events
rabbitmqadmin declare queue name=redemption.completed durable=true arguments='{"x-dead-letter-exchange":"gasolinera.dlx","x-dead-letter-routing-key":"redemption.completed.dlq","x-message-ttl":3600000}'
rabbitmqadmin declare queue name=redemption.failed durable=true arguments='{"x-dead-letter-exchange":"gasolinera.dlx","x-dead-letter-routing-key":"redemption.failed.dlq","x-message-ttl":3600000}'

# Ad engagement events
rabbitmqadmin declare queue name=ad.completed durable=true arguments='{"x-dead-letter-exchange":"gasolinera.dlx","x-dead-letter-routing-key":"ad.completed.dlq","x-message-ttl":3600000}'
rabbitmqadmin declare queue name=ad.started durable=true arguments='{"x-dead-letter-exchange":"gasolinera.dlx","x-dead-letter-routing-key":"ad.started.dlq","x-message-ttl":3600000}'

# Raffle events
rabbitmqadmin declare queue name=raffle.ticket.generated durable=true arguments='{"x-dead-letter-exchange":"gasolinera.dlx","x-dead-letter-routing-key":"raffle.ticket.generated.dlq","x-message-ttl":3600000}'
rabbitmqadmin declare queue name=raffle.draw.completed durable=true arguments='{"x-dead-letter-exchange":"gasolinera.dlx","x-dead-letter-routing-key":"raffle.draw.completed.dlq","x-message-ttl":3600000}'

# Audit events
rabbitmqadmin declare queue name=audit.events durable=true arguments='{"x-dead-letter-exchange":"gasolinera.dlx","x-dead-letter-routing-key":"audit.events.dlq","x-message-ttl":7200000}'

# Dead letter queues
rabbitmqadmin declare queue name=coupon.redeemed.dlq durable=true
rabbitmqadmin declare queue name=coupon.generated.dlq durable=true
rabbitmqadmin declare queue name=redemption.completed.dlq durable=true
rabbitmqadmin declare queue name=redemption.failed.dlq durable=true
rabbitmqadmin declare queue name=ad.completed.dlq durable=true
rabbitmqadmin declare queue name=ad.started.dlq durable=true
rabbitmqadmin declare queue name=raffle.ticket.generated.dlq durable=true
rabbitmqadmin declare queue name=raffle.draw.completed.dlq durable=true
rabbitmqadmin declare queue name=audit.events.dlq durable=true

# Create bindings
# Coupon service bindings
rabbitmqadmin declare binding source=gasolinera.events destination=coupon.redeemed routing_key=coupon.redeemed
rabbitmqadmin declare binding source=gasolinera.events destination=coupon.generated routing_key=coupon.generated

# Redemption service bindings
rabbitmqadmin declare binding source=gasolinera.events destination=redemption.completed routing_key=redemption.completed
rabbitmqadmin declare binding source=gasolinera.events destination=redemption.failed routing_key=redemption.failed

# Ad engine bindings
rabbitmqadmin declare binding source=gasolinera.events destination=ad.completed routing_key=ad.completed
rabbitmqadmin declare binding source=gasolinera.events destination=ad.started routing_key=ad.started

# Raffle service bindings
rabbitmqadmin declare binding source=gasolinera.events destination=raffle.ticket.generated routing_key=raffle.ticket.generated
rabbitmqadmin declare binding source=gasolinera.events destination=raffle.draw.completed routing_key=raffle.draw.completed

# Audit bindings (catch all events)
rabbitmqadmin declare binding source=gasolinera.events destination=audit.events routing_key=#

# Dead letter bindings
rabbitmqadmin declare binding source=gasolinera.dlx destination=coupon.redeemed.dlq routing_key=coupon.redeemed.dlq
rabbitmqadmin declare binding source=gasolinera.dlx destination=coupon.generated.dlq routing_key=coupon.generated.dlq
rabbitmqadmin declare binding source=gasolinera.dlx destination=redemption.completed.dlq routing_key=redemption.completed.dlq
rabbitmqadmin declare binding source=gasolinera.dlx destination=redemption.failed.dlq routing_key=redemption.failed.dlq
rabbitmqadmin declare binding source=gasolinera.dlx destination=ad.completed.dlq routing_key=ad.completed.dlq
rabbitmqadmin declare binding source=gasolinera.dlx destination=ad.started.dlq routing_key=ad.started.dlq
rabbitmqadmin declare binding source=gasolinera.dlx destination=raffle.ticket.generated.dlq routing_key=raffle.ticket.generated.dlq
rabbitmqadmin declare binding source=gasolinera.dlx destination=raffle.draw.completed.dlq routing_key=raffle.draw.completed.dlq
rabbitmqadmin declare binding source=gasolinera.dlx destination=audit.events.dlq routing_key=audit.events.dlq

# Set up policies for high availability and performance
rabbitmqctl set_policy ha-all "^gasolinera\." '{"ha-mode":"all","ha-sync-mode":"automatic"}'
rabbitmqctl set_policy ttl-policy "^gasolinera\." '{"message-ttl":3600000}' --apply-to queues

echo "RabbitMQ initialization completed successfully!"
echo "Exchanges created: gasolinera.events, gasolinera.dlx"
echo "Queues created for: coupon, redemption, ad, raffle, audit services"
echo "Dead letter queues configured for message recovery"
echo "High availability policies applied"