#!/bin/bash

# Production Environment Setup Script
# Sets up the complete production environment with security and monitoring

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
DATA_DIR="/opt/gasolinera"
BACKUP_DIR="/opt/gasolinera/backups"
LOG_DIR="/opt/gasolinera/logs"

# Functions
print_banner() {
    echo -e "${PURPLE}"
    echo "╔══════════════════════════════════════════════════════════════════════════════╗"
    echo "║                  🚀 GASOLINERA JSM PRODUCTION SETUP 🚀                      ║"
    echo "║                        ENTERPRISE GRADE DEPLOYMENT                          ║"
    echo "╚══════════════════════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

print_header() {
    echo -e "${CYAN}================================${NC}"
    echo -e "${CYAN}  $1${NC}"
    echo -e "${CYAN}================================${NC}"
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

check_root_privileges() {
    if [[ $EUID -ne 0 ]]; then
        print_error "This script must be run as root for production setup"
        print_status "Please run: sudo $0"
        exit 1
    fi
    print_success "Running with root privileges"
}

check_prerequisites() {
    print_header "Checking Prerequisites"

    local missing_tools=()

    # Check Docker
    if ! command -v docker &> /dev/null; then
        missing_tools+=("docker")
    fi

    # Check Docker Compose
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        missing_tools+=("docker-compose")
    fi

    # Check OpenSSL
    if ! command -v openssl &> /dev/null; then
        missing_tools+=("openssl")
    fi

    # Check jq
    if ! command -v jq &> /dev/null; then
        missing_tools+=("jq")
    fi

    if [ ${#missing_tools[@]} -ne 0 ]; then
        print_error "Missing required tools: ${missing_tools[*]}"
        print_status "Installing missing tools..."

        # Install missing tools based on OS
        if command -v apt-get &> /dev/null; then
            apt-get update
            for tool in "${missing_tools[@]}"; do
                case "$tool" in
                    "docker")
                        curl -fsSL https://get.docker.com -o get-docker.sh
                        sh get-docker.sh
                        rm get-docker.sh
                        ;;
                    "docker-compose")
                        curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
                        chmod +x /usr/local/bin/docker-compose
                        ;;
                    *)
                        apt-get install -y "$tool"
                        ;;
                esac
            done
        elif command -v yum &> /dev/null; then
            for tool in "${missing_tools[@]}"; do
                yum install -y "$tool"
            done
        else
            print_error "Unsupported package manager. Please install tools manually."
            exit 1
        fi
    fi

    # Check Docker daemon
    if ! docker info &> /dev/null; then
        print_error "Docker daemon is not running. Starting Docker..."
        systemctl start docker
        systemctl enable docker
    fi

    print_success "All prerequisites are available"
}

create_production_directories() {
    print_header "Creating Production Directory Structure"

    local directories=(
        "$DATA_DIR/data/postgres-primary"
        "$DATA_DIR/data/postgres-replica"
        "$DATA_DIR/data/redis-primary"
        "$DATA_DIR/data/redis-replica"
        "$DATA_DIR/data/rabbitmq-primary"
        "$DATA_DIR/data/vault"
        "$DATA_DIR/data/consul"
        "$DATA_DIR/data/elasticsearch"
        "$DATA_DIR/data/prometheus"
        "$DATA_DIR/data/grafana"
        "$DATA_DIR/data/jaeger"
        "$LOG_DIR/postgres"
        "$LOG_DIR/redis"
        "$LOG_DIR/rabbitmq"
        "$LOG_DIR/vault"
        "$LOG_DIR/consul"
        "$LOG_DIR/elasticsearch"
        "$BACKUP_DIR/postgres"
        "$BACKUP_DIR/redis"
        "$BACKUP_DIR/wal"
        "/etc/gasolinera/ssl"
        "/etc/gasolinera/secrets"
    )

    for dir in "${directories[@]}"; do
        if [ ! -d "$dir" ]; then
            mkdir -p "$dir"
            print_success "Created directory: $dir"
        else
            print_status "Directory already exists: $dir"
        fi
    done

    # Set proper permissions
    chown -R 999:999 "$DATA_DIR/data/postgres-primary" "$DATA_DIR/data/postgres-replica"
    chown -R 999:999 "$LOG_DIR/postgres"
    chown -R 999:999 "$DATA_DIR/data/redis-primary" "$DATA_DIR/data/redis-replica"
    chown -R 472:472 "$DATA_DIR/data/grafana"
    chown -R 1000:1000 "$DATA_DIR/data/elasticsearch"

    chmod 700 "/etc/gasolinera/secrets"
    chmod 755 "$DATA_DIR" "$LOG_DIR" "$BACKUP_DIR"

    print_success "Production directories created and configured"
}

generate_ssl_certificates() {
    print_header "Generating SSL Certificates"

    local ssl_dir="/etc/gasolinera/ssl"

    if [ ! -f "$ssl_dir/ca.key" ]; then
        print_status "Generating CA certificate..."

        # Generate CA private key
        openssl genrsa -out "$ssl_dir/ca.key" 4096

        # Generate CA certificate
        openssl req -new -x509 -days 3650 -key "$ssl_dir/ca.key" -out "$ssl_dir/ca.crt" \
            -subj "/C=US/ST=State/L=City/O=Gasolinera JSM/OU=IT Department/CN=Gasolinera JSM CA"

        print_success "CA certificate generated"
    fi

    if [ ! -f "$ssl_dir/server.key" ]; then
        print_status "Generating server certificate..."

        # Generate server private key
        openssl genrsa -out "$ssl_dir/server.key" 2048

        # Generate server certificate signing request
        openssl req -new -key "$ssl_dir/server.key" -out "$ssl_dir/server.csr" \
            -subj "/C=US/ST=State/L=City/O=Gasolinera JSM/OU=IT Department/CN=gasolinera-jsm.local"

        # Generate server certificate
        openssl x509 -req -days 365 -in "$ssl_dir/server.csr" -CA "$ssl_dir/ca.crt" -CAkey "$ssl_dir/ca.key" \
            -CAcreateserial -out "$ssl_dir/server.crt"

        # Clean up CSR
        rm "$ssl_dir/server.csr"

        print_success "Server certificate generated"
    fi

    # Set proper permissions
    chmod 600 "$ssl_dir"/*.key
    chmod 644 "$ssl_dir"/*.crt

    print_success "SSL certificates configured"
}

create_docker_secrets() {
    print_header "Creating Docker Secrets"

    local secrets_dir="/etc/gasolinera/secrets"

    # Generate random passwords if they don't exist
    local secrets=(
        "postgres_password"
        "postgres_replica_password"
        "rabbitmq_user"
        "rabbitmq_password"
        "rabbitmq_erlang_cookie"
        "vault_token"
        "elasticsearch_password"
        "grafana_admin_password"
        "redis_password"
    )

    for secret in "${secrets[@]}"; do
        local secret_file="$secrets_dir/$secret"

        if [ ! -f "$secret_file" ]; then
            case "$secret" in
                "rabbitmq_user")
                    echo "gasolinera_admin" > "$secret_file"
                    ;;
                "rabbitmq_erlang_cookie")
                    openssl rand -hex 32 > "$secret_file"
                    ;;
                "vault_token")
                    openssl rand -hex 16 > "$secret_file"
                    ;;
                *)
                    openssl rand -base64 32 > "$secret_file"
                    ;;
            esac

            chmod 600 "$secret_file"
            print_success "Generated secret: $secret"
        else
            print_status "Secret already exists: $secret"
        fi
    done

    # Create Docker secrets
    for secret in "${secrets[@]}"; do
        local secret_name="gasolinera_$secret"
        local secret_file="$secrets_dir/$secret"

        if ! docker secret ls | grep -q "$secret_name"; then
            docker secret create "$secret_name" "$secret_file"
            print_success "Created Docker secret: $secret_name"
        else
            print_status "Docker secret already exists: $secret_name"
        fi
    done
}

setup_monitoring_configs() {
    print_header "Setting Up Monitoring Configurations"

    # Create Prometheus configuration
    local prometheus_config="$PROJECT_ROOT/infrastructure/monitoring/prometheus-prod.yml"
    if [ ! -f "$prometheus_config" ]; then
        cat > "$prometheus_config" << 'EOF'
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    cluster: 'gasolinera-prod'
    environment: 'production'

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

  - job_name: 'gasolinera-services'
    static_configs:
      - targets:
        - 'api-gateway:8080'
        - 'auth-service:8080'
        - 'coupon-service:8080'
        - 'station-service:8080'
        - 'raffle-service:8080'
        - 'redemption-service:8080'
        - 'ad-engine:8080'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 30s

  - job_name: 'postgres-exporter'
    static_configs:
      - targets: ['postgres-exporter:9187']

  - job_name: 'redis-exporter'
    static_configs:
      - targets: ['redis-exporter:9121']

  - job_name: 'rabbitmq'
    static_configs:
      - targets: ['rabbitmq-primary:15692']

  - job_name: 'node-exporter'
    static_configs:
      - targets: ['node-exporter:9100']
EOF
        print_success "Created Prometheus configuration"
    fi

    # Create Grafana datasource configuration
    local grafana_datasources="$PROJECT_ROOT/infrastructure/monitoring/grafana/datasources/prometheus.yml"
    mkdir -p "$(dirname "$grafana_datasources")"

    if [ ! -f "$grafana_datasources" ]; then
        cat > "$grafana_datasources" << 'EOF'
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
EOF
        print_success "Created Grafana datasource configuration"
    fi
}

setup_backup_system() {
    print_header "Setting Up Backup System"

    # Create backup script
    local backup_script="/usr/local/bin/gasolinera-backup.sh"

    cat > "$backup_script" << 'EOF'
#!/bin/bash

# Gasolinera JSM Production Backup Script

set -euo pipefail

BACKUP_DIR="/opt/gasolinera/backups"
DATE=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=30

# PostgreSQL Backup
echo "Starting PostgreSQL backup..."
docker exec gasolinera-postgres-primary-prod pg_dump -U gasolinera_user gasolinera_jsm | gzip > "$BACKUP_DIR/postgres/gasolinera_jsm_$DATE.sql.gz"

# Redis Backup
echo "Starting Redis backup..."
docker exec gasolinera-redis-primary-prod redis-cli --rdb /data/dump_$DATE.rdb
docker cp gasolinera-redis-primary-prod:/data/dump_$DATE.rdb "$BACKUP_DIR/redis/"

# Vault Backup
echo "Starting Vault backup..."
docker exec gasolinera-vault-prod vault operator raft snapshot save /vault/data/vault_snapshot_$DATE.snap

# Clean old backups
echo "Cleaning old backups..."
find "$BACKUP_DIR" -name "*.gz" -mtime +$RETENTION_DAYS -delete
find "$BACKUP_DIR" -name "*.rdb" -mtime +$RETENTION_DAYS -delete
find "$BACKUP_DIR" -name "*.snap" -mtime +$RETENTION_DAYS -delete

echo "Backup completed successfully: $DATE"
EOF

    chmod +x "$backup_script"

    # Create cron job for daily backups
    local cron_job="0 2 * * * $backup_script >> /var/log/gasolinera-backup.log 2>&1"

    if ! crontab -l 2>/dev/null | grep -q "gasolinera-backup"; then
        (crontab -l 2>/dev/null; echo "$cron_job") | crontab -
        print_success "Created daily backup cron job"
    else
        print_status "Backup cron job already exists"
    fi
}

setup_log_rotation() {
    print_header "Setting Up Log Rotation"

    local logrotate_config="/etc/logrotate.d/gasolinera"

    cat > "$logrotate_config" << 'EOF'
/opt/gasolinera/logs/*/*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    create 644 root root
    postrotate
        docker kill --signal="USR1" $(docker ps -q --filter "name=gasolinera-") 2>/dev/null || true
    endscript
}

/var/log/gasolinera-*.log {
    daily
    missingok
    rotate 7
    compress
    delaycompress
    notifempty
    create 644 root root
}
EOF

    print_success "Created log rotation configuration"
}

setup_firewall() {
    print_header "Setting Up Firewall Rules"

    if command -v ufw &> /dev/null; then
        # UFW (Ubuntu/Debian)
        ufw --force reset
        ufw default deny incoming
        ufw default allow outgoing

        # SSH
        ufw allow ssh

        # HTTP/HTTPS
        ufw allow 80/tcp
        ufw allow 443/tcp

        # Application ports (restrict to specific IPs in production)
        ufw allow from 10.0.0.0/8 to any port 8080
        ufw allow from 172.16.0.0/12 to any port 8080
        ufw allow from 192.168.0.0/16 to any port 8080

        # Monitoring ports (restrict to monitoring network)
        ufw allow from 10.0.0.0/8 to any port 9090
        ufw allow from 10.0.0.0/8 to any port 3000

        ufw --force enable
        print_success "UFW firewall configured"

    elif command -v firewall-cmd &> /dev/null; then
        # FirewallD (RHEL/CentOS)
        firewall-cmd --permanent --zone=public --add-service=ssh
        firewall-cmd --permanent --zone=public --add-service=http
        firewall-cmd --permanent --zone=public --add-service=https

        # Application ports
        firewall-cmd --permanent --zone=public --add-port=8080/tcp

        # Monitoring ports
        firewall-cmd --permanent --zone=public --add-port=9090/tcp
        firewall-cmd --permanent --zone=public --add-port=3000/tcp

        firewall-cmd --reload
        print_success "FirewallD configured"
    else
        print_warning "No supported firewall found. Please configure manually."
    fi
}

optimize_system() {
    print_header "Optimizing System for Production"

    # Kernel parameters for high performance
    cat > /etc/sysctl.d/99-gasolinera.conf << 'EOF'
# Network optimizations
net.core.somaxconn = 65535
net.core.netdev_max_backlog = 5000
net.ipv4.tcp_max_syn_backlog = 65535
net.ipv4.tcp_keepalive_time = 600
net.ipv4.tcp_keepalive_intvl = 60
net.ipv4.tcp_keepalive_probes = 3

# Memory optimizations
vm.swappiness = 10
vm.dirty_ratio = 15
vm.dirty_background_ratio = 5

# File system optimizations
fs.file-max = 2097152
fs.nr_open = 1048576

# Security
kernel.dmesg_restrict = 1
kernel.kptr_restrict = 2
EOF

    sysctl -p /etc/sysctl.d/99-gasolinera.conf

    # Increase file limits
    cat > /etc/security/limits.d/99-gasolinera.conf << 'EOF'
* soft nofile 65536
* hard nofile 65536
* soft nproc 32768
* hard nproc 32768
EOF

    print_success "System optimizations applied"
}

create_systemd_service() {
    print_header "Creating Systemd Service"

    local service_file="/etc/systemd/system/gasolinera-jsm.service"

    cat > "$service_file" << EOF
[Unit]
Description=Gasolinera JSM Production Stack
Requires=docker.service
After=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=$PROJECT_ROOT
ExecStart=/usr/local/bin/docker-compose -f docker-compose.prod.yml up -d
ExecStop=/usr/local/bin/docker-compose -f docker-compose.prod.yml down
TimeoutStartSec=0

[Install]
WantedBy=multi-user.target
EOF

    systemctl daemon-reload
    systemctl enable gasolinera-jsm.service

    print_success "Systemd service created and enabled"
}

show_production_info() {
    print_header "🎉 Production Environment Ready!"

    echo -e "${GREEN}Your Gasolinera JSM production environment is configured!${NC}"
    echo ""
    echo -e "${CYAN}🔐 Security Information:${NC}"
    echo "  SSL Certificates: /etc/gasolinera/ssl/"
    echo "  Docker Secrets: Created and configured"
    echo "  Firewall: Configured with restricted access"
    echo ""
    echo -e "${CYAN}📁 Data Locations:${NC}"
    echo "  Application Data: $DATA_DIR/data/"
    echo "  Logs: $LOG_DIR/"
    echo "  Backups: $BACKUP_DIR/"
    echo ""
    echo -e "${CYAN}🔧 Management Commands:${NC}"
    echo "  Start Services: systemctl start gasolinera-jsm"
    echo "  Stop Services: systemctl stop gasolinera-jsm"
    echo "  View Status: systemctl status gasolinera-jsm"
    echo "  View Logs: journalctl -u gasolinera-jsm -f"
    echo ""
    echo -e "${CYAN}📊 Monitoring URLs:${NC}"
    echo "  Grafana: http://your-server:3000"
    echo "  Prometheus: http://your-server:9090"
    echo "  Jaeger: http://your-server:16686"
    echo ""
    echo -e "${CYAN}💾 Backup Information:${NC}"
    echo "  Automatic Backups: Daily at 2:00 AM"
    echo "  Backup Location: $BACKUP_DIR/"
    echo "  Retention: 30 days"
    echo ""
    echo -e "${YELLOW}⚠️  Next Steps:${NC}"
    echo "  1. Update .env.prod with your specific configuration"
    echo "  2. Configure external load balancer/reverse proxy"
    echo "  3. Set up external monitoring and alerting"
    echo "  4. Configure backup storage (S3, etc.)"
    echo "  5. Set up SSL certificates from trusted CA"
    echo "  6. Configure DNS records"
    echo "  7. Test disaster recovery procedures"
    echo ""
    echo -e "${GREEN}Production setup completed successfully! 🚀${NC}"
}

main() {
    print_banner

    # Change to project root
    cd "$PROJECT_ROOT"

    # Execute setup steps
    check_root_privileges
    check_prerequisites
    create_production_directories
    generate_ssl_certificates
    create_docker_secrets
    setup_monitoring_configs
    setup_backup_system
    setup_log_rotation
    setup_firewall
    optimize_system
    create_systemd_service

    # Show results
    show_production_info

    print_success "🚀 Production environment setup completed successfully!"
}

# Handle command line arguments
case "${1:-}" in
    --help|-h)
        echo "Usage: $0 [OPTIONS]"
        echo "Options:"
        echo "  --help, -h     Show this help message"
        echo "  --skip-firewall Skip firewall configuration"
        echo "  --skip-ssl     Skip SSL certificate generation"
        exit 0
        ;;
    --skip-firewall)
        SKIP_FIREWALL=true
        ;;
    --skip-ssl)
        SKIP_SSL=true
        ;;
esac

# Run main function
main "$@"