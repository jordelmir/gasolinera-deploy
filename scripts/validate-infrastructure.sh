#!/bin/bash

# Script para validar que toda la infraestructura esté funcionando correctamente
# Uso: ./validate-infrastructure.sh [--detailed] [--fix-issues]

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Variables
DETAILED=false
FIX_ISSUES=false
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Procesar argumentos
for arg in "$@"; do
    case $arg in
        --detailed)
            DETAILED=true
            shift
            ;;
        --fix-issues)
            FIX_ISSUES=true
            shift
            ;;
    esac
done

# Contadores
TOTAL_CHECKS=0
PASSED_CHECKS=0
FAILED_CHECKS=0
WARNING_CHECKS=0

echo -e "${BLUE}🔍 Validando infraestructura de Gasolinera JSM${NC}"
echo ""

# Funciones de utilidad
show_progress() {
    echo -e "${GREEN}✅ $1${NC}"
    ((PASSED_CHECKS++))
}

show_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
    ((WARNING_CHECKS++))
}

show_error() {
    echo -e "${RED}❌ $1${NC}"
    ((FAILED_CHECKS++))
}

show_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

run_check() {
    ((TOTAL_CHECKS++))
    if [ "$DETAILED" = true ]; then
        echo -e "${BLUE}Ejecutando: $1${NC}"
    fi
}

# Verificar variables de entorno
check_environment_variables() {
    show_info "Verificando variables de entorno..."

    cd "$PROJECT_ROOT"

    run_check "Archivo .env existe"
    if [ -f ".env" ]; then
        show_progress "Archivo .env encontrado"
    else
        show_error "Archivo .env no encontrado"
        return
    fi

    # Cargar variables
    source .env

    # Variables críticas
    critical_vars=(
        "DB_HOST" "DB_NAME" "DB_USERNAME" "DB_PASSWORD"
        "JWT_SECRET" "REDIS_HOST" "RABBITMQ_HOST"
        "AUTH_SERVICE_URL" "API_GATEWAY_URL"
    )

    for var in "${critical_vars[@]}"; do
        run_check "Variable $var está definida"
        if [ -n "${!var}" ]; then
            show_progress "Variable $var: ✓"
        else
            show_error "Variable $var no está definida"
        fi
    done

    # Verificar JWT_SECRET no sea el valor por defecto
    run_check "JWT_SECRET no es valor por defecto"
    if [[ "$JWT_SECRET" == *"change-this-in-production"* ]]; then
        show_warning "JWT_SECRET usa valor por defecto - cambiar en producción"
    else
        show_progress "JWT_SECRET configurado correctamente"
    fi
}

# Verificar servicios Docker
check_docker_services() {
    show_info "Verificando servicios Docker..."

    cd "$PROJECT_ROOT"

    run_check "Docker está funcionando"
    if docker info &> /dev/null; then
        show_progress "Docker está funcionando"
    else
        show_error "Docker no está funcionando"
        return
    fi

    run_check "Docker Compose está disponible"
    if command -v docker-compose &> /dev/null; then
        show_progress "Docker Compose disponible"
    else
        show_error "Docker Compose no está disponible"
        return
    fi

    # Verificar servicios específicos
    services=("postgres" "redis" "rabbitmq")

    for service in "${services[@]}"; do
        run_check "Servicio $service está funcionando"
        if docker-compose -f docker-compose.simple.yml ps "$service" | grep -q "Up"; then
            show_progress "Servicio $service: ✓"
        else
            show_error "Servicio $service no está funcionando"
            if [ "$FIX_ISSUES" = true ]; then
                show_info "Intentando reiniciar $service..."
                docker-compose -f docker-compose.simple.yml restart "$service"
            fi
        fi
    done
}

# Verificar conectividad de base de datos
check_database_connectivity() {
    show_info "Verificando conectividad de base de datos..."

    cd "$PROJECT_ROOT"
    source .env

    run_check "PostgreSQL está respondiendo"
    if docker-compose -f docker-compose.simple.yml exec -T postgres pg_isready -U "$DB_USERNAME" -d "$DB_NAME" &> /dev/null; then
        show_progress "PostgreSQL está respondiendo"
    else
        show_error "PostgreSQL no está respondiendo"
        if [ "$FIX_ISSUES" = true ]; then
            show_info "Intentando reiniciar PostgreSQL..."
            docker-compose -f docker-compose.simple.yml restart postgres
            sleep 10
        fi
        return
    fi

    run_check "Puede conectar a la base de datos"
    if docker-compose -f docker-compose.simple.yml exec -T postgres psql -U "$DB_USERNAME" -d "$DB_NAME" -c "SELECT 1;" &> /dev/null; then
        show_progress "Conexión a BD exitosa"
    else
        show_error "No se puede conectar a la base de datos"
        return
    fi

    # Verificar esquemas
    schemas=("auth_schema" "station_schema" "coupon_schema" "raffle_schema")
    for schema in "${schemas[@]}"; do
        run_check "Esquema $schema existe"
        if docker-compose -f docker-compose.simple.yml exec -T postgres psql -U "$DB_USERNAME" -d "$DB_NAME" -c "SELECT schema_name FROM information_schema.schemata WHERE schema_name = '$schema';" | grep -q "$schema"; then
            show_progress "Esquema $schema: ✓"
        else
            show_warning "Esquema $schema no encontrado"
        fi
    done
}

# Verificar Redis
check_redis_connectivity() {
    show_info "Verificando conectividad de Redis..."

    cd "$PROJECT_ROOT"

    run_check "Redis está respondiendo"
    if docker-compose -f docker-compose.simple.yml exec -T redis redis-cli ping | grep -q "PONG"; then
        show_progress "Redis está respondiendo"
    else
        show_error "Redis no está respondiendo"
        if [ "$FIX_ISSUES" = true ]; then
            show_info "Intentando reiniciar Redis..."
            docker-compose -f docker-compose.simple.yml restart redis
        fi
        return
    fi

    run_check "Redis puede almacenar datos"
    if docker-compose -f docker-compose.simple.yml exec -T redis redis-cli set test_key "test_value" | grep -q "OK"; then
        show_progress "Redis puede almacenar datos"
        docker-compose -f docker-compose.simple.yml exec -T redis redis-cli del test_key &> /dev/null
    else
        show_error "Redis no puede almacenar datos"
    fi
}

# Verificar RabbitMQ
check_rabbitmq_connectivity() {
    show_info "Verificando conectividad de RabbitMQ..."

    cd "$PROJECT_ROOT"
    source .env

    run_check "RabbitMQ está funcionando"
    if docker-compose -f docker-compose.simple.yml exec -T rabbitmq rabbitmqctl status &> /dev/null; then
        show_progress "RabbitMQ está funcionando"
    else
        show_error "RabbitMQ no está funcionando"
        if [ "$FIX_ISSUES" = true ]; then
            show_info "Intentando reiniciar RabbitMQ..."
            docker-compose -f docker-compose.simple.yml restart rabbitmq
        fi
        return
    fi

    run_check "Virtual host existe"
    if docker-compose -f docker-compose.simple.yml exec -T rabbitmq rabbitmqctl list_vhosts | grep -q "$RABBITMQ_VIRTUAL_HOST"; then
        show_progress "Virtual host configurado"
    else
        show_warning "Virtual host no encontrado"
        if [ "$FIX_ISSUES" = true ]; then
            show_info "Creando virtual host..."
            docker-compose -f docker-compose.simple.yml exec -T rabbitmq rabbitmqctl add_vhost "$RABBITMQ_VIRTUAL_HOST"
        fi
    fi
}

# Verificar claves de seguridad
check_security_keys() {
    show_info "Verificando claves de seguridad..."

    cd "$PROJECT_ROOT"

    run_check "Clave privada QR existe"
    if [ -f "ops/key-management/private-key.pem" ]; then
        show_progress "Clave privada QR: ✓"
    else
        show_error "Clave privada QR no encontrada"
        if [ "$FIX_ISSUES" = true ]; then
            show_info "Generando claves QR..."
            cd ops/key-management
            openssl genrsa -out private-key.pem 2048
            openssl rsa -in private-key.pem -pubout -out public-key.pem
            chmod 600 private-key.pem
            chmod 644 public-key.pem
            cd "$PROJECT_ROOT"
        fi
    fi

    run_check "Clave pública QR existe"
    if [ -f "ops/key-management/public-key.pem" ]; then
        show_progress "Clave pública QR: ✓"
    else
        show_error "Clave pública QR no encontrada"
    fi

    # Verificar permisos
    if [ -f "ops/key-management/private-key.pem" ]; then
        run_check "Permisos de clave privada son seguros"
        perms=$(stat -c "%a" ops/key-management/private-key.pem)
        if [ "$perms" = "600" ]; then
            show_progress "Permisos de clave privada: ✓"
        else
            show_warning "Permisos de clave privada no son seguros ($perms)"
            if [ "$FIX_ISSUES" = true ]; then
                chmod 600 ops/key-management/private-key.pem
                show_info "Permisos corregidos"
            fi
        fi
    fi
}

# Verificar directorios
check_directories() {
    show_info "Verificando estructura de directorios..."

    cd "$PROJECT_ROOT"

    directories=(
        "logs" "logs/requests" "logs/responses" "logs/errors"
        "data/uploads" "data/exports" "data/backups"
        "monitoring/prometheus" "monitoring/grafana"
    )

    for dir in "${directories[@]}"; do
        run_check "Directorio $dir existe"
        if [ -d "$dir" ]; then
            show_progress "Directorio $dir: ✓"
        else
            show_warning "Directorio $dir no existe"
            if [ "$FIX_ISSUES" = true ]; then
                mkdir -p "$dir"
                show_info "Directorio $dir creado"
            fi
        fi
    done
}

# Verificar puertos
check_ports() {
    show_info "Verificando puertos..."

    ports=(
        "5432:PostgreSQL"
        "6379:Redis"
        "5672:RabbitMQ"
        "15672:RabbitMQ Management"
    )

    for port_info in "${ports[@]}"; do
        port=$(echo "$port_info" | cut -d: -f1)
        service=$(echo "$port_info" | cut -d: -f2)

        run_check "Puerto $port ($service) está disponible"
        if netstat -tuln 2>/dev/null | grep -q ":$port " || ss -tuln 2>/dev/null | grep -q ":$port "; then
            show_progress "Puerto $port ($service): ✓"
        else
            show_warning "Puerto $port ($service) no está en uso"
        fi
    done
}

# Verificar configuración de monitoreo
check_monitoring_config() {
    show_info "Verificando configuración de monitoreo..."

    cd "$PROJECT_ROOT"

    run_check "Configuración de Prometheus existe"
    if [ -f "monitoring/prometheus/prometheus.yml" ]; then
        show_progress "Configuración de Prometheus: ✓"
    else
        show_warning "Configuración de Prometheus no encontrada"
    fi

    run_check "Reglas de alertas existen"
    if [ -f "monitoring/prometheus/alert_rules.yml" ]; then
        show_progress "Reglas de alertas: ✓"
    else
        show_warning "Reglas de alertas no encontradas"
    fi
}

# Verificar health checks
check_health_endpoints() {
    show_info "Verificando endpoints de health check..."

    # Esta verificación solo funciona si los servicios están ejecutándose
    services=(
        "8080:API Gateway"
        "8081:Auth Service"
        "8082:Station Service"
        "8083:Coupon Service"
        "8084:Raffle Service"
    )

    for service_info in "${services[@]}"; do
        port=$(echo "$service_info" | cut -d: -f1)
        service=$(echo "$service_info" | cut -d: -f2)

        run_check "Health check $service (puerto $port)"
        if curl -s -f "http://localhost:$port/health" &> /dev/null; then
            show_progress "Health check $service: ✓"
        else
            show_warning "Health check $service no disponible (servicio puede no estar ejecutándose)"
        fi
    done
}

# Mostrar resumen
show_summary() {
    echo ""
    echo -e "${BLUE}📊 Resumen de validación:${NC}"
    echo -e "   • Total de verificaciones: $TOTAL_CHECKS"
    echo -e "   • Exitosas: ${GREEN}$PASSED_CHECKS${NC}"
    echo -e "   • Advertencias: ${YELLOW}$WARNING_CHECKS${NC}"
    echo -e "   • Fallidas: ${RED}$FAILED_CHECKS${NC}"
    echo ""

    if [ $FAILED_CHECKS -eq 0 ]; then
        if [ $WARNING_CHECKS -eq 0 ]; then
            echo -e "${GREEN}🎉 ¡Toda la infraestructura está funcionando perfectamente!${NC}"
        else
            echo -e "${YELLOW}⚠️  La infraestructura está funcionando con algunas advertencias.${NC}"
        fi
    else
        echo -e "${RED}❌ Se encontraron problemas en la infraestructura.${NC}"
        if [ "$FIX_ISSUES" = false ]; then
            echo -e "${BLUE}💡 Ejecuta con --fix-issues para intentar corregir automáticamente.${NC}"
        fi
    fi

    echo ""
    echo -e "${BLUE}🔧 Para solucionar problemas manualmente:${NC}"
    echo -e "   • Reiniciar servicios: docker-compose -f docker-compose.simple.yml restart"
    echo -e "   • Ver logs: docker-compose -f docker-compose.simple.yml logs [servicio]"
    echo -e "   • Reconfigurar: ./setup-infrastructure.sh"
    echo ""
}

# Función principal
main() {
    check_environment_variables
    check_docker_services
    check_database_connectivity
    check_redis_connectivity
    check_rabbitmq_connectivity
    check_security_keys
    check_directories
    check_ports
    check_monitoring_config
    check_health_endpoints
    show_summary

    # Código de salida basado en resultados
    if [ $FAILED_CHECKS -gt 0 ]; then
        exit 1
    elif [ $WARNING_CHECKS -gt 0 ]; then
        exit 2
    else
        exit 0
    fi
}

# Ejecutar función principal
main "$@"