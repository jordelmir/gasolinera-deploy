#!/bin/bash

# 🚀 Script de inicio de servicios Gasolinera JSM - Versión Corregida
# Usa puertos alternativos para evitar conflictos

echo "🚀 Iniciando servicios de Gasolinera JSM (Puertos Corregidos)..."

# Verificar infraestructura
echo "📋 Verificando infraestructura..."
docker-compose -f docker-compose.simple.yml ps

# Crear directorio de logs si no existe
mkdir -p logs

echo "🔄 Iniciando servicios en puertos alternativos..."

# Auth Service - Puerto 8091
echo "🔄 Iniciando auth-service en puerto 8091..."
nohup java -jar "services/auth-service/build/libs/auth-service.jar" \
    --server.port=8091 \
    --spring.main.allow-bean-definition-overriding=true \
    > "logs/auth-service.log" 2>&1 &
AUTH_PID=$!
echo "✅ auth-service iniciado (PID: $AUTH_PID)"

# Station Service - Puerto 8092
echo "🔄 Iniciando station-service en puerto 8092..."
nohup java -jar "services/station-service/build/libs/station-service.jar" \
    --server.port=8092 \
    --spring.main.allow-bean-definition-overriding=true \
    > "logs/station-service.log" 2>&1 &
STATION_PID=$!
echo "✅ station-service iniciado (PID: $STATION_PID)"

# Coupon Service - Puerto 8093
echo "🔄 Iniciando coupon-service en puerto 8093..."
nohup java -jar "services/coupon-service/build/libs/coupon-service.jar" \
    --server.port=8093 \
    --spring.main.allow-bean-definition-overriding=true \
    > "logs/coupon-service.log" 2>&1 &
COUPON_PID=$!
echo "✅ coupon-service iniciado (PID: $COUPON_PID)"

# Raffle Service - Puerto 8094
echo "🔄 Iniciando raffle-service en puerto 8094..."
nohup java -jar "services/raffle-service/build/libs/raffle-service.jar" \
    --server.port=8094 \
    --spring.main.allow-bean-definition-overriding=true \
    > "logs/raffle-service.log" 2>&1 &
RAFFLE_PID=$!
echo "✅ raffle-service iniciado (PID: $RAFFLE_PID)"

# Redemption Service - Puerto 8095
echo "🔄 Iniciando redemption-service en puerto 8095..."
nohup java -jar "services/redemption-service/build/libs/redemption-service.jar" \
    --server.port=8095 \
    --spring.main.allow-bean-definition-overriding=true \
    > "logs/redemption-service.log" 2>&1 &
REDEMPTION_PID=$!
echo "✅ redemption-service iniciado (PID: $REDEMPTION_PID)"

# Ad Engine - Puerto 8096
echo "🔄 Iniciando ad-engine en puerto 8096..."
nohup java -jar "services/ad-engine/build/libs/ad-engine.jar" \
    --server.port=8096 \
    --spring.main.allow-bean-definition-overriding=true \
    > "logs/ad-engine.log" 2>&1 &
AD_PID=$!
echo "✅ ad-engine iniciado (PID: $AD_PID)"

# Message Improver - Puerto 8097
echo "🔄 Iniciando message-improver en puerto 8097..."
nohup java -jar "services/message-improver/build/libs/message-improver.jar" \
    --server.port=8097 \
    --spring.main.allow-bean-definition-overriding=true \
    > "logs/message-improver.log" 2>&1 &
MESSAGE_PID=$!
echo "✅ message-improver iniciado (PID: $MESSAGE_PID)"

# API Gateway - Puerto 8080 (último para que tenga las rutas actualizadas)
echo "🔄 Iniciando api-gateway en puerto 8080..."
nohup java -jar "services/api-gateway/build/libs/app.jar" \
    --server.port=8080 \
    --spring.main.allow-bean-definition-overriding=true \
    > "logs/api-gateway.log" 2>&1 &
GATEWAY_PID=$!
echo "✅ api-gateway iniciado (PID: $GATEWAY_PID)"

echo ""
echo "🎉 Todos los servicios iniciados!"
echo ""
echo "📋 URLs de los servicios (PUERTOS CORREGIDOS):"
echo "   🌐 API Gateway:        http://localhost:8080"
echo "   🔐 Auth Service:       http://localhost:8091"
echo "   ⛽ Station Service:    http://localhost:8092"
echo "   🎫 Coupon Service:     http://localhost:8093"
echo "   🎲 Raffle Service:     http://localhost:8094"
echo "   💰 Redemption Service: http://localhost:8095"
echo "   📢 Ad Engine Service:  http://localhost:8096"
echo "   💬 Message Improver:   http://localhost:8097"
echo ""
echo "📋 Infraestructura:"
echo "   🐘 PostgreSQL:         localhost:5432"
echo "   🔴 Redis:              localhost:6379"
echo "   🐰 RabbitMQ:           http://localhost:15672"
echo ""
echo "📝 Para ver logs: tail -f logs/[service-name].log"
echo "🛑 Para detener: ./stop-services.sh"
echo ""
echo "⏳ Esperando 30 segundos para que los servicios inicien..."
sleep 30
echo ""
echo "🔍 Verificando estado de servicios:"
echo "API Gateway: $(curl -s http://localhost:8080/health 2>/dev/null || echo 'No responde')"
echo "Auth Service: $(curl -s http://localhost:8091/actuator/health 2>/dev/null || echo 'No responde')"
echo "Coupon Service: $(curl -s http://localhost:8093/actuator/health 2>/dev/null || echo 'No responde')"