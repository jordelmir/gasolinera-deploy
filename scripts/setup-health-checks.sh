#!/bin/bash

# Script para configurar y monitorear health checks de Gasolinera JSM

set -e

echo "🏥 Configurando Health Checks - Gasolinera JSM"
echo "=============================================="

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Función para logging
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Variables de configuración
NAMESPACE=${NAMESPACE:-gasolinera-jsm}
SERVICES=("auth-service" "coupon-service" "station-service" "redemption-service" "raffle-service" "ad-engine")
HEALTH_CHECK_TIMEOUT=${HEALTH_CHECK_TIMEOUT:-30}

# Verificar dependencias
check_dependencies() {
    log_info "Verificando dependencias..."

    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl no está instalado. Por favor instala kubectl primero."
        exit 1
    fi

    if ! command -v curl &> /dev/null; then
        log_error "curl no está instalado. Por favor instala curl primero."
        exit 1
    fi

    if ! command -v jq &> /dev/null; then
        log_warning "jq no está instalado. Algunas funciones pueden no funcionar correctamente."
    fi

    log_success "Dependencias verificadas correctamente"
}

# Verificar conectividad con cluster de Kubernetes
check_k8s_connectivity() {
    log_info "Verificando conectividad con Kubernetes..."

    if ! kubectl cluster-info &> /dev/null; then
        log_error "No se puede conectar al cluster de Kubernetes"
        exit 1
    fi

    # Verificar si el namespace existe
    if ! kubectl get namespace "$NAMESPACE" &> /dev/null; then
        log_warning "Namespace $NAMESPACE no existe. Creándolo..."
        kubectl create namespace "$NAMESPACE"
    fi

    log_success "Conectividad con Kubernetes verificada"
}

# Desplegar configuraciones de health checks
deploy_health_configs() {
    log_info "Desplegando configuraciones de health checks..."

    # Aplicar ConfigMaps
    kubectl apply -f infrastructure/kubernetes/health-checks/health-dashboard-configmap.yml -n "$NAMESPACE"

    log_success "Configuraciones de health checks desplegadas"
}

# Verificar health checks de servicios locales
check_local_services() {
    log_info "Verificando health checks de servicios locales..."

    local services_ports=("8080" "8081" "8082" "8083" "8084" "8085")
    local healthy_services=0
    local total_services=${#services_ports[@]}

    for i in "${!SERVICES[@]}"; do
        local service="${SERVICES[$i]}"
        local port="${services_ports[$i]}"

        log_info "Verificando $service en puerto $port..."

        if curl -s --max-time "$HEALTH_CHECK_TIMEOUT" "http://localhost:$port/actuator/health" > /dev/null; then
            log_success "$service está saludable"
            ((healthy_services++))
        else
            log_warning "$service no está disponible o no está saludable"
        fi
    done

    log_info "Servicios saludables: $healthy_services/$total_services"

    if [ "$healthy_services" -eq "$total_services" ]; then
        log_success "Todos los servicios locales están saludables"
    else
        log_warning "Algunos servicios locales no están saludables"
    fi
}

# Verificar health checks en Kubernetes
check_k8s_services() {
    log_info "Verificando health checks de servicios en Kubernetes..."

    local healthy_services=0
    local total_services=${#SERVICES[@]}

    for service in "${SERVICES[@]}"; do
        log_info "Verificando $service en Kubernetes..."

        # Verificar si el deployment existe
        if kubectl get deployment "$service" -n "$NAMESPACE" &> /dev/null; then
            # Verificar readiness
            local ready_replicas=$(kubectl get deployment "$service" -n "$NAMESPACE" -o jsonpath='{.status.readyReplicas}' 2>/dev/null || echo "0")
            local desired_replicas=$(kubectl get deployment "$service" -n "$NAMESPACE" -o jsonpath='{.spec.replicas}' 2>/dev/null || echo "0")

            if [ "$ready_replicas" = "$desired_replicas" ] && [ "$ready_replicas" != "0" ]; then
                log_success "$service está saludable ($ready_replicas/$desired_replicas replicas ready)"
                ((healthy_services++))
            else
                log_warning "$service no está completamente saludable ($ready_replicas/$desired_replicas replicas ready)"
            fi
        else
            log_warning "$service no está desplegado en Kubernetes"
        fi
    done

    log_info "Servicios saludables en K8s: $healthy_services/$total_services"
}

# Obtener métricas detalladas de health checks
get_detailed_health_metrics() {
    log_info "Obteniendo métricas detalladas de health checks..."

    local service_url=${1:-"http://localhost:8080"}

    echo "=== Health Check Detallado ==="

    # Health check general
    echo "1. Health Check General:"
    if command -v jq &> /dev/null; then
        curl -s "$service_url/api/health/detailed" | jq '.' 2>/dev/null || curl -s "$service_url/api/health/detailed"
    else
        curl -s "$service_url/api/health/detailed"
    fi
    echo ""

    # Health check rápido
    echo "2. Health Check Rápido:"
    curl -s "$service_url/api/health/quick"
    echo -e "\n"

    # Métricas de base de datos
    echo "3. Métricas de Base de Datos:"
    if command -v jq &> /dev/null; then
        curl -s "$service_url/api/health/database/detailed" | jq '.' 2>/dev/null || curl -s "$service_url/api/health/database/detailed"
    else
        curl -s "$service_url/api/health/database/detailed"
    fi
    echo ""

    # Métricas de Redis
    echo "4. Métricas de Redis:"
    if command -v jq &> /dev/null; then
        curl -s "$service_url/api/health/redis/detailed" | jq '.' 2>/dev/null || curl -s "$service_url/api/health/redis/detailed"
    else
        curl -s "$service_url/api/health/redis/detailed"
    fi
    echo ""

    # Métricas del sistema
    echo "5. Métricas del Sistema:"
    if command -v jq &> /dev/null; then
        curl -s "$service_url/api/health/system/detailed" | jq '.cpuUsagePercentage, .memoryUsagePercentage, .diskUsagePercentage' 2>/dev/null || curl -s "$service_url/api/health/system/detailed"
    else
        curl -s "$service_url/api/health/system/detailed"
    fi
    echo ""
}

# Monitorear health checks en tiempo real
monitor_health_checks() {
    log_info "Iniciando monitoreo de health checks en tiempo real..."
    log_info "Presiona Ctrl+C para detener el monitoreo"

    local service_url=${1:-"http://localhost:8080"}
    local interval=${2:-10}

    while true; do
        clear
        echo "=== Monitoreo de Health Checks - $(date) ==="
        echo ""

        # Status general
        echo "📊 Estado General:"
        if command -v jq &> /dev/null; then
            local status=$(curl -s "$service_url/api/health/quick" | jq -r '.status' 2>/dev/null || echo "UNKNOWN")
            if [ "$status" = "UP" ]; then
                echo -e "   ${GREEN}✅ Sistema Saludable${NC}"
            else
                echo -e "   ${RED}❌ Sistema con Problemas${NC}"
            fi
        else
            curl -s "$service_url/api/health/quick"
        fi
        echo ""

        # Componentes críticos
        echo "🔧 Componentes Críticos:"
        if command -v jq &> /dev/null; then
            local db_status=$(curl -s "$service_url/api/health/component/database" | jq -r '.status' 2>/dev/null || echo "UNKNOWN")
            local redis_status=$(curl -s "$service_url/api/health/component/redis" | jq -r '.status' 2>/dev/null || echo "UNKNOWN")
            local business_status=$(curl -s "$service_url/api/health/component/business" | jq -r '.status' 2>/dev/null || echo "UNKNOWN")

            echo "   Database: $([ "$db_status" = "UP" ] && echo -e "${GREEN}✅${NC}" || echo -e "${RED}❌${NC}") $db_status"
            echo "   Redis: $([ "$redis_status" = "UP" ] && echo -e "${GREEN}✅${NC}" || echo -e "${RED}❌${NC}") $redis_status"
            echo "   Business: $([ "$business_status" = "UP" ] && echo -e "${GREEN}✅${NC}" || echo -e "${RED}❌${NC}") $business_status"
        fi
        echo ""

        # Alertas activas
        echo "🚨 Alertas Activas:"
        if command -v jq &> /dev/null; then
            local alerts_count=$(curl -s "$service_url/api/health/alerts" | jq '. | length' 2>/dev/null || echo "0")
            if [ "$alerts_count" = "0" ]; then
                echo -e "   ${GREEN}✅ No hay alertas activas${NC}"
            else
                echo -e "   ${RED}⚠️  $alerts_count alertas activas${NC}"
                curl -s "$service_url/api/health/alerts" | jq -r '.[] | "   - \(.componentName): \(.message)"' 2>/dev/null || echo "   Error obteniendo alertas"
            fi
        fi
        echo ""

        echo "Próxima actualización en $interval segundos..."
        sleep "$interval"
    done
}

# Generar reporte de health checks
generate_health_report() {
    log_info "Generando reporte de health checks..."

    local service_url=${1:-"http://localhost:8080"}
    local period_hours=${2:-24}
    local output_file="health-report-$(date +%Y%m%d-%H%M%S).json"

    curl -s "$service_url/api/health/report?periodHours=$period_hours" > "$output_file"

    if [ -f "$output_file" ]; then
        log_success "Reporte generado: $output_file"

        if command -v jq &> /dev/null; then
            echo ""
            echo "=== Resumen del Reporte ==="
            jq -r '
                "Período: \(.reportPeriod)",
                "Disponibilidad General: \(.overallAvailability | round)%",
                "Disponibilidad Componentes Críticos: \(.criticalComponentsAvailability | round)%",
                "Total Health Checks: \(.totalHealthChecks)",
                "Total Fallos: \(.totalFailures)",
                "Alertas Activas: \(.activeAlerts | length)"
            ' "$output_file"
        fi
    else
        log_error "Error generando el reporte"
    fi
}

# Configurar alertas de health checks
setup_health_alerts() {
    log_info "Configurando alertas de health checks..."

    local service_url=${1:-"http://localhost:8080"}

    # Configurar thresholds para componentes críticos
    local components=("database" "redis" "business" "externalServices")

    for component in "${components[@]}"; do
        log_info "Configurando alertas para $component..."

        curl -s -X POST "$service_url/api/health/alerts/threshold/$component" \
            -H "Content-Type: application/json" \
            -d '{
                "maxConsecutiveFailures": 3,
                "maxFailureRate": 0.1,
                "timeWindowMinutes": 5
            }' > /dev/null

        if [ $? -eq 0 ]; then
            log_success "Alertas configuradas para $component"
        else
            log_warning "Error configurando alertas para $component"
        fi
    done
}

# Función principal
main() {
    case "${1:-setup}" in
        "setup")
            check_dependencies
            check_k8s_connectivity
            deploy_health_configs
            setup_health_alerts
            log_success "Health checks configurados exitosamente"
            ;;
        "check-local")
            check_local_services
            ;;
        "check-k8s")
            check_k8s_connectivity
            check_k8s_services
            ;;
        "detailed")
            get_detailed_health_metrics "${2:-http://localhost:8080}"
            ;;
        "monitor")
            monitor_health_checks "${2:-http://localhost:8080}" "${3:-10}"
            ;;
        "report")
            generate_health_report "${2:-http://localhost:8080}" "${3:-24}"
            ;;
        "alerts")
            setup_health_alerts "${2:-http://localhost:8080}"
            ;;
        "help")
            echo "Uso: $0 [comando] [opciones]"
            echo ""
            echo "Comandos disponibles:"
            echo "  setup              - Configuración completa inicial (por defecto)"
            echo "  check-local        - Verificar health checks de servicios locales"
            echo "  check-k8s          - Verificar health checks en Kubernetes"
            echo "  detailed [url]     - Obtener métricas detalladas"
            echo "  monitor [url] [s]  - Monitorear health checks en tiempo real"
            echo "  report [url] [h]   - Generar reporte de health checks"
            echo "  alerts [url]       - Configurar alertas de health checks"
            echo "  help               - Mostrar esta ayuda"
            echo ""
            echo "Ejemplos:"
            echo "  $0 setup"
            echo "  $0 check-local"
            echo "  $0 detailed http://localhost:8080"
            echo "  $0 monitor http://localhost:8080 5"
            echo "  $0 report http://localhost:8080 48"
            ;;
        *)
            log_error "Comando desconocido: $1"
            echo "Usa '$0 help' para ver los comandos disponibles"
            exit 1
            ;;
    esac
}

# Ejecutar función principal
main "$@"