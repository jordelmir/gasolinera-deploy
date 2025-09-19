#!/bin/bash

# Script para detener todos los servicios de Gasolinera JSM
echo "🛑 Deteniendo servicios de Gasolinera JSM..."

# Detener servicios Java
echo "🔄 Deteniendo servicios Java..."
pkill -f "auth-service" 2>/dev/null || true
pkill -f "station-service" 2>/dev/null || true
pkill -f "coupon-service" 2>/dev/null || true
pkill -f "raffle-service" 2>/dev/null || true
pkill -f "redemption-service" 2>/dev/null || true
pkill -f "ad-engine" 2>/dev/null || true
pkill -f "message-improver" 2>/dev/null || true
pkill -f "api-gateway" 2>/dev/null || true

# Detener infraestructura Docker
echo "🔄 Deteniendo infraestructura Docker..."
docker-compose -f docker-compose.simple.yml down

echo "✅ Todos los servicios detenidos!"