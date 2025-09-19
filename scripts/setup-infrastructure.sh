#!/bin/bash

# Script maestro para configurar toda la infraestructura de Gasolinera JSM
# Uso: ./setup-infrastructure.sh [environment] [--skip-docker] [--skip-db] [--skip-keys]

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Variables
ENVIRONMENT=${1:-development}
SKIP_DOCKER=false
SKIP_DB=false
SKIP_KEYS=false
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Procesar argumentos
for arg in "$@"; do
    case $arg in
        --skip-docker)
            SKIP_DOCKER=true
            shift
            ;;
        --skip-db)
            SKIP_DB=true
            shift
            ;;
        --skip-keys)
            SKIP_KEYS=true
            shift
            ;;
    esac
done

echo -e "${BLUE}🚀 Configurando infraestructura de Gasolinera JSM${NC}"
echo -e "${BLUE}Ambiente: $ENVIRONMENT${NC}"
echo ""

# Función para mostrar progreso
show_progress() {
    echo -e "${GREEN}✅ $1${NC}"
}

show_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

show_error() {
    echo -e "${RED}❌ $1${NC}"
}

show_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# Verificar prerrequisitos
check_prerequisites() {
    show_info "Verificando prerrequisitos..."

    # Verificar Docker
    if ! command -v docker &> /dev/null; then
        show_error "Docker no está instalado. Por favor instala Docker primero."
        exit 1
    fi

    # Verificar Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        show_error "Docker Compose no está instalado. Por favor instala Docker Compose primero."
        exit 1
    fi

    # Verificar Java (para Kotlin)
    if ! command -v java &> /dev/null; then
        show_warning "Java no está instalado. Algunos scripts pueden no funcionar."
    fi

    # Verificar Node.js (para scripts de verificación)
    if ! command -v node &> /dev/null; then
        show_warning "Node.js no está instalado. Scripts de verificación QR no funcionarán."
    fi

    show_progress "Prerrequisitos verificados"
}

# Configurar variables de entorno
setup_environment() {
    show_info "Configurando variables de entorno para $ENVIRONMENT..."

    cd "$PROJECT_ROOT"

    if [ -f "scripts/setup-environment.sh" ]; then
        chmod +x scripts/setup-environment.sh
        ./scripts/setup-environment.sh "$ENVIRONMENT"
        show_progress "Variables de entorno configuradas"
    else
        show_error "Script setup-environment.sh no encontrado"
        exit 1
    fi
}

# Generar claves de seguridad
generate_security_keys() {
    if [ "$SKIP_KEYS" = true ]; then
        show_info "Saltando generación de claves de seguridad"
        return
    fi

    show_info "Generando claves de seguridad..."

    cd "$PROJECT_ROOT/ops/key-management"

    # Generar claves RSA para QR
    if [ ! -f "private-key.pem" ] || [ ! -f "public-key.pem" ]; then
        if [ -f "generate-keys-simple.kt" ]; then
            if command -v kotlinc &> /dev/null; then
                kotlinc generate-keys-simple.kt -include-runtime -d generate-keys-simple.jar
                java -jar generate-keys-simple.jar
                show_progress "Claves QR generadas"
            else
                show_warning "Kotlinc no disponible. Generando claves con OpenSSL..."
                openssl genrsa -out private-key.pem 2048
                openssl rsa -in private-key.pem -pubout -out public-key.pem
                show_progress "Claves QR generadas con OpenSSL"
            fi
        else
            show_warning "Generador de claves no encontrado. Usando OpenSSL..."
            openssl genrsa -out private-key.pem 2048
            openssl rsa -in private-key.pem -pubout -out public-key.pem
            show_progress "Claves QR generadas con OpenSSL"
        fi
    else
        show_info "Claves QR ya existen"
    fi

    # Configurar permisos seguros
    chmod 600 private-key.pem
    chmod 644 public-key.pem

    cd "$PROJECT_ROOT"
}

# Configurar Docker y servicios
setup_docker_services() {
    if [ "$SKIP_DOCKER" = true ]; then
        show_info "Saltando configuración de Docker"
        return
    fi

    show_info "Configurando servicios Docker..."

    cd "$PROJECT_ROOT"

    # Detener servicios existentes
    if [ -f "docker-compose.simple.yml" ]; then
        docker-compose -f docker-compose.simple.yml down --remove-orphans
    fi

    # Crear redes Docker si no existen
    docker network create gasolinera-network 2>/dev/null || true

    # Iniciar servicios de infraestructura
    docker-compose -f docker-compose.simple.yml up -d postgres redis rabbitmq

    show_progress "Servicios Docker iniciados"

    # Esperar a que los servicios estén listos
    show_info "Esperando a que los servicios estén listos..."
    sleep 30

    # Verificar que los servicios estén funcionando
    if docker-compose -f docker-compose.simple.yml ps | grep -q "Up"; then
        show_progress "Servicios Docker funcionando correctamente"
    else
        show_error "Algunos servicios Docker no están funcionando"
        docker-compose -f docker-compose.simple.yml ps
        exit 1
    fi
}

# Configurar base de datos
setup_database() {
    if [ "$SKIP_DB" = true ]; then
        show_info "Saltando configuración de base de datos"
        return
    fi

    show_info "Configurando base de datos..."

    cd "$PROJECT_ROOT"

    # Esperar a que PostgreSQL esté listo
    show_info "Esperando a que PostgreSQL esté listo..."
    timeout=60
    while ! docker-compose -f docker-compose.simple.yml exec -T postgres pg_isready -U gasolinera_user -d gasolinera_db; do
        sleep 2
        timeout=$((timeout - 2))
        if [ $timeout -le 0 ]; then
            show_error "Timeout esperando PostgreSQL"
            exit 1
        fi
    done

    # Ejecutar script de inicialización si existe
    if [ -f "scripts/init-db.sql" ]; then
        docker-compose -f docker-compose.simple.yml exec -T postgres psql -U gasolinera_user -d gasolinera_db -f /docker-entrypoint-initdb.d/init-db.sql
        show_progress "Script de inicialización de BD ejecutado"
    fi

    # Ejecutar migraciones Flyway si están disponibles
    if [ -d "database/migrations" ]; then
        show_info "Ejecutando migraciones de base de datos..."

        # Verificar si Flyway está disponible
        if command -v flyway &> /dev/null; then
            flyway -configFiles=database/flyway.conf migrate
            show_progress "Migraciones Flyway ejecutadas"
        else
            show_warning "Flyway no está disponible. Ejecutando migraciones manualmente..."

            # Ejecutar migraciones manualmente
            for migration in database/migrations/V*.sql; do
                if [ -f "$migration" ]; then
                    show_info "Ejecutando migración: $(basename "$migration")"
                    docker-compose -f docker-compose.simple.yml exec -T postgres psql -U gasolinera_user -d gasolinera_db < "$migration"
                fi
            done
            show_progress "Migraciones ejecutadas manualmente"
        fi
    fi
}

# Crear directorios necesarios
create_directories() {
    show_info "Creando directorios necesarios..."

    cd "$PROJECT_ROOT"

    # Directorios de logs
    mkdir -p logs/requests
    mkdir -p logs/responses
    mkdir -p logs/errors

    # Directorios de datos
    mkdir -p data/uploads
    mkdir -p data/exports
    mkdir -p data/backups

    # Directorios de monitoreo
    mkdir -p monitoring/prometheus/data
    mkdir -p monitoring/grafana/data

    # Configurar permisos
    chmod 755 logs logs/requests logs/responses logs/errors
    chmod 755 data data/uploads data/exports
    chmod 700 data/backups
    chmod 755 monitoring monitoring/prometheus monitoring/grafana

    show_progress "Directorios creados"
}

# Configurar monitoreo
setup_monitoring() {
    show_info "Configurando monitoreo..."

    cd "$PROJECT_ROOT"

    # Crear configuración de Prometheus si no existe
    if [ ! -f "monitoring/prometheus/prometheus.yml" ]; then
        show_warning "Configuración de Prometheus no encontrada"
    else
        show_progress "Configuración de Prometheus lista"
    fi

    # Verificar configuración de alertas
    if [ -f "monitoring/prometheus/alert_rules.yml" ]; then
        show_progress "Reglas de alertas configuradas"
    else
        show_warning "Reglas de alertas no encontradas"
    fi
}

# Validar configuración
validate_setup() {
    show_info "Validando configuración..."

    cd "$PROJECT_ROOT"

    # Verificar archivo .env
    if [ -f ".env" ]; then
        show_progress "Archivo .env encontrado"

        # Verificar variables críticas
        if grep -q "DB_HOST" .env && grep -q "JWT_SECRET" .env; then
            show_progress "Variables críticas configuradas"
        else
            show_warning "Algunas variables críticas pueden estar faltando"
        fi
    else
        show_error "Archivo .env no encontrado"
        exit 1
    fi

    # Verificar claves de seguridad
    if [ -f "ops/key-management/private-key.pem" ] && [ -f "ops/key-management/public-key.pem" ]; then
        show_progress "Claves de seguridad configuradas"
    else
        show_warning "Claves de seguridad no encontradas"
    fi

    # Verificar servicios Docker
    if [ "$SKIP_DOCKER" = false ]; then
        if docker-compose -f docker-compose.simple.yml ps | grep -q "Up"; then
            show_progress "Servicios Docker funcionando"
        else
            show_warning "Algunos servicios Docker pueden no estar funcionando"
        fi
    fi
}

# Mostrar resumen final
show_summary() {
    echo ""
    echo -e "${GREEN}🎉 Configuración de infraestructura completada${NC}"
    echo ""
    echo -e "${BLUE}📋 Resumen:${NC}"
    echo -e "   • Ambiente: $ENVIRONMENT"
    echo -e "   • Variables de entorno: ✅"
    echo -e "   • Claves de seguridad: $([ -f "ops/key-management/private-key.pem" ] && echo "✅" || echo "⚠️")"
    echo -e "   • Servicios Docker: $([ "$SKIP_DOCKER" = true ] && echo "⏭️" || echo "✅")"
    echo -e "   • Base de datos: $([ "$SKIP_DB" = true ] && echo "⏭️" || echo "✅")"
    echo -e "   • Directorios: ✅"
    echo -e "   • Monitoreo: ✅"
    echo ""
    echo -e "${BLUE}📊 Próximos pasos:${NC}"
    echo -e "   1. Verificar servicios: docker-compose -f docker-compose.simple.yml ps"
    echo -e "   2. Ver logs: docker-compose -f docker-compose.simple.yml logs"
    echo -e "   3. Iniciar aplicaciones: ./start-services.sh"
    echo -e "   4. Verificar health checks: curl http://localhost:8080/health"
    echo ""
    echo -e "${BLUE}🔗 URLs útiles:${NC}"
    echo -e "   • API Gateway: http://localhost:8080"
    echo -e "   • Prometheus: http://localhost:9090 (si está configurado)"
    echo -e "   • Grafana: http://localhost:3000 (si está configurado)"
    echo -e "   • RabbitMQ Management: http://localhost:15672"
    echo ""
}

# Función principal
main() {
    echo -e "${BLUE}Iniciando configuración de infraestructura...${NC}"
    echo ""

    check_prerequisites
    setup_environment
    generate_security_keys
    create_directories
    setup_docker_services
    setup_database
    setup_monitoring
    validate_setup
    show_summary

    echo -e "${GREEN}✨ ¡Configuración completada exitosamente!${NC}"
}

# Manejo de errores
trap 'echo -e "${RED}❌ Error durante la configuración. Revisa los logs arriba.${NC}"; exit 1' ERR

# Ejecutar función principal
main "$@"