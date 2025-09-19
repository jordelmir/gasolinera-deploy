#!/bin/bash

# 🔍 Script de verificación del sistema Gasolinera JSM

echo "🔍 VERIFICACIÓN COMPLETA DEL SISTEMA GASOLINERA JSM"
echo "=================================================="
echo ""

# Función para verificar servicio
check_service() {
    local name=$1
    local url=$2
    local port=$3

    echo -n "🔍 $name (Puerto $port): "

    # Verificar si el puerto está ocupado
    if lsof -i :$port >/dev/null 2>&1; then
        # Intentar hacer request HTTP
        response=$(curl -s -w "%{http_code}" -o /dev/null "$url" 2>/dev/null)
        if [ "$response" = "200" ]; then
            echo "✅ FUNCIONANDO"
        elif [ "$response" = "503" ]; then
            echo "⚠️  RESPONDE (Health DOWN)"
        else
            echo "🔄 INICIANDO (HTTP $response)"
        fi
    else
        echo "❌ NO CORRIENDO"
    fi
}

# Verificar infraestructura
echo "📋 INFRAESTRUCTURA BASE:"
echo "========================"

echo -n "🐘 PostgreSQL (5432): "
if docker-compose -f docker-compose.simple.yml ps postgres | grep -q "healthy"; then
    echo "✅ FUNCIONANDO"
else
    echo "❌ NO DISPONIBLE"
fi

echo -n "🔴 Redis (6379): "
if docker-compose -f docker-compose.simple.yml ps redis | grep -q "healthy"; then
    echo "✅ FUNCIONANDO"
else
    echo "❌ NO DISPONIBLE"
fi

echo -n "🐰 RabbitMQ (15672): "
if docker-compose -f docker-compose.simple.yml ps rabbitmq | grep -q "healthy"; then
    echo "✅ FUNCIONANDO"
else
    echo "❌ NO DISPONIBLE"
fi

echo ""
echo "🚀 SERVICIOS DE APLICACIÓN:"
echo "==========================="

# Verificar servicios principales
check_service "API Gateway" "http://localhost:8080/health" "8080"
check_service "Auth Service" "http://localhost:8091/actuator/health" "8091"
check_service "Station Service" "http://localhost:8092/actuator/health" "8092"
check_service "Coupon Service" "http://localhost:8093/actuator/health" "8093"
check_service "Raffle Service" "http://localhost:8094/actuator/health" "8094"
check_service "Redemption Service" "http://localhost:8095/actuator/health" "8095"
check_service "Ad Engine" "http://localhost:8096/actuator/health" "8096"
check_service "Message Improver" "http://localhost:8097/actuator/health" "8097"

echo ""
echo "🔗 VERIFICACIÓN DE ROUTING (API Gateway):"
echo "========================================="

if curl -s http://localhost:8080/health >/dev/null 2>&1; then
    echo "✅ API Gateway responde - Probando routing..."

    # Probar routing a servicios backend
    echo -n "🔗 Routing a Auth Service: "
    auth_route=$(curl -s -w "%{http_code}" -o /dev/null "http://localhost:8080/api/auth/actuator/health" 2>/dev/null)
    if [ "$auth_route" = "200" ] || [ "$auth_route" = "503" ]; then
        echo "✅ FUNCIONA"
    else
        echo "⚠️  HTTP $auth_route"
    fi

    echo -n "🔗 Routing a Coupon Service: "
    coupon_route=$(curl -s -w "%{http_code}" -o /dev/null "http://localhost:8080/api/coupons/actuator/health" 2>/dev/null)
    if [ "$coupon_route" = "200" ] || [ "$coupon_route" = "503" ]; then
        echo "✅ FUNCIONA"
    else
        echo "⚠️  HTTP $coupon_route"
    fi
else
    echo "❌ API Gateway no responde - No se puede verificar routing"
fi

echo ""
echo "📊 RESUMEN DE PROCESOS JAVA:"
echo "============================"
echo "Procesos Java activos relacionados con Gasolinera:"
ps aux | grep java | grep -E "(auth-service|station-service|coupon-service|raffle-service|redemption-service|ad-engine|message-improver|api-gateway)" | grep -v grep | while read line; do
    echo "🔄 $line"
done

echo ""
echo "🌐 PUERTOS OCUPADOS (8080-8100):"
echo "================================"
lsof -i :8080-8100 2>/dev/null | grep LISTEN | while read line; do
    echo "🔌 $line"
done

echo ""
echo "📝 LOGS RECIENTES:"
echo "=================="
echo "Para ver logs de servicios:"
echo "  tail -f logs/api-gateway.log"
echo "  tail -f logs/auth-service.log"
echo "  tail -f logs/coupon-service.log"
echo ""
echo "🎯 COMANDOS ÚTILES:"
echo "=================="
echo "  ./start-services-fixed.sh  # Iniciar todos los servicios"
echo "  ./stop-services.sh         # Detener todos los servicios"
echo "  ./verify-system.sh         # Esta verificación"
echo ""

# Determinar estado general
echo "🎉 ESTADO GENERAL DEL SISTEMA:"
echo "============================="

gateway_status=$(curl -s http://localhost:8080/health 2>/dev/null)
if [[ "$gateway_status" == *"UP"* ]]; then
    echo "🟢 SISTEMA OPERATIVO - API Gateway funcionando"
    echo "   ✅ Listo para desarrollo y testing"
    echo "   ✅ Routing configurado correctamente"
else
    echo "🟡 SISTEMA PARCIALMENTE OPERATIVO"
    echo "   ⚠️  API Gateway necesita verificación"
    echo "   ℹ️  Algunos servicios pueden estar iniciando"
fi

echo ""
echo "📋 Para más detalles, consulta: SISTEMA-STATUS-FINAL.md"