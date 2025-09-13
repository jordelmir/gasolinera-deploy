#!/bin/bash

# Script para optimizar la base de datos de Gasolinera JSM

set -e

echo "🗄️  Optimizando Base de Datos - Gasolinera JSM"
echo "=============================================="

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Variables de configuración
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-gasolinera}
DB_USER=${DB_USER:-gasolinera_user}
DB_PASSWORD=${DB_PASSWORD:-gasolinera_password}
SERVICE_URL=${SERVICE_URL:-http://localhost:8080}

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

# Verificar dependencias
check_dependencies() {
    log_info "Verificando dependencias..."

    if ! command -v psql &> /dev/null; then
        log_error "psql no está instalado. Por favor instala PostgreSQL client."
        exit 1
    fi

    if ! command -v curl &> /dev/null; then
        log_error "curl no está instalado."
        exit 1
    fi

    if ! command -v jq &> /dev/null; then
        log_warning "jq no está instalado. Algunas funciones pueden no funcionar correctamente."
    fi

    log_success "Dependencias verificadas"
}

# Verificar conectividad con la base de datos
check_db_connectivity() {
    log_info "Verificando conectividad con PostgreSQL..."

    if ! PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "SELECT 1;" &> /dev/null; then
        log_error "No se puede conectar a la base de datos PostgreSQL"
        log_error "Host: $DB_HOST, Port: $DB_PORT, Database: $DB_NAME, User: $DB_USER"
        exit 1
    fi

    log_success "Conectividad con PostgreSQL verificada"
}

# Ejecutar migraciones de optimización
run_optimization_migrations() {
    log_info "Ejecutando migraciones de optimización..."

    # Ejecutar migración de índices
    log_info "Aplicando índices optimizados..."
    if PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f database/migrations/V001__create_optimized_indexes.sql; then
        log_success "Índices optimizados aplicados"
    else
        log_error "Error aplicando índices optimizados"
        return 1
    fi

    # Ejecutar migración de particionado
    log_info "Aplicando configuración de particionado..."
    if PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f database/migrations/V002__create_partitioned_tables.sql; then
        log_success "Configuración de particionado aplicada"
    else
        log_warning "Error aplicando configuración de particionado (puede ser normal si las tablas no existen)"
    fi
}

# Analizar rendimiento de la base de datos
analyze_performance() {
    log_info "Analizando rendimiento de la base de datos..."

    if command -v jq &> /dev/null; then
        echo "=== Análisis de Rendimiento ==="

        # Análisis general
        log_info "Obteniendo análisis de rendimiento..."
        curl -s "$SERVICE_URL/api/database/analysis/performance" | jq '
            {
                "slow_queries_count": (.slowQueries | length),
                "tables_analyzed": (.tableStats | length),
                "indexes_analyzed": (.indexUsage | length),
                "active_connections": .connectionStats.activeConnections,
                "connection_usage_percent": .connectionStats.connectionUsagePercent,
                "active_locks": (.lockAnalysis | length)
            }
        ' 2>/dev/null || log_warning "No se pudo obtener análisis de rendimiento"

        # Queries lentas
        log_info "Top 5 queries más lentas:"
        curl -s "$SERVICE_URL/api/database/queries/slow?limit=5" | jq -r '
            .[] | "- Query: \(.query | .[0:80])... | Tiempo promedio: \(.meanTimeMs)ms | Llamadas: \(.calls)"
        ' 2>/dev/null || log_warning "No se pudieron obtener queries lentas"

        # Uso de disco
        log_info "Top 5 tablas por uso de disco:"
        curl -s "$SERVICE_URL/api/database/tables/disk-usage" | jq -r '
            .[:5] | .[] | "- \(.tableName): \(.totalSize) (\(.totalSizeBytes) bytes)"
        ' 2>/dev/null || log_warning "No se pudo obtener uso de disco"

    else
        log_warning "jq no disponible - mostrando datos sin formato"
        curl -s "$SERVICE_URL/api/database/analysis/performance" 2>/dev/null || log_warning "No se pudo conectar al servicio"
    fi
}

# Analizar índices
analyze_indexes() {
    log_info "Analizando índices..."

    if command -v jq &> /dev/null; then
        echo "=== Análisis de Índices ==="

        # Índices no utilizados
        log_info "Índices no utilizados:"
        curl -s "$SERVICE_URL/api/database/indexes/unused" | jq -r '
            .[] | "- \(.indexName) en \(.tableName): \(.indexSize) (scans: \(.scans))"
        ' 2>/dev/null || log_warning "No se pudieron obtener índices no utilizados"

        # Índices duplicados
        log_info "Índices duplicados:"
        curl -s "$SERVICE_URL/api/database/indexes/duplicates" | jq -r '
            .[] | "- \(.tableName): \(.index1) y \(.index2) en columnas [\(.columns | join(", "))]"
        ' 2>/dev/null || log_warning "No se pudieron obtener índices duplicados"

        # Índices faltantes sugeridos
        log_info "Índices faltantes sugeridos:"
        curl -s "$SERVICE_URL/api/database/indexes/missing" | jq -r '
            .[:5] | .[] | "- \(.tableName)(\(.columns | join(", "))): \(.reason)"
        ' 2>/dev/null || log_warning "No se pudieron obtener sugerencias de índices"

    else
        curl -s "$SERVICE_URL/api/database/analysis/indexes" 2>/dev/null || log_warning "No se pudo conectar al servicio"
    fi
}

# Analizar particionado
analyze_partitioning() {
    log_info "Analizando particionado..."

    if command -v jq &> /dev/null; then
        echo "=== Análisis de Particionado ==="

        # Candidatos para particionado
        log_info "Candidatos para particionado:"
        curl -s "$SERVICE_URL/api/database/partitioning/candidates" | jq -r '
            .[] | "- \(.tableName): \(.rowCount) filas, \(.sizeBytes) bytes - \(.reason)"
        ' 2>/dev/null || log_warning "No se pudieron obtener candidatos de particionado"

        # Particiones existentes
        log_info "Particiones existentes:"
        curl -s "$SERVICE_URL/api/database/analysis/partitioning" | jq -r '
            .existingPartitions[] | "- \(.tableName): \(.size) (\(.rowCount) filas)"
        ' 2>/dev/null || log_warning "No se pudieron obtener particiones existentes"

    else
        curl -s "$SERVICE_URL/api/database/analysis/partitioning" 2>/dev/null || log_warning "No se pudo conectar al servicio"
    fi
}

# Ejecutar mantenimiento
run_maintenance() {
    log_info "Ejecutando mantenimiento de base de datos..."

    case "${1:-full}" in
        "full")
            log_info "Ejecutando mantenimiento completo..."
            if command -v jq &> /dev/null; then
                curl -s -X POST "$SERVICE_URL/api/database/maintenance/full" | jq '
                    {
                        "success": .success,
                        "duration_seconds": .duration,
                        "recommendations_count": (.recommendations | length)
                    }
                ' 2>/dev/null || log_warning "Error en mantenimiento completo"
            else
                curl -s -X POST "$SERVICE_URL/api/database/maintenance/full" || log_warning "Error en mantenimiento completo"
            fi
            ;;
        "indexes")
            log_info "Ejecutando mantenimiento de índices..."
            curl -s -X POST "$SERVICE_URL/api/database/maintenance/indexes" | jq '.' 2>/dev/null || log_warning "Error en mantenimiento de índices"
            ;;
        "statistics")
            log_info "Actualizando estadísticas..."
            curl -s -X POST "$SERVICE_URL/api/database/maintenance/statistics" | jq '.' 2>/dev/null || log_warning "Error actualizando estadísticas"
            ;;
        "vacuum")
            log_info "Ejecutando vacuum..."
            curl -s -X POST "$SERVICE_URL/api/database/maintenance/vacuum" | jq '.' 2>/dev/null || log_warning "Error ejecutando vacuum"
            ;;
    esac
}

# Generar reporte de optimización
generate_optimization_report() {
    log_info "Generando reporte de optimización..."

    local output_file="database-optimization-report-$(date +%Y%m%d-%H%M%S).json"

    if curl -s "$SERVICE_URL/api/database/report/optimization" > "$output_file"; then
        log_success "Reporte generado: $output_file"

        if command -v jq &> /dev/null; then
            echo ""
            echo "=== Resumen del Reporte ==="
            jq -r '
                "Score General: \(.overallScore)/100",
                "Queries Lentas: \(.performanceAnalysis.slowQueries | length)",
                "Índices No Utilizados: \(.indexOptimization.unusedIndexes | length)",
                "Índices Faltantes: \(.indexOptimization.missingIndexes | length)",
                "Candidatos Particionado: \(.partitioning.partitionCandidates | length)",
                "Recomendaciones Prioritarias: \(.priorityRecommendations | length)"
            ' "$output_file"

            echo ""
            echo "=== Top 3 Recomendaciones Prioritarias ==="
            jq -r '.priorityRecommendations[:3] | .[] | "- [\(.priority)] \(.title): \(.description)"' "$output_file"
        fi
    else
        log_error "Error generando el reporte"
    fi
}

# Obtener métricas específicas de Gasolinera
get_gasolinera_metrics() {
    log_info "Obteniendo métricas específicas de Gasolinera JSM..."

    if command -v jq &> /dev/null; then
        echo "=== Métricas de Gasolinera JSM ==="
        curl -s "$SERVICE_URL/api/database/gasolinera/metrics" | jq '
            {
                "total_users": .totalUsers,
                "total_coupons": .totalCoupons,
                "total_redemptions": .totalRedemptions,
                "total_stations": .totalStations,
                "daily_redemptions": .dailyRedemptions,
                "avg_coupons_per_user": (.avgCouponsPerUser | round),
                "redemption_rate_percent": ((.redemptionRate * 100) | round),
                "coupons_table_size_mb": ((.couponsTableSize / 1024 / 1024) | round),
                "redemptions_table_size_mb": ((.redemptionsTableSize / 1024 / 1024) | round)
            }
        ' 2>/dev/null || log_warning "No se pudieron obtener métricas de Gasolinera"
    else
        curl -s "$SERVICE_URL/api/database/gasolinera/metrics" || log_warning "No se pudo conectar al servicio"
    fi
}

# Configurar PostgreSQL para optimización
configure_postgresql() {
    log_info "Configurando PostgreSQL para optimización..."

    # Obtener recomendaciones de configuración
    if command -v jq &> /dev/null; then
        echo "=== Recomendaciones de Configuración ==="
        curl -s "$SERVICE_URL/api/database/configuration/recommendations" | jq -r '
            .[] | "- \(.parameter): \(.currentValue) → \(.recommendedValue) (\(.reason))"
        ' 2>/dev/null || log_warning "No se pudieron obtener recomendaciones de configuración"
    fi

    # Verificar extensiones necesarias
    log_info "Verificando extensiones de PostgreSQL..."
    PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "
        SELECT
            name,
            installed_version,
            CASE WHEN installed_version IS NOT NULL THEN 'INSTALLED' ELSE 'NOT_INSTALLED' END as status
        FROM pg_available_extensions
        WHERE name IN ('pg_stat_statements', 'postgis', 'uuid-ossp', 'btree_gin', 'btree_gist')
        ORDER BY name;
    " 2>/dev/null || log_warning "No se pudo verificar extensiones"
}

# Monitorear optimizaciones en tiempo real
monitor_optimizations() {
    log_info "Iniciando monitoreo de optimizaciones..."
    log_info "Presiona Ctrl+C para detener el monitoreo"

    local interval=${1:-30}

    while true; do
        clear
        echo "=== Monitoreo de Optimizaciones DB - $(date) ==="
        echo ""

        # Estadísticas de conexiones
        echo "📊 Conexiones:"
        if command -v jq &> /dev/null; then
            curl -s "$SERVICE_URL/api/database/connections/statistics" | jq -r '
                "   Activas: \(.activeConnections)/\(.maxConnections) (\(.connectionUsagePercent)%)",
                "   Idle: \(.idleConnections)",
                "   Total: \(.totalConnections)"
            ' 2>/dev/null || echo "   Error obteniendo estadísticas de conexiones"
        fi
        echo ""

        # Queries lentas
        echo "🐌 Queries Lentas (Top 3):"
        if command -v jq &> /dev/null; then
            curl -s "$SERVICE_URL/api/database/queries/slow?limit=3" | jq -r '
                .[] | "   - \(.meanTimeMs | round)ms avg (\(.calls) calls): \(.query | .[0:60])..."
            ' 2>/dev/null || echo "   Error obteniendo queries lentas"
        fi
        echo ""

        # Locks activos
        echo "🔒 Locks Activos:"
        if command -v jq &> /dev/null; then
            local locks_count=$(curl -s "$SERVICE_URL/api/database/locks/active" | jq '. | length' 2>/dev/null || echo "0")
            if [ "$locks_count" = "0" ]; then
                echo -e "   ${GREEN}✅ No hay locks activos${NC}"
            else
                echo -e "   ${RED}⚠️  $locks_count locks activos${NC}"
                curl -s "$SERVICE_URL/api/database/locks/active" | jq -r '
                    .[:3] | .[] | "   - \(.relationName): \(.mode) (\(.lockType))"
                ' 2>/dev/null
            fi
        fi
        echo ""

        # Estado de mantenimiento
        echo "🔧 Estado de Mantenimiento:"
        if command -v jq &> /dev/null; then
            curl -s "$SERVICE_URL/api/database/maintenance/status" | jq -r '
                "   Habilitado: \(.isEnabled)",
                "   En ventana: \(.isInMaintenanceWindow)",
                "   Próximo: \(.nextScheduledMaintenance)"
            ' 2>/dev/null || echo "   Error obteniendo estado de mantenimiento"
        fi
        echo ""

        echo "Próxima actualización en $interval segundos..."
        sleep "$interval"
    done
}

# Crear índices recomendados
create_recommended_indexes() {
    log_info "Creando índices recomendados..."

    # Obtener recomendaciones de índices faltantes
    local missing_indexes=$(curl -s "$SERVICE_URL/api/database/indexes/missing" 2>/dev/null)

    if command -v jq &> /dev/null && [ -n "$missing_indexes" ]; then
        echo "$missing_indexes" | jq -r '.[:5] | .[] |
            {
                "tableName": .tableName,
                "indexName": ("idx_" + .tableName + "_" + (.columns | join("_"))),
                "columns": .columns,
                "indexType": .indexType,
                "reason": .reason,
                "priority": "HIGH",
                "estimatedImpact": .estimatedImpact
            }
        ' | while IFS= read -r recommendation; do
            log_info "Creando índice recomendado..."
            echo "$recommendation" | curl -s -X POST "$SERVICE_URL/api/database/indexes/create" \
                -H "Content-Type: application/json" \
                -d @- | jq '.' 2>/dev/null || log_warning "Error creando índice"
        done
    else
        log_warning "No se pudieron obtener recomendaciones de índices"
    fi
}

# Función principal
main() {
    case "${1:-analyze}" in
        "setup")
            check_dependencies
            check_db_connectivity
            run_optimization_migrations
            configure_postgresql
            log_success "Configuración de optimización completada"
            ;;
        "analyze")
            check_dependencies
            analyze_performance
            analyze_indexes
            analyze_partitioning
            ;;
        "performance")
            analyze_performance
            ;;
        "indexes")
            analyze_indexes
            ;;
        "partitioning")
            analyze_partitioning
            ;;
        "maintenance")
            run_maintenance "${2:-full}"
            ;;
        "monitor")
            monitor_optimizations "${2:-30}"
            ;;
        "report")
            generate_optimization_report
            ;;
        "metrics")
            get_gasolinera_metrics
            ;;
        "create-indexes")
            create_recommended_indexes
            ;;
        "configure")
            configure_postgresql
            ;;
        "help")
            echo "Uso: $0 [comando] [opciones]"
            echo ""
            echo "Comandos disponibles:"
            echo "  setup              - Configuración completa inicial"
            echo "  analyze            - Análisis completo de rendimiento (por defecto)"
            echo "  performance        - Análisis de rendimiento solamente"
            echo "  indexes            - Análisis de índices solamente"
            echo "  partitioning       - Análisis de particionado solamente"
            echo "  maintenance [tipo] - Ejecutar mantenimiento (full|indexes|statistics|vacuum)"
            echo "  monitor [segundos] - Monitorear optimizaciones en tiempo real"
            echo "  report             - Generar reporte completo de optimización"
            echo "  metrics            - Obtener métricas específicas de Gasolinera"
            echo "  create-indexes     - Crear índices recomendados automáticamente"
            echo "  configure          - Mostrar recomendaciones de configuración"
            echo "  help               - Mostrar esta ayuda"
            echo ""
            echo "Variables de entorno:"
            echo "  DB_HOST            - Host de PostgreSQL (default: localhost)"
            echo "  DB_PORT            - Puerto de PostgreSQL (default: 5432)"
            echo "  DB_NAME            - Nombre de la base de datos (default: gasolinera)"
            echo "  DB_USER            - Usuario de PostgreSQL (default: gasolinera_user)"
            echo "  DB_PASSWORD        - Password de PostgreSQL (default: gasolinera_password)"
            echo "  SERVICE_URL        - URL del servicio (default: http://localhost:8080)"
            echo ""
            echo "Ejemplos:"
            echo "  $0 setup"
            echo "  $0 analyze"
            echo "  DB_HOST=prod-db $0 performance"
            echo "  $0 maintenance indexes"
            echo "  $0 monitor 60"
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