#!/bin/bash

# Script para configurar el sistema de logging centralizado de Gasolinera JSM

set -e

echo "🚀 Configurando Sistema de Logging Centralizado - Gasolinera JSM"
echo "================================================================"

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

# Verificar dependencias
check_dependencies() {
    log_info "Verificando dependencias..."

    if ! command -v docker &> /dev/null; then
        log_error "Docker no está instalado. Por favor instala Docker primero."
        exit 1
    fi

    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose no está instalado. Por favor instala Docker Compose primero."
        exit 1
    fi

    log_success "Dependencias verificadas correctamente"
}

# Crear directorios necesarios
create_directories() {
    log_info "Creando directorios necesarios..."

    mkdir -p logs
    mkdir -p infrastructure/logging/logstash/templates
    mkdir -p infrastructure/logging/kibana/saved_objects

    log_success "Directorios creados"
}

# Configurar permisos
setup_permissions() {
    log_info "Configurando permisos..."

    # Permisos para Elasticsearch
    sudo sysctl -w vm.max_map_count=262144
    echo 'vm.max_map_count=262144' | sudo tee -a /etc/sysctl.conf

    # Permisos para directorios de logs
    chmod 755 logs/

    log_success "Permisos configurados"
}

# Crear template de Elasticsearch
create_elasticsearch_template() {
    log_info "Creando template de Elasticsearch..."

    cat > infrastructure/logging/logstash/templates/gasolinera-template.json << 'EOF'
{
  "index_patterns": ["gasolinera-*"],
  "template": {
    "settings": {
      "number_of_shards": 1,
      "number_of_replicas": 0,
      "index.refresh_interval": "5s"
    },
    "mappings": {
      "properties": {
        "@timestamp": {
          "type": "date"
        },
        "level": {
          "type": "keyword"
        },
        "message": {
          "type": "text",
          "analyzer": "standard"
        },
        "service": {
          "type": "keyword"
        },
        "correlation_id": {
          "type": "keyword"
        },
        "user_id": {
          "type": "keyword"
        },
        "trace_id": {
          "type": "keyword"
        },
        "span_id": {
          "type": "keyword"
        },
        "log_category": {
          "type": "keyword"
        },
        "business_operation": {
          "type": "keyword"
        },
        "http_method": {
          "type": "keyword"
        },
        "http_uri": {
          "type": "keyword"
        },
        "http_status": {
          "type": "integer"
        },
        "duration_ms": {
          "type": "long"
        },
        "client_ip": {
          "type": "ip"
        },
        "exception_class": {
          "type": "keyword"
        },
        "exception_message": {
          "type": "text"
        }
      }
    }
  }
}
EOF

    log_success "Template de Elasticsearch creado"
}

# Iniciar stack ELK
start_elk_stack() {
    log_info "Iniciando stack ELK..."

    cd infrastructure/logging

    # Detener contenedores existentes si los hay
    docker-compose -f docker-compose.logging.yml down 2>/dev/null || true

    # Iniciar servicios
    docker-compose -f docker-compose.logging.yml up -d

    cd ../..

    log_success "Stack ELK iniciado"
}

# Esperar a que los servicios estén listos
wait_for_services() {
    log_info "Esperando a que los servicios estén listos..."

    # Esperar Elasticsearch
    log_info "Esperando Elasticsearch..."
    timeout=300
    while ! curl -s http://localhost:9200/_cluster/health >/dev/null 2>&1; do
        sleep 5
        timeout=$((timeout - 5))
        if [ $timeout -le 0 ]; then
            log_error "Timeout esperando Elasticsearch"
            exit 1
        fi
    done
    log_success "Elasticsearch está listo"

    # Esperar Kibana
    log_info "Esperando Kibana..."
    timeout=300
    while ! curl -s http://localhost:5601/api/status >/dev/null 2>&1; do
        sleep 5
        timeout=$((timeout - 5))
        if [ $timeout -le 0 ]; then
            log_error "Timeout esperando Kibana"
            exit 1
        fi
    done
    log_success "Kibana está listo"

    # Esperar Logstash
    log_info "Esperando Logstash..."
    timeout=300
    while ! curl -s http://localhost:9600/_node/stats >/dev/null 2>&1; do
        sleep 5
        timeout=$((timeout - 5))
        if [ $timeout -le 0 ]; then
            log_error "Timeout esperando Logstash"
            exit 1
        fi
    done
    log_success "Logstash está listo"
}

# Configurar índices en Kibana
setup_kibana_indices() {
    log_info "Configurando índices en Kibana..."

    # Crear index patterns
    curl -X POST "localhost:5601/api/saved_objects/index-pattern/gasolinera-logs" \
        -H "kbn-xsrf: true" \
        -H "Content-Type: application/json" \
        -d '{
            "attributes": {
                "title": "gasolinera-logs-*",
                "timeFieldName": "@timestamp"
            }
        }' 2>/dev/null || log_warning "No se pudo crear index pattern gasolinera-logs"

    curl -X POST "localhost:5601/api/saved_objects/index-pattern/gasolinera-business" \
        -H "kbn-xsrf: true" \
        -H "Content-Type: application/json" \
        -d '{
            "attributes": {
                "title": "gasolinera-business-*",
                "timeFieldName": "@timestamp"
            }
        }' 2>/dev/null || log_warning "No se pudo crear index pattern gasolinera-business"

    curl -X POST "localhost:5601/api/saved_objects/index-pattern/gasolinera-security" \
        -H "kbn-xsrf: true" \
        -H "Content-Type: application/json" \
        -d '{
            "attributes": {
                "title": "gasolinera-security-*",
                "timeFieldName": "@timestamp"
            }
        }' 2>/dev/null || log_warning "No se pudo crear index pattern gasolinera-security"

    curl -X POST "localhost:5601/api/saved_objects/index-pattern/gasolinera-performance" \
        -H "kbn-xsrf: true" \
        -H "Content-Type: application/json" \
        -d '{
            "attributes": {
                "title": "gasolinera-performance-*",
                "timeFieldName": "@timestamp"
            }
        }' 2>/dev/null || log_warning "No se pudo crear index pattern gasolinera-performance"

    log_success "Índices configurados en Kibana"
}

# Generar logs de prueba
generate_test_logs() {
    log_info "Generando logs de prueba..."

    # Crear algunos logs de prueba
    for i in {1..10}; do
        echo "{\"@timestamp\":\"$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)\",\"level\":\"INFO\",\"message\":\"Test log message $i\",\"service\":\"test-service\",\"correlation_id\":\"test-$i\",\"log_category\":\"APPLICATION\"}" | nc localhost 5000
        sleep 0.1
    done

    log_success "Logs de prueba generados"
}

# Mostrar información de acceso
show_access_info() {
    echo ""
    echo "🎉 ¡Sistema de Logging Configurado Exitosamente!"
    echo "=============================================="
    echo ""
    echo "📊 Acceso a las interfaces:"
    echo "  • Kibana:        http://localhost:5601"
    echo "  • Elasticsearch: http://localhost:9200"
    echo "  • Logstash API:  http://localhost:9600"
    echo ""
    echo "📝 Index Patterns creados en Kibana:"
    echo "  • gasolinera-logs-*        (logs generales)"
    echo "  • gasolinera-business-*    (logs de negocio)"
    echo "  • gasolinera-security-*    (logs de seguridad)"
    echo "  • gasolinera-performance-* (logs de performance)"
    echo ""
    echo "🔧 Comandos útiles:"
    echo "  • Ver logs:     docker-compose -f infrastructure/logging/docker-compose.logging.yml logs -f"
    echo "  • Detener:      docker-compose -f infrastructure/logging/docker-compose.logging.yml down"
    echo "  • Reiniciar:    docker-compose -f infrastructure/logging/docker-compose.logging.yml restart"
    echo ""
    echo "📖 Para más información, consulta: infrastructure/logging/README.md"
}

# Función principal
main() {
    case "${1:-setup}" in
        "setup")
            check_dependencies
            create_directories
            setup_permissions
            create_elasticsearch_template
            start_elk_stack
            wait_for_services
            setup_kibana_indices
            generate_test_logs
            show_access_info
            ;;
        "start")
            log_info "Iniciando stack ELK..."
            cd infrastructure/logging
            docker-compose -f docker-compose.logging.yml up -d
            cd ../..
            log_success "Stack ELK iniciado"
            ;;
        "stop")
            log_info "Deteniendo stack ELK..."
            cd infrastructure/logging
            docker-compose -f docker-compose.logging.yml down
            cd ../..
            log_success "Stack ELK detenido"
            ;;
        "restart")
            log_info "Reiniciando stack ELK..."
            cd infrastructure/logging
            docker-compose -f docker-compose.logging.yml restart
            cd ../..
            log_success "Stack ELK reiniciado"
            ;;
        "logs")
            cd infrastructure/logging
            docker-compose -f docker-compose.logging.yml logs -f
            cd ../..
            ;;
        "status")
            log_info "Estado de los servicios:"
            echo "Elasticsearch: $(curl -s http://localhost:9200/_cluster/health | jq -r .status 2>/dev/null || echo 'No disponible')"
            echo "Kibana: $(curl -s http://localhost:5601/api/status | jq -r .status.overall.state 2>/dev/null || echo 'No disponible')"
            echo "Logstash: $(curl -s http://localhost:9600/_node/stats | jq -r .status 2>/dev/null || echo 'No disponible')"
            ;;
        "help")
            echo "Uso: $0 [comando]"
            echo ""
            echo "Comandos disponibles:"
            echo "  setup    - Configuración completa inicial (por defecto)"
            echo "  start    - Iniciar stack ELK"
            echo "  stop     - Detener stack ELK"
            echo "  restart  - Reiniciar stack ELK"
            echo "  logs     - Ver logs de los contenedores"
            echo "  status   - Ver estado de los servicios"
            echo "  help     - Mostrar esta ayuda"
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