#!/bin/bash

# Script de despliegue para Gasolinera JSM
# Uso: ./deploy.sh [environment] [--rollback] [--dry-run] [--force]

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Variables
ENVIRONMENT=${1:-staging}
ROLLBACK=false
DRY_RUN=false
FORCE=false
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Procesar argumentos
for arg in "$@"; do
    case $arg in
        --rollback)
            ROLLBACK=true
            shift
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --force)
            FORCE=true
            shift
            ;;
    esac
done

echo -e "${BLUE}🚀 Iniciando despliegue de Gasolinera JSM${NC}"
echo -e "${BLUE}Ambiente: $ENVIRONMENT${NC}"
echo ""

# Funciones de utilidad
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

# Validar ambiente
validate_environment() {
    case $ENVIRONMENT in
        development|dev)
            ENVIRONMENT="development"
            ;;
        staging|stage)
            ENVIRONMENT="staging"
            ;;
        production|prod)
            ENVIRONMENT="production"
            ;;
        *)
            show_error "Ambiente inválido: $ENVIRONMENT"
            echo "Ambientes válidos: development, staging, production"
            exit 1
            ;;
    esac

    show_progress "Ambiente validado: $ENVIRONMENT"
}

# Verificar prerrequisitos
check_prerequisites() {
    show_info "Verificando prerrequisitos..."

    cd "$PROJECT_ROOT"

    # Verificar Docker
    if ! command -v docker &> /dev/null; then
        show_error "Docker no está instalado"
        exit 1
    fi

    # Verificar Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        show_error "Docker Compose no está instalado"
        exit 1
    fi

    # Verificar kubectl para ambientes remotos
    if [ "$ENVIRONMENT" != "development" ]; then
        if ! command -v kubectl &> /dev/null; then
            show_error "kubectl no está instalado (requerido para $ENVIRONMENT)"
            exit 1
        fi
    fi

    # Verificar configuración
    if [ ! -f "config/environments/.env.$ENVIRONMENT" ]; then
        show_error "Configuración no encontrada: config/environments/.env.$ENVIRONMENT"
        exit 1
    fi

    show_progress "Prerrequisitos verificados"
}

# Ejecutar tests pre-despliegue
run_pre_deployment_tests() {
    if [ "$FORCE" = true ]; then
        show_warning "Saltando tests pre-despliegue (--force)"
        return
    fi

    show_info "Ejecutando tests pre-despliegue..."

    cd "$PROJECT_ROOT"

    # Ejecutar tests de infraestructura
    if [ -f "scripts/run-infrastructure-tests.sh" ]; then
        chmod +x scripts/run-infrastructure-tests.sh
        ./scripts/run-infrastructure-tests.sh

        if [ $? -ne 0 ]; then
            show_error "Tests de infraestructura fallaron"
            exit 1
        fi
    fi

    # Ejecutar tests unitarios
    if [ -f "gradlew" ]; then
        ./gradlew test

        if [ $? -ne 0 ]; then
            show_error "Tests unitarios fallaron"
            exit 1
        fi
    fi

    show_progress "Tests pre-despliegue completados"
}

# Construir imágenes Docker
build_images() {
    show_info "Construyendo imágenes Docker..."

    cd "$PROJECT_ROOT"

    # Lista de servicios
    services=("auth-service" "station-service" "coupon-service" "raffle-service" "redemption-service" "api-gateway")

    for service in "${services[@]}"; do
        show_info "Construyendo $service..."

        if [ "$DRY_RUN" = true ]; then
            echo "[DRY RUN] docker build -t gasolinera-jsm/$service:$ENVIRONMENT services/$service/"
        else
            docker build -t "gasolinera-jsm/$service:$ENVIRONMENT" "services/$service/" || {
                show_error "Falló la construcción de $service"
                exit 1
            }
        fi
    done

    show_progress "Imágenes Docker construidas"
}

# Desplegar en desarrollo
deploy_development() {
    show_info "Desplegando en ambiente de desarrollo..."

    cd "$PROJECT_ROOT"

    # Configurar variables de entorno
    cp "config/environments/.env.$ENVIRONMENT" .env

    if [ "$DRY_RUN" = true ]; then
        echo "[DRY RUN] docker-compose -f docker-compose.simple.yml down"
        echo "[DRY RUN] docker-compose -f docker-compose.simple.yml up -d"
        return
    fi

    # Detener servicios existentes
    docker-compose -f docker-compose.simple.yml down --remove-orphans

    # Iniciar infraestructura
    docker-compose -f docker-compose.simple.yml up -d postgres redis rabbitmq

    # Esperar a que la infraestructura esté lista
    show_info "Esperando a que la infraestructura esté lista..."
    sleep 30

    # Ejecutar migraciones
    show_info "Ejecutando migraciones de base de datos..."
    docker-compose -f docker-compose.simple.yml exec -T postgres psql -U gasolinera_user -d gasolinera_db -f /docker-entrypoint-initdb.d/init-db.sql || true

    # Iniciar servicios de aplicación
    show_info "Iniciando servicios de aplicación..."
    # En desarrollo, los servicios se ejecutan directamente con Gradle

    show_progress "Despliegue en desarrollo completado"
}

# Desplegar en staging/production
deploy_remote() {
    show_info "Desplegando en ambiente remoto: $ENVIRONMENT..."

    cd "$PROJECT_ROOT"

    # Verificar conexión a Kubernetes
    if [ "$DRY_RUN" = false ]; then
        kubectl cluster-info &> /dev/null || {
            show_error "No se puede conectar al cluster de Kubernetes"
            exit 1
        }
    fi

    # Crear namespace si no existe
    if [ "$DRY_RUN" = true ]; then
        echo "[DRY RUN] kubectl create namespace gasolinera-$ENVIRONMENT --dry-run=client -o yaml | kubectl apply -f -"
    else
        kubectl create namespace "gasolinera-$ENVIRONMENT" --dry-run=client -o yaml | kubectl apply -f -
    fi

    # Aplicar configuraciones
    show_info "Aplicando configuraciones de Kubernetes..."

    if [ "$DRY_RUN" = true ]; then
        echo "[DRY RUN] kubectl apply -f k8s/$ENVIRONMENT/"
    else
        kubectl apply -f "k8s/$ENVIRONMENT/"
    fi

    # Esperar a que los deployments estén listos
    if [ "$DRY_RUN" = false ]; then
        show_info "Esperando a que los deployments estén listos..."

        services=("auth-service" "station-service" "coupon-service" "raffle-service" "redemption-service" "api-gateway")

        for service in "${services[@]}"; do
            kubectl rollout status "deployment/$service" -n "gasolinera-$ENVIRONMENT" --timeout=300s || {
                show_error "Timeout esperando el despliegue de $service"
                exit 1
            }
        done
    fi

    show_progress "Despliegue remoto completado"
}

# Ejecutar rollback
perform_rollback() {
    show_info "Ejecutando rollback en $ENVIRONMENT..."

    if [ "$ENVIRONMENT" = "development" ]; then
        show_info "Rollback en desarrollo: restaurando servicios anteriores..."

        if [ "$DRY_RUN" = true ]; then
            echo "[DRY RUN] docker-compose -f docker-compose.simple.yml down"
            echo "[DRY RUN] docker-compose -f docker-compose.simple.yml up -d"
        else
            docker-compose -f docker-compose.simple.yml restart
        fi
    else
        show_info "Rollback en $ENVIRONMENT: revirtiendo deployments..."

        services=("auth-service" "station-service" "coupon-service" "raffle-service" "redemption-service" "api-gateway")

        for service in "${services[@]}"; do
            if [ "$DRY_RUN" = true ]; then
                echo "[DRY RUN] kubectl rollout undo deployment/$service -n gasolinera-$ENVIRONMENT"
            else
                kubectl rollout undo "deployment/$service" -n "gasolinera-$ENVIRONMENT"
                kubectl rollout status "deployment/$service" -n "gasolinera-$ENVIRONMENT" --timeout=300s
            fi
        done
    fi

    show_progress "Rollback completado"
}

# Ejecutar tests post-despliegue
run_post_deployment_tests() {
    show_info "Ejecutando tests post-despliegue..."

    # Esperar a que los servicios estén completamente listos
    sleep 60

    # Determinar URL base según el ambiente
    case $ENVIRONMENT in
        development)
            BASE_URL="http://localhost:8080"
            ;;
        staging)
            BASE_URL="https://staging-api.gasolinerajsm.com"
            ;;
        production)
            BASE_URL="https://api.gasolinerajsm.com"
            ;;
    esac

    # Tests básicos de salud
    show_info "Verificando health checks..."

    if [ "$DRY_RUN" = true ]; then
        echo "[DRY RUN] curl -f $BASE_URL/health"
        echo "[DRY RUN] curl -f $BASE_URL/actuator/health"
    else
        curl -f "$BASE_URL/health" || {
            show_error "Health check falló"
            exit 1
        }

        curl -f "$BASE_URL/actuator/health" || {
            show_error "Actuator health check falló"
            exit 1
        }
    fi

    # Tests de endpoints críticos
    show_info "Verificando endpoints críticos..."

    critical_endpoints=("/api/auth/health" "/api/stations/health" "/api/coupons/health" "/api/raffles/health")

    for endpoint in "${critical_endpoints[@]}"; do
        if [ "$DRY_RUN" = true ]; then
            echo "[DRY RUN] curl -f $BASE_URL$endpoint"
        else
            curl -f "$BASE_URL$endpoint" || {
                show_warning "Endpoint crítico falló: $endpoint"
            }
        fi
    done

    show_progress "Tests post-despliegue completados"
}

# Generar reporte de despliegue
generate_deployment_report() {
    show_info "Generando reporte de despliegue..."

    local report_file="deployment-report-$(date +%Y%m%d-%H%M%S).txt"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')

    cat > "$report_file" << EOF
REPORTE DE DESPLIEGUE - GASOLINERA JSM
=====================================
Fecha: $timestamp
Ambiente: $ENVIRONMENT
Tipo: $([ "$ROLLBACK" = true ] && echo "Rollback" || echo "Despliegue")
Modo: $([ "$DRY_RUN" = true ] && echo "Dry Run" || echo "Ejecución Real")

SERVICIOS DESPLEGADOS:
- auth-service
- station-service
- coupon-service
- raffle-service
- redemption-service
- api-gateway

ESTADO:
- Pre-deployment tests: $([ "$FORCE" = true ] && echo "Omitidos" || echo "Ejecutados")
- Build: Completado
- Deployment: Completado
- Post-deployment tests: Completado

PRÓXIMOS PASOS:
- Monitorear métricas de aplicación
- Verificar logs de errores
- Confirmar funcionalidad de negocio
- Revisar alertas de monitoreo

URLS DE ACCESO:
EOF

    case $ENVIRONMENT in
        development)
            echo "- API Gateway: http://localhost:8080" >> "$report_file"
            echo "- Health Check: http://localhost:8080/health" >> "$report_file"
            ;;
        staging)
            echo "- API Gateway: https://staging-api.gasolinerajsm.com" >> "$report_file"
            echo "- Health Check: https://staging-api.gasolinerajsm.com/health" >> "$report_file"
            ;;
        production)
            echo "- API Gateway: https://api.gasolinerajsm.com" >> "$report_file"
            echo "- Health Check: https://api.gasolinerajsm.com/health" >> "$report_file"
            ;;
    esac

    echo "" >> "$report_file"
    echo "Reporte generado por: $(whoami)" >> "$report_file"
    echo "Commit: $(git rev-parse HEAD 2>/dev/null || echo 'N/A')" >> "$report_file"

    show_progress "Reporte generado: $report_file"
}

# Función principal
main() {
    validate_environment
    check_prerequisites

    if [ "$ROLLBACK" = true ]; then
        perform_rollback
    else
        run_pre_deployment_tests
        build_images

        case $ENVIRONMENT in
            development)
                deploy_development
                ;;
            staging|production)
                deploy_remote
                ;;
        esac

        run_post_deployment_tests
    fi

    generate_deployment_report

    echo ""
    echo -e "${GREEN}🎉 Despliegue completado exitosamente${NC}"
    echo ""
    echo -e "${BLUE}📋 Próximos pasos:${NC}"
    echo -e "   • Monitorear métricas: Grafana dashboard"
    echo -e "   • Verificar logs: kubectl logs -n gasolinera-$ENVIRONMENT"
    echo -e "   • Confirmar funcionalidad: Tests de aceptación"
    echo ""
}

# Manejo de errores
trap 'echo -e "${RED}❌ Error durante el despliegue. Revisa los logs arriba.${NC}"; exit 1' ERR

# Ejecutar función principal
main "$@"