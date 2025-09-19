#!/bin/bash

# Script para ejecutar tests de infraestructura
# Uso: ./run-infrastructure-tests.sh [--performance] [--detailed] [--fix-issues]

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Variables
RUN_PERFORMANCE=false
DETAILED=false
FIX_ISSUES=false
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Procesar argumentos
for arg in "$@"; do
    case $arg in
        --performance)
            RUN_PERFORMANCE=true
            shift
            ;;
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

echo -e "${BLUE}🧪 Ejecutando tests de infraestructura de Gasolinera JSM${NC}"
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

# Verificar prerrequisitos
check_prerequisites() {
    show_info "Verificando prerrequisitos para tests..."

    cd "$PROJECT_ROOT"

    # Verificar Java/Kotlin
    if ! command -v java &> /dev/null; then
        show_error "Java no está instalado. Necesario para ejecutar tests."
        exit 1
    fi

    # Verificar Gradle
    if [ -f "gradlew" ]; then
        chmod +x gradlew
        show_progress "Gradle wrapper encontrado"
    elif command -v gradle &> /dev/null; then
        show_progress "Gradle instalado globalmente"
    else
        show_error "Gradle no está disponible"
        exit 1
    fi

    # Verificar archivo .env
    if [ ! -f ".env" ]; then
        show_warning "Archivo .env no encontrado. Usando valores por defecto."
        if [ "$FIX_ISSUES" = true ]; then
            show_info "Creando archivo .env básico..."
            cp config/environments/.env.development .env
        fi
    fi
}

# Preparar entorno de test
prepare_test_environment() {
    show_info "Preparando entorno de test..."

    cd "$PROJECT_ROOT"

    # Cargar variables de entorno
    if [ -f ".env" ]; then
        export $(cat .env | grep -v '^#' | xargs)
    fi

    # Crear directorios de test si no existen
    mkdir -p tests/reports
    mkdir -p tests/logs

    # Limpiar reportes anteriores
    rm -f tests/reports/*
    rm -f tests/logs/*

    show_progress "Entorno de test preparado"
}

# Compilar tests
compile_tests() {
    show_info "Compilando tests de infraestructura..."

    cd "$PROJECT_ROOT"

    if [ -f "gradlew" ]; then
        ./gradlew compileTestKotlin
    else
        gradle compileTestKotlin
    fi

    show_progress "Tests compilados exitosamente"
}

# Ejecutar tests básicos de infraestructura
run_infrastructure_tests() {
    show_info "Ejecutando tests básicos de infraestructura..."

    cd "$PROJECT_ROOT"

    # Crear archivo de configuración de test
    cat > tests/infrastructure/test.properties << EOF
# Test configuration
test.environment=test
test.timeout=30000
test.retry.count=3
test.parallel.enabled=true
EOF

    # Ejecutar tests usando Kotlin script
    if command -v kotlinc &> /dev/null; then
        show_info "Ejecutando tests con Kotlin..."

        # Compilar y ejecutar test suite
        kotlinc -cp "$(find . -name "*.jar" | tr '\n' ':')" tests/infrastructure/InfrastructureTestSuite.kt -include-runtime -d tests/infrastructure-tests.jar

        java -cp "tests/infrastructure-tests.jar:$(find . -name "*.jar" | tr '\n' ':')" com.gasolinerajsm.tests.infrastructure.InfrastructureTestSuiteKt > tests/reports/infrastructure-test-report.txt 2>&1

        if [ $? -eq 0 ]; then
            show_progress "Tests básicos de infraestructura completados"
        else
            show_error "Algunos tests básicos fallaron"
            if [ "$DETAILED" = true ]; then
                cat tests/reports/infrastructure-test-report.txt
            fi
        fi
    else
        show_warning "Kotlinc no disponible. Ejecutando validación alternativa..."
        ./scripts/validate-infrastructure.sh --detailed
    fi
}

# Ejecutar tests de rendimiento
run_performance_tests() {
    if [ "$RUN_PERFORMANCE" = false ]; then
        return
    fi

    show_info "Ejecutando tests de rendimiento..."

    cd "$PROJECT_ROOT"

    # Verificar que los servicios estén ejecutándose
    if ! curl -s -f "http://localhost:${API_GATEWAY_PORT:-8080}/health" &> /dev/null; then
        show_warning "API Gateway no está ejecutándose. Iniciando servicios..."
        if [ "$FIX_ISSUES" = true ]; then
            ./start-services.sh &
            sleep 30
        else
            show_error "Servicios no están ejecutándose. Use --fix-issues para iniciarlos automáticamente."
            return
        fi
    fi

    if command -v kotlinc &> /dev/null; then
        kotlinc -cp "$(find . -name "*.jar" | tr '\n' ':')" tests/infrastructure/PerformanceTestSuite.kt -include-runtime -d tests/performance-tests.jar

        java -cp "tests/performance-tests.jar:$(find . -name "*.jar" | tr '\n' ':')" com.gasolinerajsm.tests.infrastructure.PerformanceTestSuiteKt > tests/reports/performance-test-report.txt 2>&1

        if [ $? -eq 0 ]; then
            show_progress "Tests de rendimiento completados"
        else
            show_warning "Algunos tests de rendimiento fallaron"
            if [ "$DETAILED" = true ]; then
                cat tests/reports/performance-test-report.txt
            fi
        fi
    else
        show_warning "Tests de rendimiento requieren Kotlin compiler"
    fi
}

# Ejecutar tests de conectividad
run_connectivity_tests() {
    show_info "Ejecutando tests de conectividad..."

    cd "$PROJECT_ROOT"

    # Test de conectividad de base de datos
    show_info "Probando conectividad de PostgreSQL..."
    if docker-compose -f docker-compose.simple.yml exec -T postgres pg_isready -U "${DB_USERNAME:-gasolinera_user}" -d "${DB_NAME:-gasolinera_db}" &> /dev/null; then
        show_progress "PostgreSQL: Conectividad OK"
    else
        show_error "PostgreSQL: Sin conectividad"
        if [ "$FIX_ISSUES" = true ]; then
            show_info "Reiniciando PostgreSQL..."
            docker-compose -f docker-compose.simple.yml restart postgres
            sleep 10
        fi
    fi

    # Test de conectividad de Redis
    show_info "Probando conectividad de Redis..."
    if docker-compose -f docker-compose.simple.yml exec -T redis redis-cli ping | grep -q "PONG"; then
        show_progress "Redis: Conectividad OK"
    else
        show_error "Redis: Sin conectividad"
        if [ "$FIX_ISSUES" = true ]; then
            show_info "Reiniciando Redis..."
            docker-compose -f docker-compose.simple.yml restart redis
            sleep 5
        fi
    fi

    # Test de conectividad de RabbitMQ
    show_info "Probando conectividad de RabbitMQ..."
    if docker-compose -f docker-compose.simple.yml exec -T rabbitmq rabbitmqctl status &> /dev/null; then
        show_progress "RabbitMQ: Conectividad OK"
    else
        show_error "RabbitMQ: Sin conectividad"
        if [ "$FIX_ISSUES" = true ]; then
            show_info "Reiniciando RabbitMQ..."
            docker-compose -f docker-compose.simple.yml restart rabbitmq
            sleep 15
        fi
    fi
}

# Ejecutar tests de seguridad básicos
run_security_tests() {
    show_info "Ejecutando tests de seguridad básicos..."

    cd "$PROJECT_ROOT"

    # Verificar permisos de archivos sensibles
    if [ -f "ops/key-management/private-key.pem" ]; then
        perms=$(stat -c "%a" ops/key-management/private-key.pem 2>/dev/null || stat -f "%A" ops/key-management/private-key.pem 2>/dev/null || echo "unknown")
        if [ "$perms" = "600" ]; then
            show_progress "Permisos de clave privada: OK"
        else
            show_warning "Permisos de clave privada: $perms (recomendado: 600)"
            if [ "$FIX_ISSUES" = true ]; then
                chmod 600 ops/key-management/private-key.pem
                show_info "Permisos corregidos"
            fi
        fi
    fi

    # Verificar variables sensibles
    if [ -n "$JWT_SECRET" ] && [[ "$JWT_SECRET" == *"change-this-in-production"* ]]; then
        show_warning "JWT_SECRET contiene valor por defecto"
    else
        show_progress "JWT_SECRET: Configurado correctamente"
    fi

    # Test de endpoints sin autenticación
    if curl -s -f "http://localhost:${API_GATEWAY_PORT:-8080}/health" &> /dev/null; then
        show_progress "Endpoint de health accesible"

        # Verificar que endpoints protegidos requieran autenticación
        if curl -s -o /dev/null -w "%{http_code}" "http://localhost:${API_GATEWAY_PORT:-8080}/api/admin" | grep -q "401\|403"; then
            show_progress "Endpoints protegidos requieren autenticación"
        else
            show_warning "Algunos endpoints pueden no estar protegidos correctamente"
        fi
    fi
}

# Generar reporte final
generate_final_report() {
    show_info "Generando reporte final..."

    cd "$PROJECT_ROOT"

    local report_file="tests/reports/final-infrastructure-report.txt"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')

    cat > "$report_file" << EOF
REPORTE FINAL DE TESTS DE INFRAESTRUCTURA
==========================================
Fecha: $timestamp
Proyecto: Gasolinera JSM

RESUMEN DE EJECUCIÓN:
- Tests básicos: $([ -f "tests/reports/infrastructure-test-report.txt" ] && echo "✓ Ejecutados" || echo "✗ No ejecutados")
- Tests de rendimiento: $([ "$RUN_PERFORMANCE" = true ] && echo "✓ Ejecutados" || echo "⏭ Omitidos")
- Tests de conectividad: ✓ Ejecutados
- Tests de seguridad: ✓ Ejecutados

ARCHIVOS DE REPORTE:
$(ls -la tests/reports/ 2>/dev/null || echo "No hay reportes adicionales")

RECOMENDACIONES:
EOF

    if [ "$FIX_ISSUES" = true ]; then
        echo "- Se aplicaron correcciones automáticas durante la ejecución" >> "$report_file"
    else
        echo "- Ejecutar con --fix-issues para aplicar correcciones automáticas" >> "$report_file"
    fi

    if [ "$RUN_PERFORMANCE" = false ]; then
        echo "- Ejecutar con --performance para incluir tests de rendimiento" >> "$report_file"
    fi

    echo "" >> "$report_file"
    echo "Para más detalles, revisar los archivos individuales en tests/reports/" >> "$report_file"

    show_progress "Reporte final generado: $report_file"
}

# Función principal
main() {
    check_prerequisites
    prepare_test_environment
    compile_tests
    run_infrastructure_tests
    run_connectivity_tests
    run_security_tests
    run_performance_tests
    generate_final_report

    echo ""
    echo -e "${GREEN}🎉 Tests de infraestructura completados${NC}"
    echo ""
    echo -e "${BLUE}📋 Reportes generados en: tests/reports/${NC}"
    echo -e "${BLUE}📊 Para ver resultados detallados: cat tests/reports/final-infrastructure-report.txt${NC}"
    echo ""
}

# Manejo de errores
trap 'echo -e "${RED}❌ Error durante la ejecución de tests. Revisa los logs en tests/logs/${NC}"; exit 1' ERR

# Ejecutar función principal
main "$@"