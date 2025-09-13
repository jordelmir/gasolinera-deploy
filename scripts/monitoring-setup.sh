#!/bin/bash

# Gasolinera JSM Platform Monitoring Setup Script
# This script configures monitoring, alerting, and observability tools

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
MONITORING_CONFIG_DIR="$PROJECT_ROOT/config/monitoring"

# Default values
OPERATION=""
ENABLE_GRAFANA=true
ENABLE_PROMETHEUS=true
ENABLE_JAEGER=true
ENABLE_ALERTMANAGER=true
SETUP_DASHBOARDS=true
SETUP_ALERTS=true
NOTIFICATION_EMAIL=""
SLACK_WEBHOOK=""
VERBOSE=false

# Function to print colored output
print_header() {
    echo -e "${PURPLE}================================${NC}"
    echo -e "${PURPLE}$1${NC}"
    echo -e "${PURPLE}================================${NC}"
}

print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check prerequisites
check_prerequisites() {
    print_status "Checking monitoring setup prerequisites..."

    # Check Docker
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed"
        exit 1
    fi

    # Check Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose is not installed"
        exit 1
    fi

    # Check if jq is available for JSON processing
    if ! command -v jq &> /dev/null; then
        print_warning "jq is not installed. Some features may not work properly."
        print_status "Install jq with: sudo apt-get install jq (Ubuntu/Debian) or brew install jq (macOS)"
    fi

    # Create monitoring config directory
    mkdir -p "$MONITORING_CONFIG_DIR"

    print_success "Prerequisites check completed"
}

# Function to setup Prometheus configuration
setup_prometheus() {
    print_status "Setting up Prometheus configuration..."

    # Create Prometheus rules directory
    mkdir -p "$PROJECT_ROOT/config/prometheus/rules"

    # Create custom Prometheus configuration if it doesn't exist
    if [ ! -f "$PROJECT_ROOT/config/prometheus/prometheus.yml" ]; then
        cat > "$PROJECT_ROOT/config/prometheus/prometheus.yml" << 'EOF'
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "/etc/prometheus/rules/*.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  - job_name: 'api-gateway'
    static_configs:
      - targets: ['api-gateway:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 5s

  - job_name: 'auth-service'
    static_configs:
      - targets: ['auth-service:8081']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 5s

  - job_name: 'station-service'
    static_configs:
      - targets: ['station-service:8083']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 5s

  - job_name: 'coupon-service'
    static_configs:
      - targets: ['coupon-service:8086']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 5s

  - job_name: 'redemption-service'
    static_configs:
      - targets: ['redemption-service:8082']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 5s

  - job_name: 'ad-engine'
    static_configs:
      - targets: ['ad-engine:8084']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 5s

  - job_name: 'raffle-service'
    static_configs:
      - targets: ['raffle-service:8085']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 5s

  - job_name: 'postgres-exporter'
    static_configs:
      - targets: ['postgres-exporter:9187']

  - job_name: 'redis-exporter'
    static_configs:
      - targets: ['redis-exporter:9121']

  - job_name: 'rabbitmq-exporter'
    static_configs:
      - targets: ['rabbitmq:15692']

  - job_name: 'node-exporter'
    static_configs:
      - targets: ['node-exporter:9100']

  - job_name: 'cadvisor'
    static_configs:
      - targets: ['cadvisor:8080']
EOF
    fi

    print_success "Prometheus configuration completed"
}

# Function to setup Grafana dashboards
setup_grafana_dashboards() {
    print_status "Setting up Grafana dashboards..."

    # Create Grafana provisioning directories
    mkdir -p "$PROJECT_ROOT/config/grafana/provisioning/dashboards"
    mkdir -p "$PROJECT_ROOT/config/grafana/provisioning/datasources"
    mkdir -p "$PROJECT_ROOT/config/grafana/dashboards"

    # Create datasource configuration
    cat > "$PROJECT_ROOT/config/grafana/provisioning/datasources/prometheus.yml" << 'EOF'
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true

  - name: Jaeger
    type: jaeger
    access: proxy
    url: http://jaeger:16686
    editable: true
EOF

    # Create dashboard provisioning configuration
    cat > "$PROJECT_ROOT/config/grafana/provisioning/dashboards/gasolinera.yml" << 'EOF'
apiVersion: 1

providers:
  - name: 'gasolinera-dashboards'
    orgId: 1
    folder: 'Gasolinera JSM'
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: true
    options:
      path: /var/lib/grafana/dashboards
EOF

    # Create main application dashboard
    cat > "$PROJECT_ROOT/config/grafana/dashboards/gasolinera-overview.json" << 'EOF'
{
  "dashboard": {
    "id": null,
    "title": "Gasolinera JSM Platform Overview",
    "tags": ["gasolinera", "overview"],
    "timezone": "browser",
    "panels": [
      {
        "id": 1,
        "title": "Service Health Status",
        "type": "stat",
        "targets": [
          {
            "expr": "up{job=~\".*-service|api-gateway\"}",
            "legendFormat": "{{job}}"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "color": {
              "mode": "thresholds"
            },
            "thresholds": {
              "steps": [
                {"color": "red", "value": 0},
                {"color": "green", "value": 1}
              ]
            }
          }
        },
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 0}
      },
      {
        "id": 2,
        "title": "Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(http_requests_total[5m])) by (service)",
            "legendFormat": "{{service}}"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 0}
      },
      {
        "id": 3,
        "title": "Response Time (95th percentile)",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket[5m])) by (le, service))",
            "legendFormat": "{{service}}"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 8}
      },
      {
        "id": 4,
        "title": "Error Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(http_requests_total{status=~\"4..|5..\"}[5m])) by (service) / sum(rate(http_requests_total[5m])) by (service)",
            "legendFormat": "{{service}}"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 8}
      }
    ],
    "time": {
      "from": "now-1h",
      "to": "now"
    },
    "refresh": "5s"
  }
}
EOF

    # Create JVM dashboard
    cat > "$PROJECT_ROOT/config/grafana/dashboards/jvm-metrics.json" << 'EOF'
{
  "dashboard": {
    "id": null,
    "title": "JVM Metrics",
    "tags": ["gasolinera", "jvm"],
    "panels": [
      {
        "id": 1,
        "title": "JVM Memory Usage",
        "type": "graph",
        "targets": [
          {
            "expr": "jvm_memory_used_bytes{area=\"heap\"}",
            "legendFormat": "{{service}} - {{id}}"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 0}
      },
      {
        "id": 2,
        "title": "Garbage Collection",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(jvm_gc_collection_seconds_sum[5m])",
            "legendFormat": "{{service}} - {{gc}}"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 0}
      },
      {
        "id": 3,
        "title": "Thread Count",
        "type": "graph",
        "targets": [
          {
            "expr": "jvm_threads_current",
            "legendFormat": "{{service}}"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 8}
      },
      {
        "id": 4,
        "title": "CPU Usage",
        "type": "graph",
        "targets": [
          {
            "expr": "process_cpu_usage",
            "legendFormat": "{{service}}"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 8}
      }
    ],
    "time": {
      "from": "now-1h",
      "to": "now"
    },
    "refresh": "5s"
  }
}
EOF

    print_success "Grafana dashboards setup completed"
}

# Function to setup Alertmanager
setup_alertmanager() {
    print_status "Setting up Alertmanager configuration..."

    # Create Alertmanager config directory
    mkdir -p "$PROJECT_ROOT/config/alertmanager"

    # Create Alertmanager configuration
    cat > "$PROJECT_ROOT/config/alertmanager/alertmanager.yml" << EOF
global:
  smtp_smarthost: 'localhost:587'
  smtp_from: 'alerts@gasolinerajsm.com'
  smtp_auth_username: '${NOTIFICATION_EMAIL}'
  smtp_auth_password: 'your-email-password'

route:
  group_by: ['alertname']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'web.hook'

receivers:
  - name: 'web.hook'
    email_configs:
      - to: '${NOTIFICATION_EMAIL:-admin@gasolinerajsm.com}'
        subject: 'Gasolinera JSM Alert: {{ .GroupLabels.alertname }}'
        body: |
          {{ range .Alerts }}
          Alert: {{ .Annotations.summary }}
          Description: {{ .Annotations.description }}
          Labels: {{ range .Labels.SortedPairs }}{{ .Name }}={{ .Value }} {{ end }}
          {{ end }}

    slack_configs:
      - api_url: '${SLACK_WEBHOOK}'
        channel: '#alerts'
        title: 'Gasolinera JSM Alert'
        text: |
          {{ range .Alerts }}
          *Alert:* {{ .Annotations.summary }}
          *Description:* {{ .Annotations.description }}
          *Labels:* {{ range .Labels.SortedPairs }}{{ .Name }}={{ .Value }} {{ end }}
          {{ end }}

inhibit_rules:
  - source_match:
      severity: 'critical'
    target_match:
      severity: 'warning'
    equal: ['alertname', 'dev', 'instance']
EOF

    print_success "Alertmanager configuration completed"
}

# Function to setup alert rules
setup_alert_rules() {
    print_status "Setting up Prometheus alert rules..."

    # Create alert rules file if it doesn't exist
    if [ ! -f "$PROJECT_ROOT/config/prometheus/rules/gasolinera-alerts.yml" ]; then
        cat > "$PROJECT_ROOT/config/prometheus/rules/gasolinera-alerts.yml" << 'EOF'
groups:
  - name: gasolinera-services
    rules:
      - alert: ServiceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Service {{ $labels.job }} is down"
          description: "Service {{ $labels.job }} has been down for more than 1 minute."

      - alert: HighErrorRate
        expr: sum(rate(http_requests_total{status=~"4..|5.."}[5m])) by (service) / sum(rate(http_requests_total[5m])) by (service) > 0.1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High error rate on {{ $labels.service }}"
          description: "Error rate is {{ $value | humanizePercentage }} on {{ $labels.service }}"

      - alert: HighResponseTime
        expr: histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket[5m])) by (le, service)) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High response time on {{ $labels.service }}"
          description: "95th percentile response time is {{ $value }}s on {{ $labels.service }}"

      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage on {{ $labels.service }}"
          description: "Memory usage is {{ $value | humanizePercentage }} on {{ $labels.service }}"

      - alert: DatabaseConnectionsHigh
        expr: postgres_stat_database_numbackends > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High number of database connections"
          description: "Database has {{ $value }} active connections"

      - alert: RedisMemoryHigh
        expr: redis_memory_used_bytes / redis_memory_max_bytes > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Redis memory usage is high"
          description: "Redis memory usage is {{ $value | humanizePercentage }}"

      - alert: DiskSpaceLow
        expr: (node_filesystem_avail_bytes / node_filesystem_size_bytes) < 0.1
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Disk space is low"
          description: "Disk space is {{ $value | humanizePercentage }} full"

      - alert: HighCPUUsage
        expr: 100 - (avg by(instance) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High CPU usage"
          description: "CPU usage is {{ $value }}% on {{ $labels.instance }}"
EOF
    fi

    print_success "Alert rules setup completed"
}

# Function to setup Jaeger tracing
setup_jaeger() {
    print_status "Setting up Jaeger tracing configuration..."

    # Create Jaeger configuration
    mkdir -p "$PROJECT_ROOT/config/jaeger"

    cat > "$PROJECT_ROOT/config/jaeger/jaeger-config.yml" << 'EOF'
sampling:
  default_strategy:
    type: probabilistic
    param: 0.1
  max_traces_per_second: 100

storage:
  type: memory
  memory:
    max_traces: 10000

query:
  base-path: /
  ui-config: /etc/jaeger/ui-config.json

collector:
  zipkin:
    host-port: :9411
EOF

    # Create Jaeger UI configuration
    cat > "$PROJECT_ROOT/config/jaeger/ui-config.json" << 'EOF'
{
  "monitor": {
    "menuEnabled": true
  },
  "dependencies": {
    "menuEnabled": true
  },
  "archiveEnabled": true,
  "tracking": {
    "gaID": "UA-000000-2"
  }
}
EOF

    print_success "Jaeger configuration completed"
}

# Function to start monitoring stack
start_monitoring() {
    print_status "Starting monitoring stack..."

    cd "$PROJECT_ROOT"

    # Start monitoring services
    if [ "$ENABLE_PROMETHEUS" = true ]; then
        print_status "Starting Prometheus..."
        docker-compose -f docker-compose.monitoring.yml up -d prometheus
    fi

    if [ "$ENABLE_GRAFANA" = true ]; then
        print_status "Starting Grafana..."
        docker-compose -f docker-compose.monitoring.yml up -d grafana
    fi

    if [ "$ENABLE_JAEGER" = true ]; then
        print_status "Starting Jaeger..."
        docker-compose -f docker-compose.monitoring.yml up -d jaeger
    fi

    if [ "$ENABLE_ALERTMANAGER" = true ]; then
        print_status "Starting Alertmanager..."
        docker-compose -f docker-compose.monitoring.yml up -d alertmanager
    fi

    # Wait for services to be ready
    print_status "Waiting for monitoring services to be ready..."
    sleep 30

    # Verify services are running
    local failed_services=()

    if [ "$ENABLE_PROMETHEUS" = true ]; then
        if ! curl -f -s http://localhost:9090/-/ready > /dev/null; then
            failed_services+=("Prometheus")
        else
            print_success "Prometheus is ready at http://localhost:9090"
        fi
    fi

    if [ "$ENABLE_GRAFANA" = true ]; then
        if ! curl -f -s http://localhost:3000/api/health > /dev/null; then
            failed_services+=("Grafana")
        else
            print_success "Grafana is ready at http://localhost:3000"
        fi
    fi

    if [ "$ENABLE_JAEGER" = true ]; then
        if ! curl -f -s http://localhost:16686/ > /dev/null; then
            failed_services+=("Jaeger")
        else
            print_success "Jaeger is ready at http://localhost:16686"
        fi
    fi

    if [ ${#failed_services[@]} -eq 0 ]; then
        print_success "All monitoring services are running successfully"
    else
        print_warning "Some services failed to start: ${failed_services[*]}"
    fi
}

# Function to stop monitoring stack
stop_monitoring() {
    print_status "Stopping monitoring stack..."

    cd "$PROJECT_ROOT"
    docker-compose -f docker-compose.monitoring.yml down

    print_success "Monitoring stack stopped"
}

# Function to show monitoring status
show_monitoring_status() {
    print_status "Monitoring Stack Status:"
    echo ""

    cd "$PROJECT_ROOT"

    # Check if monitoring compose file exists
    if [ ! -f "docker-compose.monitoring.yml" ]; then
        print_warning "Monitoring compose file not found"
        return 1
    fi

    # Show service status
    docker-compose -f docker-compose.monitoring.yml ps

    echo ""
    print_status "Service URLs:"

    local services=(
        "Prometheus:http://localhost:9090"
        "Grafana:http://localhost:3000"
        "Jaeger:http://localhost:16686"
        "Alertmanager:http://localhost:9093"
    )

    for service_info in "${services[@]}"; do
        IFS=':' read -r service_name service_url <<< "$service_info"
        if curl -f -s "$service_url" > /dev/null 2>&1; then
            print_success "$service_name: $service_url"
        else
            print_warning "$service_name: $service_url (not accessible)"
        fi
    done
}

# Function to setup log aggregation
setup_log_aggregation() {
    print_status "Setting up log aggregation..."

    # Create log aggregation configuration
    mkdir -p "$PROJECT_ROOT/config/logging"

    # Create Fluentd configuration for log collection
    cat > "$PROJECT_ROOT/config/logging/fluent.conf" << 'EOF'
<source>
  @type forward
  port 24224
  bind 0.0.0.0
</source>

<match gasolinera.**>
  @type elasticsearch
  host elasticsearch
  port 9200
  logstash_format true
  logstash_prefix gasolinera
  <buffer>
    @type file
    path /var/log/fluentd-buffers/gasolinera.buffer
    flush_mode interval
    retry_type exponential_backoff
    flush_thread_count 2
    flush_interval 5s
    retry_forever
    retry_max_interval 30
    chunk_limit_size 2M
    queue_limit_length 8
    overflow_action block
  </buffer>
</match>
EOF

    print_success "Log aggregation setup completed"
}

# Function to show usage
show_usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Gasolinera JSM Platform Monitoring Setup Script

Operations:
  --operation setup            Setup monitoring configuration
  --operation start            Start monitoring services
  --operation stop             Stop monitoring services
  --operation status           Show monitoring status
  --operation restart          Restart monitoring services

Configuration Options:
  --enable-grafana             Enable Grafana (default: true)
  --enable-prometheus          Enable Prometheus (default: true)
  --enable-jaeger              Enable Jaeger tracing (default: true)
  --enable-alertmanager        Enable Alertmanager (default: true)
  --setup-dashboards           Setup Grafana dashboards (default: true)
  --setup-alerts               Setup alert rules (default: true)

Notification Options:
  --notification-email EMAIL   Email for alert notifications
  --slack-webhook URL          Slack webhook URL for notifications

General Options:
  --verbose                    Enable verbose output
  --help                       Show this help message

Examples:
  $0 --operation setup --notification-email admin@company.com
  $0 --operation start
  $0 --operation status
  $0 --operation setup --slack-webhook https://hooks.slack.com/...

Environment Variables:
  GRAFANA_ADMIN_PASSWORD       Override default Grafana admin password
  PROMETHEUS_RETENTION         Override Prometheus data retention period
  NOTIFICATION_EMAIL           Default notification email
  SLACK_WEBHOOK               Default Slack webhook URL

EOF
}

# Main function
main() {
    print_header "Gasolinera JSM Platform Monitoring Setup"
    print_status "Timestamp: $(date)"

    case "$OPERATION" in
        "setup")
            check_prerequisites
            setup_prometheus
            if [ "$SETUP_DASHBOARDS" = true ]; then
                setup_grafana_dashboards
            fi
            if [ "$SETUP_ALERTS" = true ]; then
                setup_alert_rules
                setup_alertmanager
            fi
            setup_jaeger
            setup_log_aggregation
            print_success "Monitoring setup completed successfully"
            ;;
        "start")
            start_monitoring
            ;;
        "stop")
            stop_monitoring
            ;;
        "status")
            show_monitoring_status
            ;;
        "restart")
            stop_monitoring
            sleep 5
            start_monitoring
            ;;
        "")
            print_error "Operation must be specified"
            show_usage
            exit 1
            ;;
        *)
            print_error "Invalid operation: $OPERATION"
            show_usage
            exit 1
            ;;
    esac
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --operation)
            OPERATION="$2"
            shift 2
            ;;
        --notification-email)
            NOTIFICATION_EMAIL="$2"
            shift 2
            ;;
        --slack-webhook)
            SLACK_WEBHOOK="$2"
            shift 2
            ;;
        --enable-grafana)
            ENABLE_GRAFANA=true
            shift
            ;;
        --enable-prometheus)
            ENABLE_PROMETHEUS=true
            shift
            ;;
        --enable-jaeger)
            ENABLE_JAEGER=true
            shift
            ;;
        --enable-alertmanager)
            ENABLE_ALERTMANAGER=true
            shift
            ;;
        --setup-dashboards)
            SETUP_DASHBOARDS=true
            shift
            ;;
        --setup-alerts)
            SETUP_ALERTS=true
            shift
            ;;
        --verbose)
            VERBOSE=true
            shift
            ;;
        --help)
            show_usage
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Override with environment variables if set
NOTIFICATION_EMAIL=${NOTIFICATION_EMAIL:-$NOTIFICATION_EMAIL}
SLACK_WEBHOOK=${SLACK_WEBHOOK:-$SLACK_WEBHOOK}

# Execute main function
main "$@"