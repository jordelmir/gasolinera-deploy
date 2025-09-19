#!/bin/bash

# Script para iniciar todos los servicios de Gasolinera JSM
echo "🚀 Iniciando servicios de Gasolinera JSM..."

# Verificar que la infraestructura esté funcionando
echo "📋 Verificando infraestructura..."
docker-compose -f docker-compose.simple.yml ps

# Función para iniciar un servicio en background
start_service() {
    local service_name=$1
    local port=$2
    local jar_path=$3

    echo "🔄 Iniciando $service_name en puerto $port..."

    # Matar proceso existente si existe
    pkill -f "$jar_path" 2>/dev/null || true

    # Iniciar servicio
    nohup java -jar "$jar_path" --server.port=$port > "logs/${service_name}.log" 2>&1 &

    echo "✅ $service_name iniciado (PID: $!)"
    sleep 2
}

# Crear directorio de logs
mkdir -p logs

# Iniciar servicios en orden de dependencias
echo "🔄 Iniciando servicios..."

# 1. Auth Service (otros servicios dependen de este)
start_service "auth-service" 8081 "services/auth-service/build/libs/app.jar"

# 2. Station Service
start_service "station-service" 8082 "services/station-service/build/libs/app.jar"

# 3. Coupon Service
start_service "coupon-service" 8083 "services/coupon-service/build/libs/app.jar"

# 4. Raffle Service
start_service "raffle-service" 8084 "services/raffle-service/build/libs/app.jar"

# 5. Redemption Service
start_service "redemption-service" 8085 "services/redemption-service/build/libs/app.jar"

# 6. Ad Engine Service
start_service "ad-engine" 8086 "services/ad-engine/build/libs/app.jar"

# 7. Message Improver Service
start_service "message-improver" 8087 "services/message-improver/build/libs/app.jar"

# 8. API Gateway (último, ya que enruta a otros servicios)
start_service "api-gateway" 8080 "services/api-gateway/build/libs/app.jar"

echo ""
echo "🎉 Todos los servicios iniciados!"
echo ""
echo "📋 URLs de los servicios:"
echo "   🌐 API Gateway:        http://localhost:8080"
echo "   🔐 Auth Service:       http://localhost:8081"
echo "   ⛽ Station Service:    http://localhost:8082"
echo "   🎫 Coupon Service:     http://localhost:8083"
echo "   🎲 Raffle Service:     http://localhost:8084"
echo "   💰 Redemption Service: http://localhost:8085"
echo "   📢 Ad Engine Service:  http://localhost:8086"
echo "   💬 Message Improver:   http://localhost:8087"
echo ""
echo "📋 Infraestructura:"
echo "   🐘 PostgreSQL:         localhost:5432"
echo "   🔴 Redis:              localhost:6379"
echo "   🐰 RabbitMQ:           http://localhost:15672"
echo ""
echo "📝 Para ver logs: tail -f logs/[service-name].log"
echo "🛑 Para detener: ./stop-services.sh"